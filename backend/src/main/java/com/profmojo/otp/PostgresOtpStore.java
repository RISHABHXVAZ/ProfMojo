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
 * Uses the existing onboarding_otp table to persist and retrieve OTP records.
 */
@Component("postgresOtpStore")
@RequiredArgsConstructor
public class PostgresOtpStore implements OtpStore {

    private final OnboardingOtpRepository otpRepository;

    @Override
    @Transactional
    public void saveOtp(String role, String identifier, String otp, Duration ttl) {
        otpRepository.deleteById(identifier);

        OnboardingOtp entity = new OnboardingOtp();
        entity.setUserId(identifier);
        entity.setRole(role.toUpperCase());
        entity.setOtp(otp);
        entity.setExpiry(LocalDateTime.now().plus(ttl));

        otpRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredOtp> findOtp(String role, String identifier) {
        return otpRepository.findById(identifier)
                .filter(entity -> role.equalsIgnoreCase(entity.getRole()))
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
        otpRepository.deleteById(identifier);
    }
}
