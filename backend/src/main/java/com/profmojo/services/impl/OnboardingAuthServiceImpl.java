package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.repositories.*;
import com.profmojo.services.EmailService;
import com.profmojo.services.OnboardingAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OnboardingAuthServiceImpl implements OnboardingAuthService {

    private final ProfessorMasterRepository professorMasterRepository;
    private final ProfessorRepository professorRepository;
    private final OnboardingOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StudentMasterRepository studentMasterRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    @Override
    public void sendProfessorOtp(String userId) {

        // userId == profId
        ProfessorMaster master = professorMasterRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid Professor ID"));


        // Prevent duplicate OTPs
        otpRepository.deleteById(userId);

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        OnboardingOtp entity = new OnboardingOtp();
        entity.setUserId(userId);
        entity.setRole("PROFESSOR");
        entity.setOtp(otp);
        entity.setExpiry(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(entity);

        emailService.send(
                master.getEmail(),
                "ProfMojo OTP",
                "Your OTP is: " + otp + " (valid for 5 minutes)"
        );
    }


    @Override
    public void verifyProfessorOtpAndSetPassword(VerifyOtpSetPasswordRequest req) {

        String userId = req.getUserId(); // clarity

        OnboardingOtp otp = otpRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (!"PROFESSOR".equals(otp.getRole())) {
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.getOtp().equals(req.getOtp())) {
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
        otpRepository.deleteById(userId);
    }

    @Override
    public void sendStudentOtp(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("Registration number is required");
        }

        StudentMaster student = studentMasterRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid Registration Number"));

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        OnboardingOtp entity = new OnboardingOtp();
        entity.setUserId(userId);
        entity.setRole("STUDENT");
        entity.setOtp(otp);
        entity.setExpiry(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(entity);

        emailService.send(
                student.getEmail(),
                "ProfMojo OTP",
                "Your OTP is: " + otp + " (valid for 5 minutes)"
        );
    }





    @Override
    public void verifyStudentOtpAndSetPassword(VerifyOtpSetPasswordRequest req) {

        OnboardingOtp otpEntity = otpRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (!"STUDENT".equals(otpEntity.getRole())) {
            throw new RuntimeException("Invalid OTP role");
        }

        if (otpEntity.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otpEntity.getOtp().equals(req.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }


        StudentMaster master = studentMasterRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid Registration Number"));


        Student student = Student.builder()
                .regNo(master.getRegNo())
                .name(master.getName())
                .email(master.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("STUDENT")
                .build();

        studentRepository.save(student);
        otpRepository.deleteById(req.getUserId());
    }

    @Override
    public void sendStaffOtp(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Invalid Staff ID"));

        otpRepository.deleteById(staffId);

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        OnboardingOtp entity = new OnboardingOtp();
        entity.setUserId(staffId);
        entity.setRole("STAFF");
        entity.setOtp(otp);
        entity.setExpiry(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(entity);

        emailService.send(
                staff.getEmail(),
                "ProfMojo Staff OTP",
                "Your OTP is: " + otp + " (valid for 5 minutes)"
        );
    }


    @Override
    public void verifyStaffOtpAndSetPassword(VerifyOtpSetPasswordRequest req) {

        String staffId = req.getUserId();

        OnboardingOtp otp = otpRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (!"STAFF".equals(otp.getRole())) {
            throw new RuntimeException("Invalid OTP role");
        }

        if (otp.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.getOtp().equals(req.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));


        staff.setPassword(passwordEncoder.encode(req.getPassword()));
        staffRepository.save(staff);

        otpRepository.deleteById(staffId);
    }



}
