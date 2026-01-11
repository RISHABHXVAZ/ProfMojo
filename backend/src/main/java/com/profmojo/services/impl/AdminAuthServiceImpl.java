package com.profmojo.services.impl;

import com.profmojo.models.Admin;
import com.profmojo.models.dto.AdminLoginRequest;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminSetPasswordRequest;
import com.profmojo.repositories.AdminRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {

        Admin admin = adminRepository.findById(request.getAdminId())
                .orElseThrow(() -> new RuntimeException("Invalid Admin ID"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                admin.getPassword()
        )) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                admin.getAdminId(),
                "ROLE_ADMIN",
                admin.getDepartment()
        );

        return new AdminLoginResponse(
                token,
                admin.getDepartment(),
                "ADMIN"
        );
    }

    @Override
    public void setPassword(AdminSetPasswordRequest request) {

        Admin admin = adminRepository.findById(request.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin ID not found"));

        if (admin.getPassword() != null) {
            throw new RuntimeException("Password already set");
        }

        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        adminRepository.save(admin);
    }
}
