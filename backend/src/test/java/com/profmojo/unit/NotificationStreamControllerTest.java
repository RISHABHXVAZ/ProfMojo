package com.profmojo.unit;

import com.profmojo.controllers.NotificationController;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.StreamTicketData;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.services.NotificationService;
import com.profmojo.services.NotificationSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationStreamController Unit Tests - Stream Tickets & SSE Endpoint")
class NotificationStreamControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private DepartmentSecretRepository departmentSecretRepository;

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationController controller;

    private UsernamePasswordAuthenticationToken adminAuth;
    private UsernamePasswordAuthenticationToken profAuth;
    private DepartmentSecret cseSecret;

    @BeforeEach
    void setUp() {
        adminAuth = new UsernamePasswordAuthenticationToken(
                "SECRET_CSE", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        Professor prof = Professor.builder()
                .profId("PROF101")
                .name("Dr. Smith")
                .department("CSE")
                .build();
        profAuth = new UsernamePasswordAuthenticationToken(
                prof, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        cseSecret = new DepartmentSecret();
        cseSecret.setSecretKey("SECRET_CSE");
        cseSecret.setDepartment("CSE");
        cseSecret.setActive(true);
    }

    // ─── POST /api/notifications/stream-ticket ───────────────────────────────

    @Test
    @DisplayName("createStreamTicket: Unauthenticated request returns 401 Unauthorized")
    void createStreamTicket_Unauthenticated_Returns401() {
        ResponseEntity<?> response = controller.createStreamTicket(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(notificationSseService, never()).createStreamTicket(any(), any(), any());
    }

    @Test
    @DisplayName("createStreamTicket: Admin with invalid or inactive secret returns 403 Forbidden")
    void createStreamTicket_AdminUnresolvableSecret_Returns403() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createStreamTicket(adminAuth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(notificationSseService, never()).createStreamTicket(any(), any(), any());
    }

    @Test
    @DisplayName("createStreamTicket: Legitimate admin receives ticket mapped to ADMIN role and department")
    void createStreamTicket_LegitimateAdmin_Returns200WithTicket() {
        when(departmentSecretRepository.findBySecretKey("SECRET_CSE")).thenReturn(Optional.of(cseSecret));
        when(notificationSseService.createStreamTicket("SECRET_CSE", "ADMIN", "CSE"))
                .thenReturn("valid-admin-ticket-123");

        ResponseEntity<?> response = controller.createStreamTicket(adminAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("valid-admin-ticket-123", body.get("ticket"));
        verify(notificationSseService).createStreamTicket("SECRET_CSE", "ADMIN", "CSE");
    }

    @Test
    @DisplayName("createStreamTicket: Legitimate professor receives ticket mapped to PROFESSOR role and userId")
    void createStreamTicket_LegitimateProfessor_Returns200WithTicket() {
        when(notificationSseService.createStreamTicket("PROF101", "PROFESSOR", null))
                .thenReturn("valid-prof-ticket-456");

        ResponseEntity<?> response = controller.createStreamTicket(profAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("valid-prof-ticket-456", body.get("ticket"));
        verify(notificationSseService).createStreamTicket("PROF101", "PROFESSOR", null);
    }

    // ─── GET /api/notifications/stream ───────────────────────────────────────

    @Test
    @DisplayName("streamNotifications: Missing or blank ticket returns 401 Unauthorized")
    void streamNotifications_MissingOrBlankTicket_Returns401() {
        ResponseEntity<SseEmitter> res1 = controller.streamNotifications(null);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode());

        ResponseEntity<SseEmitter> res2 = controller.streamNotifications("   ");
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode());

        verify(notificationSseService, never()).consumeStreamTicket(any());
    }

    @Test
    @DisplayName("streamNotifications: JWT passed in URL (containing dots) is strictly rejected with 401")
    void streamNotifications_JwtInUrl_StrictlyRejectedWith401() {
        String jwtToken = "header.payload.signature";

        ResponseEntity<SseEmitter> response = controller.streamNotifications(jwtToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(notificationSseService, never()).consumeStreamTicket(any());
        verify(notificationSseService, never()).createEmitter(any());
    }

    @Test
    @DisplayName("streamNotifications: Expired or already consumed ticket returns 401 Unauthorized")
    void streamNotifications_ExpiredTicket_Returns401() {
        when(notificationSseService.consumeStreamTicket("expired-ticket")).thenReturn(null);

        ResponseEntity<SseEmitter> response = controller.streamNotifications("expired-ticket");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(notificationSseService).consumeStreamTicket("expired-ticket");
        verify(notificationSseService, never()).createEmitter(any());
    }

    @Test
    @DisplayName("streamNotifications: Valid ticket is consumed once and returns SseEmitter")
    void streamNotifications_ValidTicket_ConsumesOnceAndReturnsEmitter() {
        StreamTicketData ticketData = StreamTicketData.builder()
                .userId("PROF101")
                .role("PROFESSOR")
                .build();
        SseEmitter emitter = new SseEmitter();

        when(notificationSseService.consumeStreamTicket("valid-ticket")).thenReturn(ticketData);
        when(notificationSseService.createEmitter(ticketData)).thenReturn(emitter);

        ResponseEntity<SseEmitter> response = controller.streamNotifications("valid-ticket");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(emitter, response.getBody());
        verify(notificationSseService).consumeStreamTicket("valid-ticket");
        verify(notificationSseService).createEmitter(ticketData);
    }
}
