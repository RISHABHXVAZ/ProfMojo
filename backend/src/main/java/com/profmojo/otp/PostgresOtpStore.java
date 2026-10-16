package com.profmojo.otp;

import com.profmojo.models.OnboardingOtp;
import com.profmojo.repositories.OnboardingOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * PostgreSQL fallback implementation of {@link OtpStore}.
 * Uses the onboarding_otp table to persist and retrieve OTP records
 * with unambiguous composite identity (role, identifier).
 */
@Component("postgresOtpStore")
@RequiredArgsConstructor
public class PostgresOtpStore implements OtpStore {

    private final OnboardingOtpRepository otpRepository;

    private String normalizeRole(String role) {
        return role != null ? role.trim().toUpperCase() : "UNKNOWN";
    }

    private String normalizeId(String identifier) {
        return identifier != null ? identifier.trim() : "unknown";
    }

    @Override
    @Transactional
    public void saveOtp(String role, String identifier, String otp, Duration ttl) {
        String normRole = normalizeRole(role);
        String normId = normalizeId(identifier);

        otpRepository.deleteByRoleIgnoreCaseAndUserId(normRole, normId);

        OnboardingOtp entity = OnboardingOtp.builder()
                .role(normRole)
                .userId(normId)
                .otp(otp)
                .expiry(LocalDateTime.now().plus(ttl))
                .build();

        otpRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredOtp> findOtp(String role, String identifier) {
        String normRole = normalizeRole(role);
        String normId = normalizeId(identifier);

        return otpRepository.findByRoleIgnoreCaseAndUserId(normRole, normId)
                .map(entity -> new StoredOtp(
                        entity.getOtp(),
                        entity.getRole(),
                        entity.getUserId(),
                        OtpSource.POSTGRES,
                        entity.getExpiry()
                ));
    }

    @Override
    @Transactional
    public void deleteOtp(String role, String identifier) {
        String normRole = normalizeRole(role);
        String normId = normalizeId(identifier);
        otpRepository.deleteByRoleIgnoreCaseAndUserId(normRole, normId);
    }
}
