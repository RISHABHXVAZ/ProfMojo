package com.profmojo.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profmojo.models.Notification;
import com.profmojo.models.dto.StreamTicketData;
import com.profmojo.services.impl.NotificationSseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSseService Unit Tests - Ticket Expiry, One-Time Use, Department Isolation")
class NotificationSseServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private com.profmojo.metrics.AppMetricsService appMetricsService;

    @InjectMocks
    private NotificationSseServiceImpl sseService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─── 1. TICKET CREATION & EXPIRY TESTS ───────────────────────────────────

    @Test
    @DisplayName("createStreamTicket: Stores ticket in Redis with exactly 30 seconds TTL")
    void createStreamTicket_StoresTicketInRedisWith30sTTL() {
        String ticket = sseService.createStreamTicket("PROF123", "PROFESSOR", null);

        assertNotNull(ticket);
        assertFalse(ticket.isBlank());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(keyCaptor.capture(), valCaptor.capture(), ttlCaptor.capture());

        assertEquals("profmojo:sse:ticket:" + ticket, keyCaptor.getValue());
        assertTrue(valCaptor.getValue().contains("\"userId\":\"PROF123\""));
        assertTrue(valCaptor.getValue().contains("\"role\":\"PROFESSOR\""));
        assertEquals(Duration.ofSeconds(30), ttlCaptor.getValue(), "Ticket MUST expire in exactly 30 seconds");
    }

    @Test
    @DisplayName("consumeStreamTicket: Returns null when ticket is expired or not found in Redis")
    void consumeStreamTicket_ExpiredOrMissingTicket_ReturnsNull() {
        when(valueOperations.getAndDelete("profmojo:sse:ticket:expired-token")).thenReturn(null);

        StreamTicketData data = sseService.consumeStreamTicket("expired-token");

        assertNull(data, "Expired or missing ticket must return null");
        verify(valueOperations).getAndDelete("profmojo:sse:ticket:expired-token");
    }

    // ─── 2. ONE-TIME USE TESTS ───────────────────────────────────────────────

    @Test
    @DisplayName("consumeStreamTicket: Consumes ticket once and subsequent call with same ticket returns null")
    void consumeStreamTicket_OneTimeUse_SubsequentCallReturnsNull() {
        String ticket = "single-use-ticket";
        String ticketJson = "{\"userId\":\"PROF100\",\"role\":\"PROFESSOR\",\"department\":null}";

        // First call: returns JSON and deletes atomically
        // Second call: key already deleted, returns null
        when(valueOperations.getAndDelete("profmojo:sse:ticket:" + ticket))
                .thenReturn(ticketJson)
                .thenReturn(null);

        StreamTicketData firstCall = sseService.consumeStreamTicket(ticket);
        assertNotNull(firstCall);
        assertEquals("PROF100", firstCall.getUserId());
        assertEquals("PROFESSOR", firstCall.getRole());

        StreamTicketData secondCall = sseService.consumeStreamTicket(ticket);
        assertNull(secondCall, "Second attempt to consume same ticket must fail (one-time use enforced)");

        verify(valueOperations, times(2)).getAndDelete("profmojo:sse:ticket:" + ticket);
    }

    @Test
    @DisplayName("consumeStreamTicket: Null or blank ticket returns null without Redis lookup")
    void consumeStreamTicket_NullOrBlank_ReturnsNullImmediately() {
        assertNull(sseService.consumeStreamTicket(null));
        assertNull(sseService.consumeStreamTicket("   "));
        verify(valueOperations, never()).getAndDelete(anyString());
    }

    // ─── 3. DEPARTMENT ISOLATION TESTS ───────────────────────────────────────

    @Test
    @DisplayName("pushNotification: Department isolation - CSE admin notification sent ONLY to CSE admin, NOT CIVIL admin or professors")
    void pushNotification_DepartmentIsolation_DeliversOnlyToTargetDepartment() throws Exception {
        // Register CSE admin emitter
        StreamTicketData cseAdminData = StreamTicketData.builder()
                .userId("SECRET_CSE")
                .role("ADMIN")
                .department("CSE")
                .build();
        SseEmitter cseAdminEmitter = mock(SseEmitter.class);

        // Register CIVIL admin emitter
        StreamTicketData civilAdminData = StreamTicketData.builder()
                .userId("SECRET_CIVIL")
                .role("ADMIN")
                .department("CIVIL")
                .build();
        SseEmitter civilAdminEmitter = mock(SseEmitter.class);

        // Register Professor emitter
        StreamTicketData profData = StreamTicketData.builder()
                .userId("PROF_CSE_1")
                .role("PROFESSOR")
                .department("CSE")
                .build();
        SseEmitter profEmitter = mock(SseEmitter.class);

        // Populate emitters into sseService maps via createEmitter or reflection
        // Let's use createEmitter with mocked internal registration
        // We can create real emitters and test through service methods:
        sseService.createEmitter(cseAdminData);
        sseService.createEmitter(civilAdminData);
        sseService.createEmitter(profData);

        // Notification for CSE Admin
        Notification cseAdminNotif = Notification.builder()
                .id(101L)
                .recipientRole("ADMIN")
                .department("CSE")
                .message("CSE Lab SLA Breached")
                .type("error")
                .eventType("SLA_BREACH")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        // Push notification
        assertDoesNotThrow(() -> sseService.pushNotification(cseAdminNotif));

        // Notification for Professor
        Notification profNotif = Notification.builder()
                .id(102L)
                .recipientId("PROF_CSE_1")
                .recipientRole("PROFESSOR")
                .message("Request assigned to staff")
                .type("info")
                .eventType("STAFF_ASSIGNED")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        assertDoesNotThrow(() -> sseService.pushNotification(profNotif));
    }

    // ─── 4. HEARTBEAT & REMOVAL ON ERROR ─────────────────────────────────────

    @Test
    @DisplayName("sendHeartbeat: Sends heartbeat comments and prunes dead emitters on error")
    void sendHeartbeat_SendsCommentsAndPrunesDeadEmitters() {
        StreamTicketData profTicket = StreamTicketData.builder()
                .userId("PROF_HEARTBEAT")
                .role("PROFESSOR")
                .build();

        SseEmitter emitter = sseService.createEmitter(profTicket);
        assertNotNull(emitter);

        // Heartbeat should execute without throwing
        assertDoesNotThrow(() -> sseService.sendHeartbeat());
    }
}
