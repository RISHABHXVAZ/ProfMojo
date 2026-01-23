package com.profmojo.controllers;

import com.profmojo.models.dto.StaffLoginRequest;
import com.profmojo.models.dto.StaffLoginResponse;
import com.profmojo.models.dto.StaffSetPasswordRequest;
import com.profmojo.services.StaffAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/staff/auth")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class StaffAuthController {

    private final StaffAuthService staffAuthService;
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(
            @RequestBody StaffSetPasswordRequest request
    ) {
        staffAuthService.setPassword(request);
        return ResponseEntity.ok(
                Map.of("message", "Password set successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<StaffLoginResponse> login(
            @RequestBody StaffLoginRequest request
    ) {
        return ResponseEntity.ok(staffAuthService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        staffAuthService.logout(authHeader);
        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully")
        );
    }
}
