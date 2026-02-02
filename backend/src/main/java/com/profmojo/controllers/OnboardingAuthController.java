package com.profmojo.controllers;

import com.profmojo.models.dto.SendOtpRequest;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.services.EmailService;
import com.profmojo.services.OnboardingAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class OnboardingAuthController {

    private final OnboardingAuthService onboardingAuthService;
    private final EmailService emailService;

    @PostMapping("/professor/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest request) {
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
    public ResponseEntity<?> sendStudentOtp(@RequestBody SendOtpRequest request) {
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
            @RequestBody SendOtpRequest request
    ) {
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
