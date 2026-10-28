package com.profmojo.security;

import com.profmojo.controllers.NotificationController;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.Notification;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1 Security - Admin Notification Department Isolation Tests")
class NotificationSecurityTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private DepartmentSecretRepository departmentSecretRepository;

    @Mock
    private com.profmojo.services.NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationController controller;

    private UsernamePasswordAuthenticationToken adminAuth;
    private DepartmentSecret cseSecret;

    @BeforeEach
    void setUp() {
        adminAuth = new UsernamePasswordAuthenticationToken(
                "SECRET_CSE", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        cseSecret = new DepartmentSecret();
        cseSecret.setSecretKey("SECRET_CSE");
        cseSecret.setDepartment("CSE");
        cseSecret.setActive(true);
    }

    @Test
    @DisplayName("getAdminNotifications: Foreign department parameter is rejected with 403 Forbidden")
    void getAdminNotifications_ForeignDepartmentParam_Returns403() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));

        ResponseEntity<?> response = controller.getAdminNotifications(adminAuth, "ECE");

        assertEquals(403, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(((String) body.get("error")).contains("Cannot access notifications for another department"));
        verify(notificationService, never()).getAdminNotifications(any());
    }

    @Test
    @DisplayName("getAdminNotifications: Inactive or unresolvable secret fails closed with 403 Forbidden")
    void getAdminNotifications_UnresolvableSecret_Returns403() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getAdminNotifications(adminAuth, null);

        assertEquals(403, response.getStatusCode().value());
        verify(notificationService, never()).getAdminNotifications(any());
    }

    @Test
    @DisplayName("getAdminNotifications: Legitimate admin receives only their department notifications")
    void getAdminNotifications_LegitimateAdmin_Returns200WithOnlyOwnDepartment() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));

        Notification n = Notification.builder()
                .id(1L)
                .recipientRole("ADMIN")
                .department("CSE")
                .message("CSE Notification")
                .isRead(false)
                .build();

        when(notificationService.getAdminNotifications("CSE"))
                .thenReturn(List.of(n));

        ResponseEntity<?> response = controller.getAdminNotifications(adminAuth, "CSE");

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1L, body.get("unreadCount"));
        verify(notificationService).getAdminNotifications("CSE");
    }

    @Test
    @DisplayName("markAsRead: Admin cannot mark notification of another department as read (403 Forbidden)")
    void markAsRead_CrossDepartmentNotification_Returns403() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));

        Notification eceNotification = Notification.builder()
                .id(50L)
                .recipientRole("ADMIN")
                .department("ECE")
                .message("ECE notification")
                .isRead(false)
                .build();

        when(notificationService.findById(50L)).thenReturn(Optional.of(eceNotification));

        ResponseEntity<?> response = controller.markAsRead(50L, adminAuth);

        assertEquals(403, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(((String) body.get("error")).contains("Unauthorized"));
        verify(notificationService, never()).markAsRead(anyLong());
    }

    @Test
    @DisplayName("markAsRead: Non-existent notification returns 404 Not Found")
    void markAsRead_NonExistentNotification_Returns404() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));
        when(notificationService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.markAsRead(999L, adminAuth);

        assertEquals(404, response.getStatusCode().value());
        verify(notificationService, never()).markAsRead(anyLong());
    }

    @Test
    @DisplayName("deleteNotification: Admin cannot delete notification of another department (403 Forbidden)")
    void deleteNotification_CrossDepartmentNotification_Returns403() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));

        Notification eceNotification = Notification.builder()
                .id(60L)
                .recipientRole("ADMIN")
                .department("ECE")
                .message("ECE notification")
                .isRead(false)
                .build();

        when(notificationService.findById(60L)).thenReturn(Optional.of(eceNotification));

        ResponseEntity<?> response = controller.deleteNotification(60L, adminAuth);

        assertEquals(403, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(((String) body.get("error")).contains("Unauthorized"));
        verify(notificationService, never()).deleteNotification(anyLong());
    }

    @Test
    @DisplayName("deleteNotification: Non-existent notification returns 404 Not Found")
    void deleteNotification_NonExistentNotification_Returns404() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));
        when(notificationService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deleteNotification(999L, adminAuth);

        assertEquals(404, response.getStatusCode().value());
        verify(notificationService, never()).deleteNotification(anyLong());
    }
}
