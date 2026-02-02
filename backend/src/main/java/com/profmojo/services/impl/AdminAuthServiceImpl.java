package com.profmojo.services.impl;

import com.profmojo.models.Admin;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.OnboardingOtp;
import com.profmojo.models.dto.AdminLoginRequest;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminSetPasswordRequest;
import com.profmojo.models.dto.AdminVerifyOtpRequest;
import com.profmojo.repositories.AdminRepository;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.repositories.OnboardingOtpRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.AdminAuthService;
import com.profmojo.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final DepartmentSecretRepository secretRepository;
    private final OnboardingOtpRepository otpRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @Override
    public void sendOtp(String secretKey) {

        DepartmentSecret secret = secretRepository.findById(secretKey)
                .orElseThrow(() -> new RuntimeException("Invalid secret key"));

        if (!secret.isActive()) {
            throw new RuntimeException("Secret key disabled");
        }

        otpRepository.deleteById(secretKey);

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        OnboardingOtp entity = new OnboardingOtp();
        entity.setUserId(secretKey);
        entity.setRole("ADMIN");
        entity.setOtp(otp);
        entity.setExpiry(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(entity);

        emailService.send(
                secret.getAdminEmail(),
                "ProfMojo Admin OTP",
                "Your admin login OTP is: " + otp
        );
    }

    @Override
    public AdminLoginResponse verifyOtpAndLogin(AdminVerifyOtpRequest request) {

        OnboardingOtp otp = otpRepository.findById(request.getSecretKey())
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (!"ADMIN".equals(otp.getRole())) {
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        DepartmentSecret secret = secretRepository.findById(request.getSecretKey())
                .orElseThrow(() -> new RuntimeException("Invalid secret key"));

        // Verify department is being set in token
        System.out.println("DEBUG: Creating token for department: " + secret.getDepartment());

        String token = jwtUtil.generateToken(
                request.getSecretKey(),
                "ADMIN",
                secret.getDepartment()  // This should be set
        );

        otpRepository.deleteById(request.getSecretKey());

        return new AdminLoginResponse(
                token,
                secret.getDepartment(),
                "ADMIN"
        );
    }
}

