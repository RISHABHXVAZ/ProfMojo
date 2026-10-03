package com.profmojo.integration;

import com.profmojo.controllers.NotificationController;
import com.profmojo.controllers.StaffAmenityController;
import com.profmojo.models.*;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.*;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.AmenityRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Comprehensive Notification Subsystem Integration Tests")
class NotificationIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private DepartmentSecretRepository secretRepo;

    @Autowired
    private ProfessorRepository profRepo;

    @Autowired
    private StaffRepository staffRepo;

    @Autowired
    private AmenityRequestRepository amenityRepo;

    @Autowired
    private AmenityRequestService amenityRequestService;

    @Autowired
    private AdminAmenityService adminAmenityService;

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private StaffAmenityController staffAmenityController;

    private Professor testProfessor;
    private Staff testStaff;
    private DepartmentSecret cseSecret;
    private DepartmentSecret eceSecret;

    @BeforeEach
    void setUp() {
        notificationRepo.deleteAll();
        amenityRepo.deleteAll();
        staffRepo.deleteAll();
        profRepo.deleteAll();
        secretRepo.deleteAll();

        // 1. Department Secrets
        cseSecret = new DepartmentSecret();
        cseSecret.setDepartment("CSE");
        cseSecret.setAdminEmail("cse-admin@profmojo.edu");
        cseSecret.setSecretKey("SEC_CSE_ADMIN_KEY");
        cseSecret.setActive(true);
        secretRepo.save(cseSecret);

        eceSecret = new DepartmentSecret();
        eceSecret.setDepartment("ECE");
        eceSecret.setAdminEmail("ece-admin@profmojo.edu");
        eceSecret.setSecretKey("SEC_ECE_ADMIN_KEY");
        eceSecret.setActive(true);
        secretRepo.save(eceSecret);

        // 2. Professor
        testProfessor = Professor.builder()
                .profId("PROF_NOTIF_01")
                .name("Prof. Ada Lovelace")
                .department("CSE")
                .email("ada@profmojo.edu")
                .password("encoded_pass")
                .build();
        profRepo.save(testProfessor);

        // 3. Staff
        testStaff = Staff.builder()
                .staffId("STAFF_NOTIF_01")
                .name("Alan Turing")
                .department("CSE")
                .email("alan@profmojo.edu")
                .password("encoded_pass")
                .available(true)
                .stars(5)
                .build();
        staffRepo.save(testStaff);
    }

    @Test
    @DisplayName("1. Notification Persistence & Schema Invariants")
    void notificationPersistence_CorrectFields() {
        Notification notif = Notification.builder()
                .recipientId(testProfessor.getProfId())
                .recipientRole("PROFESSOR")
                .department("CSE")
                .message("Your classroom projector has been approved.")
                .type("info")
                .eventType("REQUEST_QUEUED")
                .notificationKey("TEST_KEY_01")
                .entityId(101L)
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepo.save(notif);
        assertNotNull(saved.getId());
        assertEquals("PROF_NOTIF_01", saved.getRecipientId());
        assertEquals("PROFESSOR", saved.getRecipientRole());
        assertEquals("CSE", saved.getDepartment());
        assertFalse(saved.getIsRead());
        assertFalse(saved.getIsArchived());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("2. Department Isolation: CSE Admin cannot see ECE Admin notifications")
    void departmentIsolation_Verified() {
        Notification cseNotif = Notification.builder()
                .recipientId("ADMIN-CSE")
                .recipientRole("ADMIN")
                .department("CSE")
                .message("New request in CSE Lab 1")
                .type("info")
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(cseNotif);

        Notification eceNotif = Notification.builder()
                .recipientId("ADMIN-ECE")
                .recipientRole("ADMIN")
                .department("ECE")
                .message("New request in ECE Circuit Lab")
                .type("info")
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(eceNotif);

        // Authenticate as CSE Admin
        UsernamePasswordAuthenticationToken cseAuth = new UsernamePasswordAuthenticationToken(
                "SEC_CSE_ADMIN_KEY", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ResponseEntity<?> cseResponse = notificationController.getAdminNotifications(cseAuth, "CSE");
        assertEquals(200, cseResponse.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> cseBody = (Map<String, Object>) cseResponse.getBody();
        @SuppressWarnings("unchecked")
        List<Notification> cseList = (List<Notification>) cseBody.get("notifications");
        assertEquals(1, cseList.size());
        assertEquals("New request in CSE Lab 1", cseList.get(0).getMessage());

        // Authenticate as ECE Admin
        UsernamePasswordAuthenticationToken eceAuth = new UsernamePasswordAuthenticationToken(
                "SEC_ECE_ADMIN_KEY", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ResponseEntity<?> eceResponse = notificationController.getAdminNotifications(eceAuth, "ECE");
        assertEquals(200, eceResponse.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> eceBody = (Map<String, Object>) eceResponse.getBody();
        @SuppressWarnings("unchecked")
        List<Notification> eceList = (List<Notification>) eceBody.get("notifications");
        assertEquals(1, eceList.size());
        assertEquals("New request in ECE Circuit Lab", eceList.get(0).getMessage());
    }

    @Test
    @DisplayName("3. Principal Resolution: Professor entity principal resolves profId correctly")
    void professorPrincipalResolution_Success() {
        Notification profNotif = Notification.builder()
                .recipientId(testProfessor.getProfId())
                .recipientRole("PROFESSOR")
                .department("CSE")
                .message("Notification for Prof Lovelace")
                .type("info")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(profNotif);

        // SecurityContext principal is the Professor model instance
        UsernamePasswordAuthenticationToken profAuth = new UsernamePasswordAuthenticationToken(
                testProfessor, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        ResponseEntity<?> resp = notificationController.getProfessorNotifications(profAuth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));
        @SuppressWarnings("unchecked")
        List<Notification> list = (List<Notification>) body.get("notifications");
        assertEquals(1, list.size());
        assertEquals("Notification for Prof Lovelace", list.get(0).getMessage());
    }

    @Test
    @DisplayName("4. Principal Resolution: Staff entity principal resolves staffId correctly")
    void staffPrincipalResolution_Success() {
        Notification staffNotif = Notification.builder()
                .recipientId(testStaff.getStaffId())
                .recipientRole("STAFF")
                .department("CSE")
                .message("Notification for Staff Turing")
                .type("info")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(staffNotif);

        // SecurityContext principal is the Staff model instance
        UsernamePasswordAuthenticationToken staffAuth = new UsernamePasswordAuthenticationToken(
                testStaff, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        ResponseEntity<?> resp = notificationController.getStaffNotifications(staffAuth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));
        @SuppressWarnings("unchecked")
        List<Notification> list = (List<Notification>) body.get("notifications");
        assertEquals(1, list.size());
        assertEquals("Notification for Staff Turing", list.get(0).getMessage());
    }

    @Test
    @DisplayName("5. Mark As Read & Mark All As Read Lifecycle for Admin and Users")
    void markAsReadLifecycle_Success() {
        Notification n1 = Notification.builder()
                .recipientId("ADMIN-CSE")
                .recipientRole("ADMIN")
                .department("CSE")
                .message("Admin Task 1")
                .type("info")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Notification n2 = Notification.builder()
                .recipientId("ADMIN-CSE")
                .recipientRole("ADMIN")
                .department("CSE")
                .message("Admin Task 2")
                .type("info")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.saveAll(List.of(n1, n2));

        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "SEC_CSE_ADMIN_KEY", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Mark single as read
        ResponseEntity<?> markOne = notificationController.markAsRead(n1.getId(), adminAuth);
        assertEquals(200, markOne.getStatusCode().value());

        Notification reloadedN1 = notificationRepo.findById(n1.getId()).orElseThrow();
        assertTrue(reloadedN1.getIsRead());
        assertNotNull(reloadedN1.getReadAt());

        // Check unread count
        ResponseEntity<?> adminCheck = notificationController.getAdminNotifications(adminAuth, "CSE");
        @SuppressWarnings("unchecked")
        Map<String, Object> checkBody = (Map<String, Object>) adminCheck.getBody();
        assertEquals(1L, checkBody.get("unreadCount"));

        // Mark all as read
        ResponseEntity<?> markAll = notificationController.markAllAsRead(adminAuth);
        assertEquals(200, markAll.getStatusCode().value());

        Notification reloadedN2 = notificationRepo.findById(n2.getId()).orElseThrow();
        assertTrue(reloadedN2.getIsRead());

        // Verify unread count is 0 after mark-all
        ResponseEntity<?> finalCheck = notificationController.getAdminNotifications(adminAuth, "CSE");
        @SuppressWarnings("unchecked")
        Map<String, Object> finalBody = (Map<String, Object>) finalCheck.getBody();
        assertEquals(0L, finalBody.get("unreadCount"));
    }

    @Test
    @DisplayName("6. Notification Deduplication by Key prevents duplicate rows")
    void deduplication_PreventsDuplicateRows() {
        String key = "TASK_ASSIGNED-500-STAFF_NOTIF_01";

        Notification notif1 = Notification.builder()
                .recipientId(testStaff.getStaffId())
                .recipientRole("STAFF")
                .department("CSE")
                .message("Task assigned")
                .notificationKey(key)
                .type("info")
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepo.save(notif1);

        // Attempt second save with same key
        assertTrue(notificationRepo.findByNotificationKey(key).isPresent());
        // Verify only 1 notification exists in repo
        assertEquals(1, notificationRepo.findByRecipientId(testStaff.getStaffId()).size());
    }

    @Test
    @DisplayName("7. End-to-End: Amenity Delivery creates notifications and auto-assigns next queued request with confirmation code")
    void deliveryCreatesNotifications_AndAutoAssignsQueue() {
        // 1. Create first request and assign to staff
        AmenityRequestDTO dto1 = new AmenityRequestDTO();
        dto1.setClassRoom("LH-101");
        dto1.setDepartment("CSE");
        dto1.setItems(List.of("Marker"));
        AmenityRequest req1 = amenityRequestService.raiseRequest(dto1, testProfessor);
        req1.setAssignedStaff(testStaff);
        req1.setStatus(RequestStatus.ASSIGNED);
        req1.setDeliveryConfirmationCode("1234");
        req1.setConfirmationCodeExpiry(LocalDateTime.now().plusHours(2));
        req1.setDeliveryDeadline(LocalDateTime.now().plusMinutes(5));
        amenityRepo.save(req1);
        testStaff.setAvailable(false);
        staffRepo.save(testStaff);

        // 2. Create second request and queue it
        AmenityRequestDTO dto2 = new AmenityRequestDTO();
        dto2.setClassRoom("LH-102");
        dto2.setDepartment("CSE");
        dto2.setItems(List.of("Duster"));
        AmenityRequest req2 = amenityRequestService.raiseRequest(dto2, testProfessor);
        adminAmenityService.addToQueue(req2.getId());

        // 3. Staff delivers request 1
        ResponseEntity<?> deliveryResp = staffAmenityController.markAsDelivered(
                req1.getId(), "1234", testStaff
        );
        assertEquals(200, deliveryResp.getStatusCode().value());

        // 4. Verify Request 1 is DELIVERED
        AmenityRequest reloaded1 = amenityRepo.findById(req1.getId()).orElseThrow();
        assertEquals(RequestStatus.DELIVERED, reloaded1.getStatus());

        // 5. Verify Request 2 is auto-assigned to testStaff with delivery confirmation code
        AmenityRequest reloaded2 = amenityRepo.findById(req2.getId()).orElseThrow();
        assertEquals(RequestStatus.ASSIGNED, reloaded2.getStatus());
        assertEquals(testStaff.getStaffId(), reloaded2.getAssignedStaff().getStaffId());
        assertNotNull(reloaded2.getDeliveryConfirmationCode(), "Auto-assigned request MUST have confirmation code");

        // 6. Verify notifications created for delivery AND for auto-assignment
        List<Notification> staffNotifications = notificationRepo.findByRecipientId(testStaff.getStaffId());
        assertTrue(staffNotifications.stream().anyMatch(n -> "TASK_DELIVERED".equals(n.getEventType())));
        assertTrue(staffNotifications.stream().anyMatch(n -> "TASK_ASSIGNED".equals(n.getEventType())));

        List<Notification> profNotifications = notificationRepo.findByRecipientId(testProfessor.getProfId());
        assertTrue(profNotifications.stream().anyMatch(n -> "DELIVERY_COMPLETED".equals(n.getEventType())));
        assertTrue(profNotifications.stream().anyMatch(n -> "REQUEST_ASSIGNED".equals(n.getEventType())));
    }
}
