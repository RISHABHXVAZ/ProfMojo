package com.profmojo.controllers;

import com.profmojo.models.ProfessorMaster;
import com.profmojo.models.Staff;
import com.profmojo.models.StudentMaster;
import com.profmojo.models.dto.AddStaffRequest;
import com.profmojo.models.dto.SendOtpRequest;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.services.AdminOnboardingService;
import com.profmojo.services.OnboardingAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/onboarding")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AdminOnboardingController {

    private final AdminOnboardingService adminOnboardingService;
    private final OnboardingAuthService onboardingAuthService;

    @PostMapping("/add-professor")
    public ResponseEntity<?> addProfessor(
            @RequestBody ProfessorMaster request
    ) {
        adminOnboardingService.addProfessor(request);
        return ResponseEntity.ok(
                Map.of("message", "Professor added successfully")
        );
    }
    @PostMapping("/add-professors-csv")
    public ResponseEntity<?> uploadProfessorsCsv(
            @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> result = adminOnboardingService.addProfessorsFromCsv(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-student")
    public ResponseEntity<?> addStudent(
            @RequestBody StudentMaster request
    ) {
        adminOnboardingService.addStudent(request);
        return ResponseEntity.ok(
                Map.of("message", "Student added successfully")
        );
    }
    @PostMapping("/add-students-csv")
    public ResponseEntity<?> addStudentsFromCsv(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                adminOnboardingService.addStudentsFromCsv(file)
        );
    }

    @PostMapping("/add-staff")
    public ResponseEntity<?> addStaff(@RequestBody AddStaffRequest request) {
        adminOnboardingService.addStaff(request);
        return ResponseEntity.ok(
                Map.of("message", "Staff added successfully")
        );
    }



    @PostMapping("/add-staff-csv")
    public ResponseEntity<?> addStaffFromCsv(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                adminOnboardingService.addStaffFromCsv(file)
        );
    }



}
