package com.profmojo.security;

import com.profmojo.controllers.StaffAmenityController;
import com.profmojo.exception.DeliveryLockoutException;
import com.profmojo.exception.SecurityInfrastructureUnavailableException;
import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.AmenityRequestService;
import com.profmojo.services.impl.AmenityRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Phase 2 — Delivery Confirmation Code Security & Attempt Bounding Tests")
class DeliveryConfirmationSecurityTest extends BasePostgresContainerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AmenityRequestService amenityRequestService;
    @Autowired private AdminAmenityService adminAmenityService;
    @Autowired private AmenityRequestRepository requestRepo;
    @Autowired private StaffRepository staffRepo;
    @Autowired private NotificationRepository notifRepo;
    @Autowired private com.profmojo.services.NotificationService notificationService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private StaffAmenityController staffAmenityController;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private Staff staff;
    private AmenityRequest request;

    @BeforeEach
    void setUp() {
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }
        notifRepo.deleteAll();
        requestRepo.deleteAll();
        staffRepo.deleteAll();

        staff = staffRepo.save(Staff.builder()
                .staffId("staff-secure")
                .name("Diana Staff")
                .email("diana@test.com")
                .password("pass")
                .department("CSE")
                .available(true)
                .stars(5)
                .totalDeliveries(2)
                .build());

        request = requestRepo.save(AmenityRequest.builder()
                .professorId("prof-secure")
                .professorName("Dr. Knuth")
                .department("CSE")
                .classRoom("LH-201")
                .status(RequestStatus.PENDING)
                .items(List.of("Microphone"))
                .assignmentDeadline(LocalDateTime.now().plusMinutes(2))
                .build());
    }

    @Test
    @DisplayName("1. Code generation: Cryptographically secure 4-digit code is set on assignment, not leaked in staff notification")
    void assignment_GeneratesSecureCode_NoLeakInStaffNotification() {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");

        assertThat(assigned.getDeliveryConfirmationCode())
                .isNotNull()
                .hasSize(4)
                .matches("^[0-9]{4}$");
        assertThat(assigned.getConfirmationCodeExpiry())
                .isNotNull()
                .isAfter(LocalDateTime.now().plusHours(1));

        // Check staff notification does NOT contain code
        List<Notification> staffNotifs = notifRepo.findByRecipientId(staff.getStaffId());
        assertThat(staffNotifs).isNotEmpty();
        assertThat(staffNotifs.get(0).getMessage())
                .doesNotContain(assigned.getDeliveryConfirmationCode())
                .contains("Collect confirmation code from professor upon delivery");

        // Check professor notification DOES contain code
        List<Notification> profNotifs = notifRepo.findByRecipientId(request.getProfessorId());
        assertThat(profNotifs).isNotEmpty();
        assertThat(profNotifs.get(0).getMessage())
                .contains(assigned.getDeliveryConfirmationCode());
    }

    @Test
    @DisplayName("2. Single-use enforcement: Confirmation code is cleared upon successful delivery")
    void delivery_ClearsConfirmationCode_SingleUse() {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");
        String code = assigned.getDeliveryConfirmationCode();

        Map<String, Object> resp = amenityRequestService.confirmDelivery(assigned.getId(), code, staff);
        assertThat(resp.get("slaBreached")).isEqualTo(false);

        AmenityRequest delivered = requestRepo.findById(assigned.getId()).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(RequestStatus.DELIVERED);
        assertThat(delivered.getDeliveryConfirmationCode()).isNull();
        assertThat(delivered.getConfirmationCodeExpiry()).isNull();
        assertThat(delivered.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("3. Expiration enforcement: Expired confirmation code is rejected with HTTP 400")
    void delivery_ExpiredCode_Rejected() throws Exception {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");
        assigned.setConfirmationCodeExpiry(LocalDateTime.now().minusMinutes(5));
        requestRepo.save(assigned);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                amenityRequestService.confirmDelivery(assigned.getId(), assigned.getDeliveryConfirmationCode(), staff)
        );
        assertThat(ex.getMessage()).contains("Confirmation code has expired");

        String staffToken = jwtUtil.generateToken(staff.getStaffId(), "STAFF", staff.getDepartment());
        mockMvc.perform(put("/api/staff/amenities/{requestId}/delivered", assigned.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .param("confirmationCode", assigned.getDeliveryConfirmationCode()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Rate limiting: 5 failed attempts locks delivery verification, returning HTTP 429 with Retry-After")
    void delivery_FiveFailedAttempts_LocksOut_Returns429() throws Exception {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");
        String staffToken = jwtUtil.generateToken(staff.getStaffId(), "STAFF", staff.getDepartment());

        // 4 failed attempts via HTTP return 400
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(put("/api/staff/amenities/{requestId}/delivered", assigned.getId())
                            .header("Authorization", "Bearer " + staffToken)
                            .param("confirmationCode", "0000"))
                    .andExpect(status().isBadRequest());
        }

        // 5th failed attempt via HTTP returns 400 and establishes lockout in Redis
        mockMvc.perform(put("/api/staff/amenities/{requestId}/delivered", assigned.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .param("confirmationCode", "0000"))
                .andExpect(status().isBadRequest());

        // 6th attempt (even with the CORRECT code) is locked out and MUST return HTTP 429
        mockMvc.perform(put("/api/staff/amenities/{requestId}/delivered", assigned.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .param("confirmationCode", assigned.getDeliveryConfirmationCode()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        // Direct service invocation also throws DeliveryLockoutException
        DeliveryLockoutException ex = assertThrows(DeliveryLockoutException.class, () ->
                amenityRequestService.confirmDelivery(assigned.getId(), assigned.getDeliveryConfirmationCode(), staff)
        );
        assertThat(ex.getMessage()).contains("Maximum failed confirmation attempts exceeded");
        assertThat(ex.getRetryAfterSeconds()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("5. Invalidation on cancellation: Confirmation code is invalidated when request is cancelled via SLA breach")
    void cancellation_ClearsConfirmationCode() {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");
        assertThat(assigned.getDeliveryConfirmationCode()).isNotNull();

        AmenityRequest newReq = amenityRequestService.reRequestDueToSLABreach(assigned.getId(), "prof-secure");

        AmenityRequest oldReq = requestRepo.findById(assigned.getId()).orElseThrow();
        assertThat(oldReq.getStatus()).isEqualTo(RequestStatus.CANCELLED_SLA_BREACH);
        assertThat(oldReq.getDeliveryConfirmationCode()).isNull();
        assertThat(oldReq.getConfirmationCodeExpiry()).isNull();

        Staff reloadedStaff = staffRepo.findById(staff.getStaffId()).orElseThrow();
        assertThat(reloadedStaff.isAvailable()).isTrue();

        assertThat(newReq.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(newReq.getDeliveryConfirmationCode()).isNull();
    }

    @Test
    @DisplayName("6. Redis failure during lockout lookup: Fails closed and returns HTTP 503 Service Unavailable")
    void redisFailure_DuringLockoutLookup_Returns503_FailsClosed() {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");

        StringRedisTemplate faultyRedis = mock(StringRedisTemplate.class);
        when(faultyRedis.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("Redis connection refused"));

        AmenityRequestServiceImpl serviceWithFaultyRedis = new AmenityRequestServiceImpl(
                requestRepo, staffRepo, notificationService, adminAmenityService, faultyRedis
        );

        // Service invocation throws SecurityInfrastructureUnavailableException
        SecurityInfrastructureUnavailableException ex = assertThrows(
                SecurityInfrastructureUnavailableException.class,
                () -> transactionTemplate.execute(status ->
                        serviceWithFaultyRedis.confirmDelivery(assigned.getId(), assigned.getDeliveryConfirmationCode(), staff)
                )
        );
        assertThat(ex.getMessage()).contains("Security infrastructure unavailable");

        // Controller invocation returns HTTP 503
        StaffAmenityController controller = new StaffAmenityController(requestRepo, staffRepo, serviceWithFaultyRedis);
        ResponseEntity<?> response = transactionTemplate.execute(status ->
                controller.markAsDelivered(assigned.getId(), assigned.getDeliveryConfirmationCode(), staff)
        );
        assertThat(response.getStatusCode().value()).isEqualTo(503);

        // Verify request remains ASSIGNED in DB, delivery did NOT succeed
        AmenityRequest unchanged = requestRepo.findById(assigned.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(unchanged.getDeliveryConfirmationCode()).isEqualTo(assigned.getDeliveryConfirmationCode());
    }

    @Test
    @DisplayName("7. Redis failure while recording failed attempt: Fails closed and returns HTTP 503 without misreporting as code error")
    void redisFailure_WhileRecordingFailedAttempt_Returns503() {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");

        StringRedisTemplate faultyRedis = mock(StringRedisTemplate.class);
        // Lockout lookup returns false (not locked out)
        when(faultyRedis.hasKey(anyString())).thenReturn(false);
        // Recording attempt fails
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(faultyRedis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenThrow(new RedisConnectionFailureException("Redis write timeout"));

        AmenityRequestServiceImpl serviceWithFaultyRedis = new AmenityRequestServiceImpl(
                requestRepo, staffRepo, notificationService, adminAmenityService, faultyRedis
        );

        // Attempting with wrong code throws SecurityInfrastructureUnavailableException rather than IllegalArgumentException
        SecurityInfrastructureUnavailableException ex = assertThrows(
                SecurityInfrastructureUnavailableException.class,
                () -> transactionTemplate.execute(status ->
                        serviceWithFaultyRedis.confirmDelivery(assigned.getId(), "WRONG_CODE", staff)
                )
        );
        assertThat(ex.getMessage()).contains("Security infrastructure unavailable");

        // Controller invocation returns HTTP 503
        StaffAmenityController controller = new StaffAmenityController(requestRepo, staffRepo, serviceWithFaultyRedis);
        ResponseEntity<?> response = transactionTemplate.execute(status ->
                controller.markAsDelivered(assigned.getId(), "WRONG_CODE", staff)
        );
        assertThat(response.getStatusCode().value()).isEqualTo(503);

        // Verify request remains ASSIGNED in DB
        AmenityRequest unchanged = requestRepo.findById(assigned.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
    }

    @Test
    @DisplayName("8. No delivery confirmation succeeds when attempt protection cannot be enforced (null redisTemplate)")
    void delivery_NullRedisTemplate_FailsClosedWith503() {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");

        AmenityRequestServiceImpl serviceWithNullRedis = new AmenityRequestServiceImpl(
                requestRepo, staffRepo, notificationService, adminAmenityService, null
        );

        assertThrows(SecurityInfrastructureUnavailableException.class, () ->
                transactionTemplate.execute(status ->
                        serviceWithNullRedis.confirmDelivery(assigned.getId(), assigned.getDeliveryConfirmationCode(), staff)
                )
        );

        StaffAmenityController controller = new StaffAmenityController(requestRepo, staffRepo, serviceWithNullRedis);
        ResponseEntity<?> response = transactionTemplate.execute(status ->
                controller.markAsDelivered(assigned.getId(), assigned.getDeliveryConfirmationCode(), staff)
        );
        assertThat(response.getStatusCode().value()).isEqualTo(503);

        AmenityRequest unchanged = requestRepo.findById(assigned.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
    }

    @Test
    @DisplayName("9. HTTP Endpoint: Successful delivery with correct code returns HTTP 200 OK")
    void delivery_ValidCodeViaHttp_Returns200() throws Exception {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");
        String staffToken = jwtUtil.generateToken(staff.getStaffId(), "STAFF", staff.getDepartment());

        mockMvc.perform(put("/api/staff/amenities/{requestId}/delivered", assigned.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .param("confirmationCode", assigned.getDeliveryConfirmationCode()))
                .andExpect(status().isOk());

        AmenityRequest delivered = requestRepo.findById(assigned.getId()).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(RequestStatus.DELIVERED);
        assertThat(delivered.getDeliveryConfirmationCode()).isNull();
    }

    @Test
    @DisplayName("10. HTTP Endpoint: Unauthorized staff attempting delivery confirmation returns HTTP 403 Forbidden")
    void delivery_UnauthorizedStaffViaHttp_Returns403() throws Exception {
        AmenityRequest assigned = adminAmenityService.assignStaff(request.getId(), staff.getStaffId(), "CSE");

        Staff otherStaff = staffRepo.save(Staff.builder()
                .staffId("staff-other")
                .name("Other Staff")
                .email("other@test.com")
                .password("pass")
                .department("CSE")
                .available(true)
                .stars(5)
                .totalDeliveries(0)
                .build());

        String otherToken = jwtUtil.generateToken(otherStaff.getStaffId(), "STAFF", otherStaff.getDepartment());

        mockMvc.perform(put("/api/staff/amenities/{requestId}/delivered", assigned.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .param("confirmationCode", assigned.getDeliveryConfirmationCode()))
                .andExpect(status().isForbidden());
    }
}
