package com.profmojo.services.impl;

import com.profmojo.models.Staff;
import com.profmojo.models.dto.StaffLoginRequest;
import com.profmojo.models.dto.StaffLoginResponse;
import com.profmojo.models.dto.StaffSetPasswordRequest;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.StaffAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffAuthServiceImpl implements StaffAuthService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AdminAmenityService adminAmenityService;
    private final com.profmojo.security.jwt.TokenBlacklistService tokenBlacklistService;

    @Override
    public void setPassword(StaffSetPasswordRequest request) {

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Invalid Staff ID"));

        if (staff.getPassword() != null) {
            throw new RuntimeException("Password already set");
        }

        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staffRepository.save(staff);
    }

    @Override
    public StaffLoginResponse login(StaffLoginRequest request) {

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Invalid Staff ID"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        staff.setOnline(true);
        staffRepository.save(staff);
        adminAmenityService.tryAssignQueuedRequest(staff);
        String token = jwtUtil.generateToken(
                staff.getStaffId(),
                staff.getRole()   // "STAFF"
        );

        return new StaffLoginResponse(
                token,
                staff.getStaffId(),
                staff.getDepartment()
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void logout(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return;
        }

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();

        // 1. Database state update (essential for FIFO staff availability)
        try {
            String staffId = jwtUtil.extractUsername(token);
            if (staffId != null) {
                staffRepository.findById(staffId).ifPresent(staff -> {
                    staff.setOnline(false);
                    staffRepository.save(staff);
                });
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StaffAuthServiceImpl.class)
                    .warn("Error updating staff offline state during logout: {}", e.getMessage());
        }

        // 2. Redis token revocation
        try {
            tokenBlacklistService.revokeToken(token);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StaffAuthServiceImpl.class)
                    .warn("Error revoking staff token during logout: {}", e.getMessage());
        }
    }
}
