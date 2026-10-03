package com.profmojo.controllers;

import com.profmojo.models.dto.SendOtpRequest;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.ratelimit.ClientIpResolver;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.services.EmailService;
import com.profmojo.services.OnboardingAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingAuthController {

    private final OnboardingAuthService onboardingAuthService;
    private final EmailService emailService;
    private final OtpRateLimiter otpRateLimiter;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @PostMapping("/professor/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody SendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientIpResolver.resolve(httpRequest);
        try {
            otpRateLimiter.checkSendOtpRateLimit(clientIp, request.getUserId());
        } catch (com.profmojo.ratelimit.RateLimitExceededException e) {
            appMetricsService.incrementOtpRequest("onboarding", "rate_limited");
            throw e;
        }

        onboardingAuthService.sendProfessorOtp(request.getUserId());
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    @PostMapping("/professor/set-password")
    public ResponseEntity<?> setPassword(
            @RequestBody VerifyOtpSetPasswordRequest request
    ) {
        onboardingAuthService.verifyProfessorOtpAndSetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password set successfully"));
    }

    @PostMapping("/student/send-otp")
    public ResponseEntity<?> sendStudentOtp(
            @RequestBody SendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientIpResolver.resolve(httpRequest);
        try {
            otpRateLimiter.checkSendOtpRateLimit(clientIp, request.getUserId());
        } catch (com.profmojo.ratelimit.RateLimitExceededException e) {
            appMetricsService.incrementOtpRequest("onboarding", "rate_limited");
            throw e;
        }

        onboardingAuthService.sendStudentOtp(request.getUserId());
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    @PostMapping("/student/set-password")
    public ResponseEntity<?> setStudentPassword(
            @RequestBody VerifyOtpSetPasswordRequest request
    ) {
        onboardingAuthService.verifyStudentOtpAndSetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password set successfully"));
    }

    @PostMapping("/staff/send-otp")
    public ResponseEntity<?> sendStaffOtp(
            @RequestBody SendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientIpResolver.resolve(httpRequest);
        otpRateLimiter.checkSendOtpRateLimit(clientIp, request.getUserId());

        onboardingAuthService.sendStaffOtp(request.getUserId());
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }


    @PostMapping("/staff/set-password")
    public ResponseEntity<?> setStaffPassword(
            @RequestBody VerifyOtpSetPasswordRequest request
    ) {
        onboardingAuthService.verifyStaffOtpAndSetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password set successfully"));
    }


}
