package com.profmojo.concurrency;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for the FIFO staff-assignment logic.
 *
 * <p>Each test starts real PostgreSQL via Testcontainers (inherited from
 * {@link BasePostgresContainerTest}) and fires multiple threads that call
 * {@link AdminAmenityService#tryAssignQueuedRequest(Staff)} simultaneously.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ConcurrentStaffAssignmentTest extends BasePostgresContainerTest {

    @Autowired private AdminAmenityService adminAmenityService;
    @Autowired private AmenityRequestRepository requestRepo;
    @Autowired private StaffRepository staffRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        requestRepo.deleteAll();
        staffRepo.deleteAll();
    }

    // ------------------------------------------------------------------ //
    //  Helper builders                                                     //
    // ------------------------------------------------------------------ //

    private Staff savedStaff(String id) {
        return staffRepo.save(Staff.builder()
                .staffId(id)
                .name("Staff " + id)
                .password(passwordEncoder.encode("pass"))
                .department("CSE")
                .contactNo("0000000000")
                .email(id + "@test.com")
                .available(true)
                .online(true)
                .build());
    }

    private AmenityRequest queuedRequest(LocalDateTime createdAt) {
        return requestRepo.save(AmenityRequest.builder()
                .professorId("prof-1")
                .professorName("Prof One")
                .department("CSE")
                .classRoom("Room 101")
                .status(RequestStatus.QUEUED)
                .createdAt(createdAt)
                .build());
    }

    // ------------------------------------------------------------------ //
    //  Test 1 — single queued request, 3 concurrent staff                 //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Exactly one staff is assigned when 3 threads race for a single QUEUED request")
    void singleRequest_threeRacingStaff_exactlyOneAssignment() throws InterruptedException {

        // Arrange — 1 QUEUED request, 3 available staff
        AmenityRequest req = queuedRequest(LocalDateTime.now());
        Staff s1 = savedStaff("staff-c1");
        Staff s2 = savedStaff("staff-c2");
        Staff s3 = savedStaff("staff-c3");

        int threadCount = 3;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        List<Staff> racers = List.of(s1, s2, s3);
        List<Throwable> errors = new ArrayList<>();

        for (Staff racer : racers) {
            pool.submit(() -> {
                try {
                    startGate.await();                          // wait for the gun
                    adminAmenityService.tryAssignQueuedRequest(racer);
                } catch (Throwable t) {
                    synchronized (errors) { errors.add(t); }
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();                                  // fire!
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // Assert — no unexpected exceptions
        assertThat(errors)
                .as("No thread should throw an unexpected exception")
                .isEmpty();

        // Assert — exactly ONE request is now ASSIGNED
        AmenityRequest result = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(result.getStatus())
                .as("Request must be ASSIGNED")
                .isEqualTo(RequestStatus.ASSIGNED);
        assertThat(result.getAssignedStaff())
                .as("Request must have an assigned staff member")
                .isNotNull();

        // Assert — exactly ONE staff member is unavailable
        List<Staff> allStaff = staffRepo.findAll();
        long unavailable = allStaff.stream().filter(s -> !s.isAvailable()).count();
        assertThat(unavailable)
                .as("Exactly one staff member should be marked unavailable")
                .isEqualTo(1L);

        // Assert — exactly two staff members are still available
        long available = allStaff.stream().filter(Staff::isAvailable).count();
        assertThat(available)
                .as("Two staff members should remain available")
                .isEqualTo(2L);
    }

    // ------------------------------------------------------------------ //
    //  Test 2 — two queued requests, FIFO order preserved under concurrency//
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("FIFO ordering is preserved: older request A is always assigned before newer request B")
    void twoRequests_twoRacingStaff_fifoOrderPreserved() throws InterruptedException {

        // Arrange — request A is older than request B
        LocalDateTime earlier = LocalDateTime.now().minusMinutes(5);
        LocalDateTime later   = LocalDateTime.now();

        AmenityRequest reqA = queuedRequest(earlier);
        AmenityRequest reqB = queuedRequest(later);

        Staff s1 = savedStaff("staff-f1");
        Staff s2 = savedStaff("staff-f2");

        int threadCount = 2;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        List<Throwable> errors = new ArrayList<>();

        for (Staff racer : List.of(s1, s2)) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    adminAmenityService.tryAssignQueuedRequest(racer);
                } catch (Throwable t) {
                    synchronized (errors) { errors.add(t); }
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishLine.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(errors)
                .as("No thread should throw an unexpected exception")
                .isEmpty();

        // Both requests should be ASSIGNED (2 staff, 2 requests)
        AmenityRequest resultA = requestRepo.findById(reqA.getId()).orElseThrow();
        AmenityRequest resultB = requestRepo.findById(reqB.getId()).orElseThrow();

        assertThat(resultA.getStatus())
                .as("Older request A must be ASSIGNED")
                .isEqualTo(RequestStatus.ASSIGNED);
        assertThat(resultB.getStatus())
                .as("Newer request B must also be ASSIGNED")
                .isEqualTo(RequestStatus.ASSIGNED);

        // FIFO: A was created earlier, so it must have been assigned first
        // (i.e. A.assignedAt <= B.assignedAt, or at worst equal in the same instant)
        assertThat(resultA.getAssignedAt())
                .as("Request A (older) should be assigned at the same time or before request B")
                .isBeforeOrEqualTo(resultB.getAssignedAt());

        // Both staff members should now be unavailable
        List<Staff> allStaff = staffRepo.findAll();
        long unavailable = allStaff.stream().filter(s -> !s.isAvailable()).count();
        assertThat(unavailable)
                .as("Both staff members should be marked unavailable")
                .isEqualTo(2L);
    }
}
