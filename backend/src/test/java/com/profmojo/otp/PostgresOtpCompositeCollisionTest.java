package com.profmojo.otp;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.OnboardingOtp;
import com.profmojo.repositories.OnboardingOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Phase 3: PostgreSQL OTP Fallback Composite Identity & Collision Isolation Tests")
class PostgresOtpCompositeCollisionTest extends BasePostgresContainerTest {

    @Autowired
    private PostgresOtpStore postgresOtpStore;

    @Autowired
    private OnboardingOtpRepository onboardingOtpRepo;

    @BeforeEach
    void cleanUp() {
        onboardingOtpRepo.deleteAll();
    }

    @Test
    @DisplayName("Two different roles sharing the exact same identifier do NOT overwrite or collide")
    void twoRoles_SameIdentifier_DoNotCollideInPostgres() {
        String sharedId = "DUAL_USER_101";

        // Student saves OTP
        postgresOtpStore.saveOtp("STUDENT", sharedId, "111111", Duration.ofMinutes(5));

        // Staff saves OTP with the SAME identifier
        postgresOtpStore.saveOtp("STAFF", sharedId, "222222", Duration.ofMinutes(5));

        // Both records must exist in PostgreSQL
        Optional<OnboardingOtp> studentRecord = onboardingOtpRepo.findByRoleIgnoreCaseAndUserId("STUDENT", sharedId);
        Optional<OnboardingOtp> staffRecord = onboardingOtpRepo.findByRoleIgnoreCaseAndUserId("STAFF", sharedId);

        assertTrue(studentRecord.isPresent(), "Student OTP must be present");
        assertEquals("111111", studentRecord.get().getOtp());
        assertEquals("STUDENT", studentRecord.get().getRole());

        assertTrue(staffRecord.isPresent(), "Staff OTP must be present and must not overwrite student");
        assertEquals("222222", staffRecord.get().getOtp());
        assertEquals("STAFF", staffRecord.get().getRole());
    }

    @Test
    @DisplayName("Lookup by role and identifier returns the matching role's OTP only")
    void lookup_IsStrictlyRoleIsolated() {
        String sharedId = "SHARED_IDENTIFIER_002";

        postgresOtpStore.saveOtp("STUDENT", sharedId, "333333", Duration.ofMinutes(5));
        postgresOtpStore.saveOtp("PROFESSOR", sharedId, "444444", Duration.ofMinutes(5));

        // Lookup STUDENT returns student OTP
        Optional<StoredOtp> studentOtp = postgresOtpStore.findOtp("STUDENT", sharedId);
        assertTrue(studentOtp.isPresent());
        assertEquals("333333", studentOtp.get().otp());
        assertEquals("STUDENT", studentOtp.get().role());

        // Lookup PROFESSOR returns professor OTP
        Optional<StoredOtp> profOtp = postgresOtpStore.findOtp("PROFESSOR", sharedId);
        assertTrue(profOtp.isPresent());
        assertEquals("444444", profOtp.get().otp());
        assertEquals("PROFESSOR", profOtp.get().role());

        // Lookup STAFF with same id returns empty
        Optional<StoredOtp> staffOtp = postgresOtpStore.findOtp("STAFF", sharedId);
        assertFalse(staffOtp.isPresent(), "Staff lookup must not return Student or Professor OTP");
    }

    @Test
    @DisplayName("Deleting OTP for one role does not delete or affect another role's OTP")
    void deletion_IsStrictlyRoleIsolated() {
        String sharedId = "SHARED_IDENTIFIER_003";

        postgresOtpStore.saveOtp("STUDENT", sharedId, "555555", Duration.ofMinutes(5));
        postgresOtpStore.saveOtp("STAFF", sharedId, "666666", Duration.ofMinutes(5));

        // Delete STUDENT OTP
        postgresOtpStore.deleteOtp("STUDENT", sharedId);

        // Student OTP is gone
        assertFalse(postgresOtpStore.findOtp("STUDENT", sharedId).isPresent());
        assertFalse(onboardingOtpRepo.existsByRoleIgnoreCaseAndUserId("STUDENT", sharedId));

        // Staff OTP is still preserved
        Optional<StoredOtp> staffOtp = postgresOtpStore.findOtp("STAFF", sharedId);
        assertTrue(staffOtp.isPresent(), "Staff OTP must survive student deletion");
        assertEquals("666666", staffOtp.get().otp());
    }

    @Test
    @DisplayName("Expired OTP in PostgreSQL is correctly identified as expired")
    void expiredOtp_IsRecognizedAsExpired() {
        String userId = "EXPIRED_USER_004";

        // Save with negative TTL to simulate expired OTP
        postgresOtpStore.saveOtp("STUDENT", userId, "999999", Duration.ofSeconds(-10));

        Optional<StoredOtp> stored = postgresOtpStore.findOtp("STUDENT", userId);
        assertTrue(stored.isPresent());
        assertTrue(stored.get().isExpired(), "Stored OTP must report isExpired() == true");
    }
}
