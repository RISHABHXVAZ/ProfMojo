package com.profmojo.unit;

import com.profmojo.controllers.NotificationController;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.repositories.NotificationRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Unit Tests")
class NotificationControllerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DepartmentSecretRepository departmentSecretRepository;

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
    @DisplayName("getAdminNotifications: Resolves department from secret when not passed")
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
                .isRead(false)
                .build();
        when(notificationRepository.findTop50ByRecipientRoleAndDepartmentOrderByCreatedAtDesc("ADMIN", "CSE"))
                .thenReturn(List.of(n));

        ResponseEntity<?> resp = controller.getAdminNotifications(auth, null);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));
    }

    @Test
    @DisplayName("getProfessorNotifications: Resolves profId from Professor principal")
    void getProfessor_ResolvesProfIdFromPrincipal() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                professor, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        Notification n = Notification.builder()
                .id(2L)
                .recipientId("PROF_TEST")
                .recipientRole("PROFESSOR")
                .message("Prof note")
                .isRead(false)
                .build();
        when(notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc("PROF_TEST"))
                .thenReturn(List.of(n));

        ResponseEntity<?> resp = controller.getProfessorNotifications(auth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));
    }

    @Test
    @DisplayName("getStaffNotifications: Resolves staffId from Staff principal")
    void getStaff_ResolvesStaffIdFromPrincipal() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staff, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        Notification n = Notification.builder()
                .id(3L)
                .recipientId("STAFF_TEST")
                .recipientRole("STAFF")
                .message("Staff note")
                .isRead(false)
                .build();
        when(notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc("STAFF_TEST"))
                .thenReturn(List.of(n));

        ResponseEntity<?> resp = controller.getStaffNotifications(auth);
        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals(1L, body.get("unreadCount"));
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
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(n));

        ResponseEntity<?> resp = controller.markAsRead(10L, auth);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(n.getIsRead());
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("markAsRead: Non-owner cannot mark notification as read")
    void markAsRead_Unauthorized_Returns400() {
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
        when(notificationRepository.findById(20L)).thenReturn(Optional.of(n));

        ResponseEntity<?> resp = controller.markAsRead(20L, auth);
        assertEquals(400, resp.getStatusCode().value());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteNotification: Authorized owner deletes successfully")
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
        when(notificationRepository.findById(30L)).thenReturn(Optional.of(n));

        ResponseEntity<?> resp = controller.deleteNotification(30L, auth);
        assertEquals(200, resp.getStatusCode().value());
        verify(notificationRepository).delete(n);
    }
}
