package com.profmojo.unit;

import com.profmojo.controllers.NotificationController;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.dto.NotificationResponseDTO;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Unit Tests")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private DepartmentSecretRepository departmentSecretRepository;

    @Mock
    private com.profmojo.services.NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationController controller;

    private Professor professor;
    private Staff staff;

    @BeforeEach
    void setUp() {
        professor = Professor.builder()
                .profId("PROF_TEST")
                .name("Test Professor")
                .department("CSE")
                .build();

        staff = Staff.builder()
                .staffId("STAFF_TEST")
                .name("Test Staff")
                .department("CSE")
                .build();
    }

    @Test
    @DisplayName("getAdminNotifications: Returns 401 if unauthenticated")
    void getAdmin_Unauthenticated_Returns401() {
        ResponseEntity<?> resp = controller.getAdminNotifications(null, "CSE");
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    @DisplayName("getAdminNotifications: Resolves department and returns NotificationResponseDTO list")
    void getAdmin_ResolvesDeptFromSecret() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "SECRET_KEY", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        DepartmentSecret sec = new DepartmentSecret();
        sec.setDepartment("CSE");
        sec.setSecretKey("SECRET_KEY");
        when(departmentSecretRepository.findBySecretKey("SECRET_KEY")).thenReturn(Optional.of(sec));

        Notification n = Notification.builder()
                .id(1L)
                .recipientRole("ADMIN")
                .department("CSE")
                .message("Test")
                .type("info")
                .eventType("TEST_EVENT")
                .entityId(100L)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationService.getAdminNotifications("CSE"))
                .thenReturn(List.of(n));

        ResponseEntity<?> resp = controller.getAdminNotifications(auth, null);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));

        @SuppressWarnings("unchecked")
        List<NotificationResponseDTO> list = (List<NotificationResponseDTO>) body.get("notifications");
        assertEquals(1, list.size());
        NotificationResponseDTO dto = list.get(0);
        assertEquals(1L, dto.getId());
        assertEquals("Test", dto.getMessage());
        assertEquals("info", dto.getType());
        assertEquals("TEST_EVENT", dto.getEventType());
        assertEquals(100L, dto.getEntityId());
        assertEquals("CSE", dto.getDepartment());
        assertFalse(dto.getIsRead());
        verify(notificationService).getAdminNotifications("CSE");
    }

    @Test
    @DisplayName("getProfessorNotifications: Resolves profId and returns NotificationResponseDTO list")
    void getProfessor_ResolvesProfIdFromPrincipal() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                professor, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        Notification n = Notification.builder()
                .id(2L)
                .recipientId("PROF_TEST")
                .recipientRole("PROFESSOR")
                .message("Prof note")
                .type("success")
                .eventType("REQUEST_ASSIGNED")
                .entityId(200L)
                .department("CSE")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationService.getNotificationHistory("PROF_TEST"))
                .thenReturn(List.of(n));

        ResponseEntity<?> resp = controller.getProfessorNotifications(auth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));

        @SuppressWarnings("unchecked")
        List<NotificationResponseDTO> list = (List<NotificationResponseDTO>) body.get("notifications");
        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).getId());
        assertEquals("Prof note", list.get(0).getMessage());
        assertEquals("success", list.get(0).getType());
        verify(notificationService).getNotificationHistory("PROF_TEST");
    }

    @Test
    @DisplayName("getStaffNotifications: Resolves staffId and returns NotificationResponseDTO list")
    void getStaff_ResolvesStaffIdFromPrincipal() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staff, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        Notification n = Notification.builder()
                .id(3L)
                .recipientId("STAFF_TEST")
                .recipientRole("STAFF")
                .message("Staff note")
                .type("info")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationService.getNotificationHistory("STAFF_TEST"))
                .thenReturn(List.of(n));

        ResponseEntity<?> resp = controller.getStaffNotifications(auth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));

        @SuppressWarnings("unchecked")
        List<NotificationResponseDTO> list = (List<NotificationResponseDTO>) body.get("notifications");
        assertEquals(1, list.size());
        assertEquals(3L, list.get(0).getId());
        assertEquals("Staff note", list.get(0).getMessage());
        verify(notificationService).getNotificationHistory("STAFF_TEST");
    }

    @Test
    @DisplayName("markAsRead: Admin can mark admin notification as read even if recipientId is null")
    void markAsRead_Admin_NullRecipientId_Success() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "SECRET_KEY", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        DepartmentSecret sec = new DepartmentSecret();
        sec.setDepartment("CSE");
        when(departmentSecretRepository.findBySecretKey("SECRET_KEY")).thenReturn(Optional.of(sec));

        Notification n = Notification.builder()
                .id(10L)
                .recipientId(null)
                .recipientRole("ADMIN")
                .department("CSE")
                .message("Admin note")
                .isRead(false)
                .build();
        when(notificationService.findById(10L)).thenReturn(Optional.of(n));

        ResponseEntity<?> resp = controller.markAsRead(10L, auth);
        assertEquals(200, resp.getStatusCode().value());
        verify(notificationService).markAsRead(10L);
    }

    @Test
    @DisplayName("markAsRead: Non-owner cannot mark notification as read (403 Forbidden)")
    void markAsRead_Unauthorized_Returns403() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                professor, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        Notification n = Notification.builder()
                .id(20L)
                .recipientId("OTHER_PROF")
                .recipientRole("PROFESSOR")
                .message("Other prof note")
                .isRead(false)
                .build();
        when(notificationService.findById(20L)).thenReturn(Optional.of(n));

        ResponseEntity<?> resp = controller.markAsRead(20L, auth);
        assertEquals(403, resp.getStatusCode().value());
        verify(notificationService, never()).markAsRead(anyLong());
    }

    @Test
    @DisplayName("markAsRead: Non-existent notification returns 404 Not Found")
    void markAsRead_NotFound_Returns404() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                professor, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        when(notificationService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.markAsRead(999L, auth);
        assertEquals(404, resp.getStatusCode().value());
        verify(notificationService, never()).markAsRead(anyLong());
    }

    @Test
    @DisplayName("markAllAsRead: Admin calls markAllAsReadForAdmin with department")
    void markAllAsRead_Admin_CallsServiceWithDept() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "SECRET_KEY", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        DepartmentSecret sec = new DepartmentSecret();
        sec.setDepartment("CSE");
        when(departmentSecretRepository.findBySecretKey("SECRET_KEY")).thenReturn(Optional.of(sec));
        when(notificationService.markAllAsReadForAdmin("CSE")).thenReturn(7);

        ResponseEntity<?> resp = controller.markAllAsRead(auth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(7, body.get("count"));
        verify(notificationService).markAllAsReadForAdmin("CSE");
    }

    @Test
    @DisplayName("markAllAsRead: Non-admin user calls markAllAsRead with userId")
    void markAllAsRead_User_CallsServiceWithUserId() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                professor, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        when(notificationService.markAllAsRead("PROF_TEST")).thenReturn(3);

        ResponseEntity<?> resp = controller.markAllAsRead(auth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(3, body.get("count"));
        verify(notificationService).markAllAsRead("PROF_TEST");
    }

    @Test
    @DisplayName("deleteNotification: Authorized owner deletes successfully (soft delete / archive)")
    void deleteNotification_Authorized_Deletes() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staff, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        Notification n = Notification.builder()
                .id(30L)
                .recipientId("STAFF_TEST")
                .recipientRole("STAFF")
                .message("Delete me")
                .build();
        when(notificationService.findById(30L)).thenReturn(Optional.of(n));

        ResponseEntity<?> resp = controller.deleteNotification(30L, auth);
        assertEquals(200, resp.getStatusCode().value());
        verify(notificationService).deleteNotification(30L);
    }
}
