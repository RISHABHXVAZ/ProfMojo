package com.profmojo.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Composite OTP store providing primary storage in Redis with automatic,
 * controlled fallback to PostgreSQL when Redis is unavailable.
 */
@Service
@Primary
@Slf4j
public class PrimaryFallbackOtpStore implements OtpStore {

    private final OtpStore redisOtpStore;
    private final OtpStore postgresOtpStore;

    public PrimaryFallbackOtpStore(
            @Qualifier("redisOtpStore") OtpStore redisOtpStore,
            @Qualifier("postgresOtpStore") OtpStore postgresOtpStore
    ) {
        this.redisOtpStore = redisOtpStore;
        this.postgresOtpStore = postgresOtpStore;
    }

    @Override
    public void saveOtp(String role, String identifier, String otp, Duration ttl) {
        try {
            redisOtpStore.saveOtp(role, identifier, otp, ttl);
            // Clean up any stale PostgreSQL fallback record for this identifier
            try {
                postgresOtpStore.deleteOtp(role, identifier);
            } catch (Exception ex) {
                // Non-fatal cleanup exception ignored
            }
        } catch (Exception ex) {
            log.warn("Redis OTP store unavailable; using PostgreSQL fallback.");
            postgresOtpStore.saveOtp(role, identifier, otp, ttl);
        }
    }

    @Override
    public Optional<StoredOtp> findOtp(String role, String identifier) {
        try {
            Optional<StoredOtp> redisOtp = redisOtpStore.findOtp(role, identifier);
            if (redisOtp.isPresent()) {
                return redisOtp;
            }
        } catch (Exception ex) {
            log.warn("Redis OTP store unavailable during verification; attempting PostgreSQL fallback.");
        }

        // Check PostgreSQL fallback (handles case where OTP was stored while Redis was down, or Redis is down now)
        return postgresOtpStore.findOtp(role, identifier);
    }

    @Override
    public void deleteOtp(String role, String identifier) {
        try {
            redisOtpStore.deleteOtp(role, identifier);
        } catch (Exception ex) {
            // Redis error ignored on delete
        }
        try {
            postgresOtpStore.deleteOtp(role, identifier);
        } catch (Exception ex) {
            // DB error ignored on delete
        }
    }
}
