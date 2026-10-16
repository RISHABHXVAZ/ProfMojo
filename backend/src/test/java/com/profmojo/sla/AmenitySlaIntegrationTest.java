package com.profmojo.sla;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.scheduler.AmenitySlaMonitor;
import com.profmojo.services.AdminAmenityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Phase 2 — Amenity SLA Processing & Idempotency Integration Tests")
class AmenitySlaIntegrationTest extends BasePostgresContainerTest {

    @Autowired private AmenityRequestRepository requestRepo;
    @Autowired private StaffRepository staffRepo;
    @Autowired private AmenitySlaMonitor slaMonitor;
    @Autowired private AdminAmenityService adminAmenityService;

    @BeforeEach
    void cleanDb() {
        requestRepo.deleteAll();
        staffRepo.deleteAll();
    }

    private AmenityRequest createRequest(String profId, String dept, RequestStatus status, LocalDateTime deadline) {
        return requestRepo.save(AmenityRequest.builder()
                .professorId(profId)
                .professorName("Prof " + profId)
                .department(dept)
                .classRoom("LH-101")
                .status(status)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .assignmentDeadline(deadline)
                .assignmentSlaBreached(false)
                .deliverySlaBreached(false)
                .slaBreached(false)
                .build());
    }

    @Test
    @DisplayName("1. Before deadline: SLA monitor does NOT breach assignment SLA")
    void beforeDeadline_MonitorDoesNotBreach() {
        // Deadline in the future
        LocalDateTime futureDeadline = LocalDateTime.now().plusMinutes(5);
        AmenityRequest req = createRequest("prof-1", "CSE", RequestStatus.PENDING, futureDeadline);

        slaMonitor.monitorSla();

        AmenityRequest reloaded = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getAssignmentSlaBreached()).isFalse();
    }

    @Test
    @DisplayName("2. At or after deadline: qualifying unassigned request is marked breached")
    void atOrAfterDeadline_UnassignedRequestMarkedBreached() {
        // Deadline in the past
        LocalDateTime pastDeadline = LocalDateTime.now().minusMinutes(1);
        AmenityRequest req = createRequest("prof-2", "CSE", RequestStatus.PENDING, pastDeadline);

        slaMonitor.monitorSla();

        AmenityRequest reloaded = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getAssignmentSlaBreached()).isTrue();
    }

    @Test
    @DisplayName("3. Repeated scheduler runs are idempotent and do not re-process")
    void repeatedRuns_AreIdempotent() {
        LocalDateTime pastDeadline = LocalDateTime.now().minusMinutes(3);
        AmenityRequest req = createRequest("prof-3", "CSE", RequestStatus.PENDING, pastDeadline);

        // Run 1
        slaMonitor.monitorSla();
        AmenityRequest run1 = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(run1.getAssignmentSlaBreached()).isTrue();

        // Run 2
        slaMonitor.monitorSla();
        AmenityRequest run2 = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(run2.getAssignmentSlaBreached()).isTrue();
    }

    @Test
    @DisplayName("4. Assignment before deadline prevents assignment breach")
    void assignmentBeforeDeadline_PreventsBreach() {
        Staff staff = staffRepo.save(Staff.builder()
                .staffId("staff-on-time")
                .name("Bob Staff")
                .email("bob@test.com")
                .password("pass")
                .department("CSE")
                .available(true)
                .build());

        LocalDateTime futureDeadline = LocalDateTime.now().plusMinutes(5);
        AmenityRequest req = createRequest("prof-4", "CSE", RequestStatus.PENDING, futureDeadline);

        // Assign before deadline
        adminAmenityService.assignStaff(req.getId(), staff.getStaffId(), "CSE");

        slaMonitor.monitorSla();

        AmenityRequest reloaded = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(reloaded.getAssignmentSlaBreached()).isFalse();
    }

    @Test
    @DisplayName("5. Delivery SLA: overdue ASSIGNED request is marked deliverySlaBreached and slaBreached")
    void deliverySla_OverdueAssignedRequestMarkedBreached() {
        Staff staff = staffRepo.save(Staff.builder()
                .staffId("staff-delivery")
                .name("Charlie Staff")
                .email("charlie@test.com")
                .password("pass")
                .department("CSE")
                .available(false)
                .build());

        AmenityRequest req = createRequest("prof-5", "CSE", RequestStatus.ASSIGNED, LocalDateTime.now().plusMinutes(2));
        req.setAssignedStaff(staff);
        req.setDeliveryDeadline(LocalDateTime.now().minusMinutes(1));
        req.setSlaDeadline(LocalDateTime.now().minusMinutes(1));
        requestRepo.save(req);

        slaMonitor.monitorSla();

        AmenityRequest reloaded = requestRepo.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getDeliverySlaBreached()).isTrue();
        assertThat(reloaded.isSlaBreached()).isTrue();
    }

    @Test
    @DisplayName("6. Legacy record with null assignmentDeadline uses createdAt fallback safely")
    void legacyRecord_NullDeadline_UsesFallbackSafely() {
        AmenityRequest legacyRecent = requestRepo.save(AmenityRequest.builder()
                .professorId("prof-recent")
                .department("CSE")
                .classRoom("LH-101")
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now().minusSeconds(30)) // less than 2 min ago
                .assignmentDeadline(null) // legacy
                .assignmentSlaBreached(false)
                .build());

        AmenityRequest legacyOld = requestRepo.save(AmenityRequest.builder()
                .professorId("prof-old")
                .department("CSE")
                .classRoom("LH-102")
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(5)) // more than 2 min ago
                .assignmentDeadline(null) // legacy
                .assignmentSlaBreached(false)
                .build());

        slaMonitor.monitorSla();

        AmenityRequest reloadedRecent = requestRepo.findById(legacyRecent.getId()).orElseThrow();
        assertThat(reloadedRecent.getAssignmentSlaBreached()).isFalse();

        AmenityRequest reloadedOld = requestRepo.findById(legacyOld.getId()).orElseThrow();
        assertThat(reloadedOld.getAssignmentSlaBreached()).isTrue();
    }
}
