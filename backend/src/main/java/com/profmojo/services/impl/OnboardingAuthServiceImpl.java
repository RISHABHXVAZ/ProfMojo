package com.profmojo.services.impl;

import com.profmojo.models.Professor;
import com.profmojo.models.ProfessorMaster;
import com.profmojo.models.Staff;
import com.profmojo.models.Student;
import com.profmojo.models.StudentMaster;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.otp.OtpStore;
import com.profmojo.otp.StoredOtp;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.repositories.ProfessorMasterRepository;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.repositories.StudentMasterRepository;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.services.EmailService;
import com.profmojo.services.OnboardingAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OnboardingAuthServiceImpl implements OnboardingAuthService {

    private final ProfessorMasterRepository professorMasterRepository;
    private final ProfessorRepository professorRepository;
    private final OtpStore otpStore;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StudentMasterRepository studentMasterRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final OtpRateLimiter otpRateLimiter;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofSeconds(300);

    @Override
    public void sendProfessorOtp(String userId) {
        try {
            ProfessorMaster master = professorMasterRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Invalid Professor ID"));

            otpRateLimiter.resetVerifyAttempts("PROFESSOR", userId);

            String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

            otpStore.saveOtp("PROFESSOR", userId, otp, OTP_TTL);

            emailService.send(
                    master.getEmail(),
                    "ProfMojo OTP",
                    "Your OTP is: " + otp + " (valid for 5 minutes)"
            );
            appMetricsService.incrementOtpRequest("onboarding", "success");
        } catch (Exception e) {
            appMetricsService.incrementOtpRequest("onboarding", "failed");
            throw e;
        }
    }

    @Override
    public void verifyProfessorOtpAndSetPassword(VerifyOtpSetPasswordRequest req) {
        String userId = req.getUserId();

        otpRateLimiter.checkVerifyAttemptLimit("PROFESSOR", userId);

        StoredOtp otp = otpStore.findOtp("PROFESSOR", userId)
                .orElseThrow(() -> {
                    appMetricsService.incrementOtpVerification("onboarding", "invalid");
                    return new RuntimeException("OTP not found");
                });

        if (!"PROFESSOR".equalsIgnoreCase(otp.role())) {
            appMetricsService.incrementOtpVerification("onboarding", "invalid");
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.isExpired()) {
            appMetricsService.incrementOtpVerification("onboarding", "expired");
            throw new RuntimeException("OTP expired");
        }

        if (!otp.otp().equals(req.getOtp())) {
            otpRateLimiter.recordFailedVerifyAttempt("PROFESSOR", userId);
            appMetricsService.incrementOtpVerification("onboarding", "invalid");
            throw new RuntimeException("Invalid OTP");
        }

        ProfessorMaster master = professorMasterRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid Professor ID"));

        Professor professor = Professor.builder()
                .profId(master.getProfId())
                .name(master.getName())
                .department(master.getDepartment())
                .email(master.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("PROFESSOR")
                .build();

        professorRepository.save(professor);

        otpStore.deleteOtp("PROFESSOR", userId);
        otpRateLimiter.resetVerifyAttempts("PROFESSOR", userId);
        appMetricsService.incrementOtpVerification("onboarding", "success");
    }

    @Override
    public void sendStudentOtp(String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                throw new RuntimeException("Registration number is required");
            }

            StudentMaster student = studentMasterRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Invalid Registration Number"));

            otpRateLimiter.resetVerifyAttempts("STUDENT", userId);

            String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

            otpStore.saveOtp("STUDENT", userId, otp, OTP_TTL);

            emailService.send(
                    student.getEmail(),
                    "ProfMojo OTP",
                    "Your OTP is: " + otp + " (valid for 5 minutes)"
            );
            appMetricsService.incrementOtpRequest("onboarding", "success");
        } catch (Exception e) {
            appMetricsService.incrementOtpRequest("onboarding", "failed");
            throw e;
        }
    }

    @Override
    public void verifyStudentOtpAndSetPassword(VerifyOtpSetPasswordRequest req) {
        String userId = req.getUserId();

        otpRateLimiter.checkVerifyAttemptLimit("STUDENT", userId);

        StoredOtp otp = otpStore.findOtp("STUDENT", userId)
                .orElseThrow(() -> {
                    appMetricsService.incrementOtpVerification("onboarding", "invalid");
                    return new RuntimeException("Invalid OTP");
                });

        if (!"STUDENT".equalsIgnoreCase(otp.role())) {
            appMetricsService.incrementOtpVerification("onboarding", "invalid");
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.isExpired()) {
            appMetricsService.incrementOtpVerification("onboarding", "expired");
            throw new RuntimeException("OTP expired");
        }

        if (!otp.otp().equals(req.getOtp())) {
            otpRateLimiter.recordFailedVerifyAttempt("STUDENT", userId);
            appMetricsService.incrementOtpVerification("onboarding", "invalid");
            throw new RuntimeException("Invalid OTP");
        }

        StudentMaster master = studentMasterRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid Registration Number"));

        Student student = Student.builder()
                .regNo(master.getRegNo())
                .name(master.getName())
                .email(master.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("STUDENT")
                .build();

        studentRepository.save(student);

        otpStore.deleteOtp("STUDENT", userId);
        otpRateLimiter.resetVerifyAttempts("STUDENT", userId);
        appMetricsService.incrementOtpVerification("onboarding", "success");
    }

    @Override
    public void sendStaffOtp(String staffId) {
        try {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new RuntimeException("Invalid Staff ID"));

            otpRateLimiter.resetVerifyAttempts("STAFF", staffId);

            String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

            otpStore.saveOtp("STAFF", staffId, otp, OTP_TTL);

            emailService.send(
                    staff.getEmail(),
                    "ProfMojo Staff OTP",
                    "Your OTP is: " + otp + " (valid for 5 minutes)"
            );
            appMetricsService.incrementOtpRequest("onboarding", "success");
        } catch (Exception e) {
            appMetricsService.incrementOtpRequest("onboarding", "failed");
            throw e;
        }
    }

    @Override
    public void verifyStaffOtpAndSetPassword(VerifyOtpSetPasswordRequest req) {
        String staffId = req.getUserId();

        otpRateLimiter.checkVerifyAttemptLimit("STAFF", staffId);

        StoredOtp otp = otpStore.findOtp("STAFF", staffId)
                .orElseThrow(() -> {
                    appMetricsService.incrementOtpVerification("onboarding", "invalid");
                    return new RuntimeException("OTP not found");
                });

        if (!"STAFF".equalsIgnoreCase(otp.role())) {
            appMetricsService.incrementOtpVerification("onboarding", "invalid");
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.isExpired()) {
            appMetricsService.incrementOtpVerification("onboarding", "expired");
            throw new RuntimeException("OTP expired");
        }

        if (!otp.otp().equals(req.getOtp())) {
            otpRateLimiter.recordFailedVerifyAttempt("STAFF", staffId);
            appMetricsService.incrementOtpVerification("onboarding", "invalid");
            throw new RuntimeException("Invalid OTP");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        staff.setPassword(passwordEncoder.encode(req.getPassword()));
        staffRepository.save(staff);

        otpStore.deleteOtp("STAFF", staffId);
        otpRateLimiter.resetVerifyAttempts("STAFF", staffId);
        appMetricsService.incrementOtpVerification("onboarding", "success");
    }
}
