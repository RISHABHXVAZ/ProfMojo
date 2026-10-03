package com.profmojo.services.impl;

import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminVerifyOtpRequest;
import com.profmojo.otp.OtpStore;
import com.profmojo.otp.StoredOtp;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.AdminAuthService;
import com.profmojo.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    private final DepartmentSecretRepository secretRepository;
    private final OtpStore otpStore;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final OtpRateLimiter otpRateLimiter;
    private final com.profmojo.security.jwt.TokenBlacklistService tokenBlacklistService;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofSeconds(300);

    @Override
    public void sendOtp(String secretKey) {
        try {
            DepartmentSecret secret = secretRepository.findById(secretKey)
                    .orElseThrow(() -> new RuntimeException("Invalid secret key"));

            if (!secret.isActive()) {
                throw new RuntimeException("Secret key disabled");
            }

            // Reset any previous failed verify attempts when generating a new OTP
            otpRateLimiter.resetVerifyAttempts("ADMIN", secretKey);

            String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

            // Save into OTP store (primary Redis, fallback PostgreSQL) with atomic 300s TTL
            otpStore.saveOtp("ADMIN", secretKey, otp, OTP_TTL);

            emailService.send(
                    secret.getAdminEmail(),
                    "ProfMojo Admin OTP",
                    "Your admin login OTP is: " + otp
            );
            appMetricsService.incrementOtpRequest("admin", "success");
        } catch (Exception e) {
            appMetricsService.incrementOtpRequest("admin", "failed");
            throw e;
        }
    }

    @Override
    public AdminLoginResponse verifyOtpAndLogin(AdminVerifyOtpRequest request) {
        String secretKey = request.getSecretKey();

        // Check if user has exceeded the 5-attempt limit
        otpRateLimiter.checkVerifyAttemptLimit("ADMIN", secretKey);

        StoredOtp otp = otpStore.findOtp("ADMIN", secretKey)
                .orElseThrow(() -> {
                    appMetricsService.incrementOtpVerification("admin", "invalid");
                    return new RuntimeException("OTP not found");
                });

        if (!"ADMIN".equalsIgnoreCase(otp.role())) {
            appMetricsService.incrementOtpVerification("admin", "invalid");
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.isExpired()) {
            appMetricsService.incrementOtpVerification("admin", "expired");
            throw new RuntimeException("OTP expired");
        }

        if (!otp.otp().equals(request.getOtp())) {
            otpRateLimiter.recordFailedVerifyAttempt("ADMIN", secretKey);
            appMetricsService.incrementOtpVerification("admin", "invalid");
            throw new RuntimeException("Invalid OTP");
        }

        DepartmentSecret secret = secretRepository.findById(secretKey)
                .orElseThrow(() -> new RuntimeException("Invalid secret key"));

        log.debug("Creating token for department: {}", secret.getDepartment());

        String token = jwtUtil.generateToken(
                secretKey,
                "ADMIN",
                secret.getDepartment()
        );

        // Single-use: immediately delete OTP to prevent replay attacks
        otpStore.deleteOtp("ADMIN", secretKey);
        otpRateLimiter.resetVerifyAttempts("ADMIN", secretKey);

        appMetricsService.incrementOtpVerification("admin", "success");

        return new AdminLoginResponse(
                token,
                secret.getDepartment(),
                "ADMIN"
        );
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader != null && !authHeader.isBlank()) {
            tokenBlacklistService.revokeToken(authHeader);
        }
    }
}
