package com.profmojo.concurrency;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.scheduler.AmenitySlaMonitor;
import com.profmojo.services.AdminAmenityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Phase 2 — Amenity Concurrency & Lock Ordering Integration Tests")
class AmenityConcurrencyIntegrationTest extends BasePostgresContainerTest {

    @Autowired private AdminAmenityService adminAmenityService;
    @Autowired private AmenityRequestRepository requestRepo;
    @Autowired private StaffRepository staffRepo;
    @Autowired private NotificationRepository notifRepo;
    @Autowired private AmenitySlaMonitor slaMonitor;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        notifRepo.deleteAll();
        requestRepo.deleteAll();
        staffRepo.deleteAll();
    }

    private Staff savedStaff(String id, String dept) {
        return staffRepo.save(Staff.builder()
                .staffId(id)
                .name("Staff " + id)
                .password(passwordEncoder.encode("pass"))
                .department(dept)
                .contactNo("0000000000")
                .email(id + "@test.com")
                .available(true)
                .online(true)
                .build());
    }

    private AmenityRequest savedRequest(String profId, String dept, RequestStatus status, LocalDateTime createdAt) {
        return requestRepo.save(AmenityRequest.builder()
                .professorId(profId)
                .professorName("Prof " + profId)
                .department(dept)
                .classRoom("Room 101")
                .status(status)
                .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                .assignmentDeadline(createdAt != null ? createdAt.plusMinutes(2) : LocalDateTime.now().plusMinutes(2))
                .assignmentSlaBreached(false)
                .deliverySlaBreached(false)
                .build());
    }

    @Test
    @DisplayName("1. Multiple staff concurrently assigning SAME request: exactly ONE assignment succeeds")
    void concurrentAssign_SameRequestMultipleStaff_ExactlyOneSucceeds() throws Exception {
        AmenityRequest req = savedRequest("prof-1", "CSE", RequestStatus.PENDING, LocalDateTime.now());
        Staff s1 = savedStaff("staff-m1", "CSE");
        Staff s2 = savedStaff("staff-m2", "CSE");
        Staff s3 = savedStaff("staff-m3", "CSE");

        int threadCount = 3;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        List<Staff> staffList = List.of(s1, s2, s3);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (Staff st : staffList) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    adminAmenityService.assignStaff(req.getId(), st.getStaffId(), "CSE");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(2);

        AmenityRequest updated = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(updated.getAssignedStaff()).isNotNull();

        List<Staff> allStaff = staffRepo.findAll();
        long unavailable = allStaff.stream().filter(s -> !s.isAvailable()).count();
        assertThat(unavailable).isEqualTo(1L);
    }

    @Test
    @DisplayName("2. Multiple requests concurrently claiming SAME staff: exactly ONE active assignment succeeds")
    void concurrentAssign_MultipleRequestsSameStaff_ExactlyOneSucceeds() throws Exception {
        Staff staff = savedStaff("staff-single", "CSE");
        AmenityRequest req1 = savedRequest("prof-1", "CSE", RequestStatus.PENDING, LocalDateTime.now());
        AmenityRequest req2 = savedRequest("prof-2", "CSE", RequestStatus.PENDING, LocalDateTime.now());
        AmenityRequest req3 = savedRequest("prof-3", "CSE", RequestStatus.PENDING, LocalDateTime.now());

        int threadCount = 3;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        List<AmenityRequest> requests = List.of(req1, req2, req3);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (AmenityRequest r : requests) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    adminAmenityService.assignStaff(r.getId(), staff.getStaffId(), "CSE");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(2);

        Staff updatedStaff = staffRepo.findById(staff.getStaffId()).orElseThrow();
        assertThat(updatedStaff.isAvailable()).isFalse();

        List<AmenityRequest> allReqs = requestRepo.findAll();
        long assignedCount = allReqs.stream().filter(r -> r.getStatus() == RequestStatus.ASSIGNED).count();
        assertThat(assignedCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("3. Manual assignment racing with auto queue assignment for SAME staff: at most ONE succeeds")
    void manualAndAutoAssign_RacingForSameStaff_AtMostOneSucceeds() throws Exception {
        Staff staff = savedStaff("staff-race", "CSE");
        AmenityRequest manualReq = savedRequest("prof-m", "CSE", RequestStatus.PENDING, LocalDateTime.now());
        AmenityRequest queuedReq = savedRequest("prof-q", "CSE", RequestStatus.QUEUED, LocalDateTime.now().minusMinutes(1));

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicInteger manualSuccess = new AtomicInteger(0);
        AtomicInteger autoSuccess = new AtomicInteger(0);

        pool.submit(() -> {
            try {
                startGate.await();
                adminAmenityService.assignStaff(manualReq.getId(), staff.getStaffId(), "CSE");
                manualSuccess.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                finishLine.countDown();
            }
        });

        pool.submit(() -> {
            try {
                startGate.await();
                adminAmenityService.tryAssignQueuedRequest(staff);
                autoSuccess.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                finishLine.countDown();
            }
        });

        startGate.countDown();
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        Staff reloadedStaff = staffRepo.findById(staff.getStaffId()).orElseThrow();
        assertThat(reloadedStaff.isAvailable()).isFalse();

        AmenityRequest manualReloaded = requestRepo.findById(manualReq.getId()).orElseThrow();
        AmenityRequest queuedReloaded = requestRepo.findById(queuedReq.getId()).orElseThrow();

        int assignedCount = 0;
        if (manualReloaded.getStatus() == RequestStatus.ASSIGNED) assignedCount++;
        if (queuedReloaded.getStatus() == RequestStatus.ASSIGNED) assignedCount++;

        assertThat(assignedCount)
                .as("Staff can only be actively assigned to at most 1 task")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("4. Multiple automatic workers processing queued requests: distinct assignments without duplicate claims")
    void multipleAutoWorkers_ProcessingQueuedRequests_NoDuplicates() throws Exception {
        AmenityRequest q1 = savedRequest("p1", "CSE", RequestStatus.QUEUED, LocalDateTime.now().minusMinutes(5));
        AmenityRequest q2 = savedRequest("p2", "CSE", RequestStatus.QUEUED, LocalDateTime.now().minusMinutes(3));
        AmenityRequest q3 = savedRequest("p3", "CSE", RequestStatus.QUEUED, LocalDateTime.now().minusMinutes(1));

        Staff s1 = savedStaff("w1", "CSE");
        Staff s2 = savedStaff("w2", "CSE");
        Staff s3 = savedStaff("w3", "CSE");

        int threadCount = 3;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        List<Staff> workers = List.of(s1, s2, s3);
        for (Staff w : workers) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    adminAmenityService.tryAssignQueuedRequest(w);
                } catch (Exception ignored) {
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        List<AmenityRequest> all = requestRepo.findAll();
        assertThat(all).allMatch(r -> r.getStatus() == RequestStatus.ASSIGNED);

        List<String> assignedStaffIds = all.stream()
                .map(r -> r.getAssignedStaff().getStaffId())
                .distinct()
                .toList();
        assertThat(assignedStaffIds).hasSize(3);

        List<Staff> allStaff = staffRepo.findAll();
        assertThat(allStaff).allMatch(s -> !s.isAvailable());
    }

    @Test
    @DisplayName("5. Two workers monitoring same overdue request: idempotent transition and no duplicate breaches")
    void twoWorkers_MonitoringSameOverdueRequest_Idempotent() throws Exception {
        AmenityRequest overdueReq = savedRequest("p-overdue", "CSE", RequestStatus.PENDING, LocalDateTime.now().minusMinutes(10));
        overdueReq.setAssignmentDeadline(LocalDateTime.now().minusMinutes(8));
        requestRepo.save(overdueReq);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    slaMonitor.monitorSla();
                } catch (Exception ignored) {
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        AmenityRequest reloaded = requestRepo.findById(overdueReq.getId()).orElseThrow();
        assertThat(reloaded.getAssignmentSlaBreached()).isTrue();

        // Repeated execution should remain idempotent
        slaMonitor.monitorSla();
        AmenityRequest reloadedAgain = requestRepo.findById(overdueReq.getId()).orElseThrow();
        assertThat(reloadedAgain.getAssignmentSlaBreached()).isTrue();
    }

    @Test
    @DisplayName("6. Cross-department assignment attempts remain rejected")
    void crossDepartmentAssignment_IsRejected() {
        Staff cseStaff = savedStaff("cse-staff", "CSE");
        AmenityRequest eceReq = savedRequest("ece-prof", "ECE", RequestStatus.PENDING, LocalDateTime.now());

        org.junit.jupiter.api.Assertions.assertThrows(AccessDeniedException.class, () ->
                adminAmenityService.assignStaff(eceReq.getId(), cseStaff.getStaffId(), "CSE")
        );

        AmenityRequest reloaded = requestRepo.findById(eceReq.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(reloaded.getAssignedStaff()).isNull();

        Staff staffReloaded = staffRepo.findById(cseStaff.getStaffId()).orElseThrow();
        assertThat(staffReloaded.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("7. Failed assignment transaction leaves request and staff records completely consistent")
    void failedAssignment_RollsBackCleanly() {
        Staff unavailableStaff = savedStaff("busy-staff", "CSE");
        unavailableStaff.setAvailable(false);
        staffRepo.save(unavailableStaff);

        AmenityRequest req = savedRequest("prof-clean", "CSE", RequestStatus.PENDING, LocalDateTime.now());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                adminAmenityService.assignStaff(req.getId(), unavailableStaff.getStaffId(), "CSE")
        );

        AmenityRequest reloadedReq = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(reloadedReq.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(reloadedReq.getAssignedStaff()).isNull();
        assertThat(reloadedReq.getDeliveryConfirmationCode()).isNull();

        Staff reloadedStaff = staffRepo.findById(unavailableStaff.getStaffId()).orElseThrow();
        assertThat(reloadedStaff.isAvailable()).isFalse();
    }
}
