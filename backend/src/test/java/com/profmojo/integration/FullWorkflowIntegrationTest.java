package com.profmojo.integration;

import com.profmojo.models.*;
import com.profmojo.models.dto.AddStaffRequest;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.*;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.AdminOnboardingService;
import com.profmojo.services.AmenityRequestService;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Full Onboarding and Amenity Workflow End-to-End Integration Test")
class FullWorkflowIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private AdminOnboardingService onboardingService;

    @Autowired
    private AmenityRequestService amenityRequestService;

    @Autowired
    private AdminAmenityService adminAmenityService;

    @Autowired
    private ProfessorMasterRepository profMasterRepo;

    @Autowired
    private StudentMasterRepository studentMasterRepo;

    @Autowired
    private StaffRepository staffRepo;

    @Autowired
    private AmenityRequestRepository amenityRepo;

    @Autowired
    private NotificationRepository notifRepo;

    @Test
    @DisplayName("Complete Lifecycle: Onboarding -> Request Creation -> Queue -> FIFO Assignment")
    void fullLifecycleWorkflow_Success() {
        // 1. Admin Onboards Professor
        ProfessorMaster profMaster = TestDataFactory.createProfessorMaster(
                "IT_PROF_01", "Dr. Linus Torvalds", "CSE", "linus@linux.org"
        );
        onboardingService.addProfessor(profMaster);
        assertTrue(profMasterRepo.existsById("IT_PROF_01"));

        // 2. Admin Onboards Student
        StudentMaster studentMaster = TestDataFactory.createStudentMaster(
                "IT_STU_01", "Dennis Ritchie", "CSE", "dennis@c.org"
        );
        onboardingService.addStudent(studentMaster);
        assertTrue(studentMasterRepo.existsById("IT_STU_01"));

        // 3. Admin Onboards Staff
        AddStaffRequest staffReq = new AddStaffRequest();
        staffReq.setStaffId("IT_STAFF_01");
        staffReq.setName("Ken Thompson");
        staffReq.setDepartment("CSE");
        staffReq.setEmail("ken@unix.org");
        staffReq.setContactNo("9876543210");
        onboardingService.addStaff(staffReq);
        assertTrue(staffRepo.existsById("IT_STAFF_01"));

        Staff staff = staffRepo.findById("IT_STAFF_01").orElseThrow();
        assertTrue(staff.isAvailable());

        // 4. Professor raises an Amenity Request
        Professor mockProf = Professor.builder()
                .profId("IT_PROF_01")
                .name("Dr. Linus Torvalds")
                .department("CSE")
                .build();

        AmenityRequestDTO requestDTO = new AmenityRequestDTO();
        requestDTO.setClassRoom("Hall 404");
        requestDTO.setDepartment("CSE");
        requestDTO.setItems(List.of("HDMI Cable", "Marker"));

        AmenityRequest raisedRequest = amenityRequestService.raiseRequest(requestDTO, mockProf);
        assertNotNull(raisedRequest.getId());
        assertEquals(RequestStatus.PENDING, raisedRequest.getStatus());

        // 5. Admin adds request to Queue
        AmenityRequest queued = adminAmenityService.addToQueue(raisedRequest.getId());
        assertEquals(RequestStatus.QUEUED, queued.getStatus());
        assertNotNull(queued.getDeliveryConfirmationCode());

        // Verify Notification was persisted for Professor
        List<Notification> notifs = notifRepo.findByRecipientId("IT_PROF_01");
        assertFalse(notifs.isEmpty());
        assertTrue(notifs.get(0).getMessage().contains("Hall 404"));

        // 6. Automatic FIFO Assignment to available staff
        adminAmenityService.tryAssignQueuedRequest(staff);

        AmenityRequest finalRequest = amenityRepo.findById(raisedRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.ASSIGNED, finalRequest.getStatus());
        assertEquals("IT_STAFF_01", finalRequest.getAssignedStaff().getStaffId());

        Staff updatedStaff = staffRepo.findById("IT_STAFF_01").orElseThrow();
        assertFalse(updatedStaff.isAvailable(), "Staff should be marked unavailable after assignment");
    }
}
