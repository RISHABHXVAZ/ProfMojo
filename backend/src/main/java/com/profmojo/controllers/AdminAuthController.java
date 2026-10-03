package com.profmojo.controllers;

import com.profmojo.models.dto.*;
import com.profmojo.ratelimit.ClientIpResolver;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.services.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final OtpRateLimiter otpRateLimiter;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody AdminSendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientIpResolver.resolve(httpRequest);
        try {
            otpRateLimiter.checkSendOtpRateLimit(clientIp, request.getSecretKey());
        } catch (com.profmojo.ratelimit.RateLimitExceededException e) {
            appMetricsService.incrementOtpRequest("admin", "rate_limited");
            throw e;
        }

        adminAuthService.sendOtp(request.getSecretKey());
        return ResponseEntity.ok(
                Map.of("message", "OTP sent to admin email")
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AdminLoginResponse> verifyOtp(
            @RequestBody AdminVerifyOtpRequest request
    ) {
        return ResponseEntity.ok(
                adminAuthService.verifyOtpAndLogin(request)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        adminAuthService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

