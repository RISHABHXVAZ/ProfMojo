package com.profmojo.controllers;

import com.profmojo.models.dto.*;
import com.profmojo.services.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody AdminSendOtpRequest request
    ) {
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
}

