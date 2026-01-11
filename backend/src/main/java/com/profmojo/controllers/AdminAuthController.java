package com.profmojo.controllers;

import com.profmojo.models.dto.AdminLoginRequest;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminSetPasswordRequest;
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

    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(
            @RequestBody AdminSetPasswordRequest request
    ) {
        adminAuthService.setPassword(request);
        return ResponseEntity.ok(
                Map.of("message", "Password set successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(
            @RequestBody AdminLoginRequest request
    ) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
}
