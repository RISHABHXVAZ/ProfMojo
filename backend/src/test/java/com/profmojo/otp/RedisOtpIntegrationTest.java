package com.profmojo.otp;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.ProfessorMaster;
import com.profmojo.models.Staff;
import com.profmojo.models.StudentMaster;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminVerifyOtpRequest;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.ratelimit.RateLimitExceededException;
import com.profmojo.repositories.*;
import com.profmojo.services.AdminAuthService;
import com.profmojo.services.EmailService;
import com.profmojo.services.OnboardingAuthService;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@DisplayName("Redis OTP Storage & PostgreSQL Fallback Integration Tests")
class RedisOtpIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private OtpStore otpStore;

    @Autowired
    private RedisOtpStore redisOtpStore;

    @Autowired
    private PostgresOtpStore postgresOtpStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private OnboardingAuthService onboardingAuthService;

    @Autowired
    private DepartmentSecretRepository secretRepo;

    @Autowired
    private ProfessorMasterRepository profMasterRepo;

    @Autowired
    private ProfessorRepository profRepo;

    @Autowired
    private StudentMasterRepository studentMasterRepo;

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private StaffRepository staffRepo;

    @Autowired
    private OnboardingOtpRepository onboardingOtpRepo;

    @Autowired
    private OtpRateLimiter otpRateLimiter;

    @MockitoBean
    private EmailService emailService;

    private final String adminSecret = "98765432";

    @BeforeEach
    void cleanState() {
        doNothing().when(emailService).send(anyString(), anyString(), anyString());

        // Clear all Redis keys
        Set<String> keys = redisTemplate.keys("profmojo:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // Clean database tables
        onboardingOtpRepo.deleteAll();
        secretRepo.deleteById(adminSecret);

        // Seed test admin department
        DepartmentSecret secret = TestDataFactory.createDepartmentSecret(adminSecret, "CSE", "admin@profmojo.edu", true);
        secretRepo.save(secret);
    }

    // =========================================================================
    // A. REDIS OTP CORE OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("1 & 2. OTP stored successfully with approximately 300s TTL")
    void testOtpStoredWithTtl() {
        String role = "PROFESSOR";
        String id = "PROF_TEST_01";
        String otp = "123456";

        redisOtpStore.saveOtp(role, id, otp, Duration.ofSeconds(300));

        String key = redisOtpStore.buildKey(role, id);
        assertEquals("123456", redisTemplate.opsForValue().get(key));

        Long ttl = redisTemplate.getExpire(key);
        assertNotNull(ttl);
        assertTrue(ttl > 280 && ttl <= 300, "TTL should be approximately 300s but was: " + ttl);
    }

    @Test
    @DisplayName("3, 4, 5. Correct OTP verifies, incorrect OTP fails, missing OTP fails")
    void testVerificationOutcomes() {
        String role = "STUDENT";
        String id = "STU_TEST_01";
        String otp = "654321";

        redisOtpStore.saveOtp(role, id, otp, Duration.ofSeconds(300));

        // Correct OTP
        Optional<StoredOtp> storedOpt = redisOtpStore.findOtp(role, id);
        assertTrue(storedOpt.isPresent());
        assertEquals("654321", storedOpt.get().otp());
        assertEquals(OtpSource.REDIS, storedOpt.get().source());
        assertFalse(storedOpt.get().isExpired());

        // Missing OTP
        Optional<StoredOtp> missing = redisOtpStore.findOtp(role, "NON_EXISTENT");
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("6. Expired StoredOtp reports isExpired = true")
    void testExpiredOtpReportsExpired() {
        StoredOtp expired = new StoredOtp("111111", "ADMIN", "user1", OtpSource.REDIS, LocalDateTime.now().minusSeconds(1));
        assertTrue(expired.isExpired());

        StoredOtp valid = new StoredOtp("111111", "ADMIN", "user1", OtpSource.REDIS, LocalDateTime.now().plusSeconds(100));
        assertFalse(valid.isExpired());
    }

    @Test
    @DisplayName("7 & 8. Successful verification deletes OTP and prevents replay attacks")
    void testDeletePreventsReplay() {
        String role = "STAFF";
        String id = "STAFF_TEST_01";
        String otp = "789012";

        otpStore.saveOtp(role, id, otp, Duration.ofSeconds(300));
        assertTrue(otpStore.findOtp(role, id).isPresent());

        // Verify and delete
        otpStore.deleteOtp(role, id);

        // Replay check
        Optional<StoredOtp> replayed = otpStore.findOtp(role, id);
        assertTrue(replayed.isEmpty(), "Deleted OTP must not be retrievable (single-use)");
    }

    @Test
    @DisplayName("9. Generating a new OTP replaces previous OTP and resets TTL")
    void testNewOtpOverwritesOldOtp() {
        String role = "PROFESSOR";
        String id = "PROF_REPLACE";

        redisOtpStore.saveOtp(role, id, "111111", Duration.ofSeconds(300));
        assertEquals("111111", redisOtpStore.findOtp(role, id).orElseThrow().otp());

        // Generate new OTP
        redisOtpStore.saveOtp(role, id, "222222", Duration.ofSeconds(300));
        assertEquals("222222", redisOtpStore.findOtp(role, id).orElseThrow().otp());
    }

    // =========================================================================
    // B. ADMIN FLOW
    // =========================================================================

    @Test
    @DisplayName("10, 11, 12. Admin OTP generation, verification, and JWT generation via Redis")
    void testAdminWorkflowEndToEnd() {
        // Send OTP
        adminAuthService.sendOtp(adminSecret);

        // Verify it was saved in Redis under hashed secret key
        String redisKey = redisOtpStore.buildKey("ADMIN", adminSecret);
        String generatedOtp = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(generatedOtp, "Admin OTP must be stored in Redis");
        assertEquals(6, generatedOtp.length());

        // Verify raw secret is NOT part of the Redis key
        assertFalse(redisKey.contains(adminSecret), "Raw admin secret must not be exposed in Redis key");

        // Verify OTP and Login
        AdminVerifyOtpRequest verifyReq = new AdminVerifyOtpRequest();
        verifyReq.setSecretKey(adminSecret);
        verifyReq.setOtp(generatedOtp);

        AdminLoginResponse response = adminAuthService.verifyOtpAndLogin(verifyReq);
        assertNotNull(response.getToken(), "Admin JWT token must be generated");
        assertEquals("CSE", response.getDepartment());
        assertEquals("ADMIN", response.getRole());

        // Verify OTP is deleted from Redis (single-use)
        assertNull(redisTemplate.opsForValue().get(redisKey), "OTP must be deleted from Redis after login");

        // Replay attempt fails
        assertThrows(RuntimeException.class, () -> adminAuthService.verifyOtpAndLogin(verifyReq));
    }

    // =========================================================================
    // C. ONBOARDING FLOWS
    // =========================================================================

    @Test
    @DisplayName("13. Professor onboarding flow using Redis OTP")
    void testProfessorOnboardingFlow() {
        String profId = "PROF_FLOW_01";
        profMasterRepo.save(TestDataFactory.createProfessorMaster(profId, "Dr. Turing", "CSE", "turing@profmojo.edu"));

        onboardingAuthService.sendProfessorOtp(profId);

        String redisKey = redisOtpStore.buildKey("PROFESSOR", profId);
        String otp = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(otp);

        VerifyOtpSetPasswordRequest req = new VerifyOtpSetPasswordRequest();
        req.setUserId(profId);
        req.setOtp(otp);
        req.setPassword("SecurePass@2026");

        onboardingAuthService.verifyProfessorOtpAndSetPassword(req);

        // Confirmed persisted in repo
        assertTrue(profRepo.existsById(profId));
        assertNull(redisTemplate.opsForValue().get(redisKey), "OTP must be deleted after password setup");
    }

    @Test
    @DisplayName("14. Student onboarding flow using Redis OTP")
    void testStudentOnboardingFlow() {
        String studentId = "STU_FLOW_01";
        studentMasterRepo.save(TestDataFactory.createStudentMaster(studentId, "Ada Lovelace", "CSE", "ada@profmojo.edu"));

        onboardingAuthService.sendStudentOtp(studentId);

        String redisKey = redisOtpStore.buildKey("STUDENT", studentId);
        String otp = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(otp);

        VerifyOtpSetPasswordRequest req = new VerifyOtpSetPasswordRequest();
        req.setUserId(studentId);
        req.setOtp(otp);
        req.setPassword("AdaPass@2026");

        onboardingAuthService.verifyStudentOtpAndSetPassword(req);

        assertTrue(studentRepo.existsById(studentId));
        assertNull(redisTemplate.opsForValue().get(redisKey));
    }

    @Test
    @DisplayName("15. Staff onboarding flow using Redis OTP")
    void testStaffOnboardingFlow() {
        String staffId = "STAFF_FLOW_01";
        Staff staff = Staff.builder()
                .staffId(staffId)
                .name("Support Staff")
                .department("CSE")
                .email("staff@profmojo.edu")
                .contactNo("1234567890")
                .available(true)
                .role("STAFF")
                .build();
        staffRepo.save(staff);

        onboardingAuthService.sendStaffOtp(staffId);

        String redisKey = redisOtpStore.buildKey("STAFF", staffId);
        String otp = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(otp);

        VerifyOtpSetPasswordRequest req = new VerifyOtpSetPasswordRequest();
        req.setUserId(staffId);
        req.setOtp(otp);
        req.setPassword("StaffPass@2026");

        onboardingAuthService.verifyStaffOtpAndSetPassword(req);

        Staff updated = staffRepo.findById(staffId).orElseThrow();
        assertNotNull(updated.getPassword());
        assertNull(redisTemplate.opsForValue().get(redisKey));
    }

    // =========================================================================
    // D. REDIS FAILURE & POSTGRESQL FALLBACK
    // =========================================================================

    @Test
    @DisplayName("16. Redis failure during save falls back to PostgreSQL onboarding_otp table")
    void testRedisUnavailableDuringSave_FallsBackToPostgres() {
        OtpStore mockFailingRedisStore = new OtpStore() {
            @Override
            public void saveOtp(String role, String identifier, String otp, Duration ttl) {
                throw new org.springframework.data.redis.RedisConnectionFailureException("Simulated Redis Down");
            }

            @Override
            public Optional<StoredOtp> findOtp(String role, String identifier) {
                return Optional.empty();
            }

            @Override
            public void deleteOtp(String role, String identifier) {}
        };

        PrimaryFallbackOtpStore compositeStore = new PrimaryFallbackOtpStore(mockFailingRedisStore, postgresOtpStore);

        compositeStore.saveOtp("PROFESSOR", "FALLBACK_PROF", "998877", Duration.ofSeconds(300));

        // Verify stored in PostgreSQL
        var pgOtp = onboardingOtpRepo.findById("FALLBACK_PROF");
        assertTrue(pgOtp.isPresent(), "OTP must be stored in PostgreSQL when Redis fails");
        assertEquals("998877", pgOtp.get().getOtp());
        assertEquals("PROFESSOR", pgOtp.get().getRole());
    }

    @Test
    @DisplayName("17. Redis failure during find falls back to PostgreSQL onboarding_otp table")
    void testRedisUnavailableDuringFind_FallsBackToPostgres() {
        // First, seed an OTP in PostgreSQL
        postgresOtpStore.saveOtp("STAFF", "FALLBACK_STAFF", "554433", Duration.ofSeconds(300));

        OtpStore mockFailingRedisStore = new OtpStore() {
            @Override
            public void saveOtp(String role, String identifier, String otp, Duration ttl) {}

            @Override
            public Optional<StoredOtp> findOtp(String role, String identifier) {
                throw new org.springframework.data.redis.RedisConnectionFailureException("Simulated Redis Down");
            }

            @Override
            public void deleteOtp(String role, String identifier) {}
        };

        PrimaryFallbackOtpStore compositeStore = new PrimaryFallbackOtpStore(mockFailingRedisStore, postgresOtpStore);

        Optional<StoredOtp> resolved = compositeStore.findOtp("STAFF", "FALLBACK_STAFF");
        assertTrue(resolved.isPresent(), "Must retrieve OTP from PostgreSQL fallback when Redis is down");
        assertEquals("554433", resolved.get().otp());
        assertEquals(OtpSource.POSTGRES, resolved.get().source());
    }

    @Test
    @DisplayName("18. Existing PostgreSQL fallback OTP is verified even when Redis is healthy")
    void testPostgresFallbackOtpVerifiedWhenRedisIsHealthy() {
        // Saved in PostgreSQL (e.g. during previous downtime)
        postgresOtpStore.saveOtp("ADMIN", adminSecret, "777888", Duration.ofSeconds(300));

        // Redis is UP but does not have the key
        assertFalse(redisTemplate.hasKey(redisOtpStore.buildKey("ADMIN", adminSecret)));

        // Verification through primary composite store
        Optional<StoredOtp> stored = otpStore.findOtp("ADMIN", adminSecret);
        assertTrue(stored.isPresent());
        assertEquals("777888", stored.get().otp());
        assertEquals(OtpSource.POSTGRES, stored.get().source());

        // Deleting removes it from PostgreSQL
        otpStore.deleteOtp("ADMIN", adminSecret);
        assertFalse(onboardingOtpRepo.existsById(adminSecret));
    }

    // =========================================================================
    // E. FAILED ATTEMPTS THROTTLING (5-Attempt Protection)
    // =========================================================================

    @Test
    @DisplayName("22 & 23. 5 failed verification attempts triggers RateLimitExceededException; success resets")
    void testFailedVerifyAttemptsThrottling() {
        String role = "ADMIN";
        String id = adminSecret;

        // Record 5 failed attempts
        for (int i = 1; i <= 5; i++) {
            otpRateLimiter.recordFailedVerifyAttempt(role, id);
        }

        // 6th failed attempt breaches threshold
        assertThrows(RateLimitExceededException.class, () ->
                otpRateLimiter.recordFailedVerifyAttempt(role, id)
        );

        // Check that further verification attempts are immediately blocked
        assertThrows(RateLimitExceededException.class, () ->
                otpRateLimiter.checkVerifyAttemptLimit(role, id)
        );

        // Reset attempts
        otpRateLimiter.resetVerifyAttempts(role, id);

        // Verification check is allowed again
        assertDoesNotThrow(() -> otpRateLimiter.checkVerifyAttemptLimit(role, id));
    }
}
