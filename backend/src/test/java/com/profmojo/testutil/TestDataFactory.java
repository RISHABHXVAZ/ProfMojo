package com.profmojo.testutil;

import com.profmojo.models.*;
import java.time.LocalDateTime;
import java.util.List;

public class TestDataFactory {

    public static DepartmentSecret createDepartmentSecret(String secretKey, String department, String email, boolean active) {
        return DepartmentSecret.builder()
                .secretKey(secretKey)
                .department(department)
                .adminEmail(email)
                .active(active)
                .build();
    }

    public static OnboardingOtp createOtp(String userId, String role, String otp, int expiryMinutes) {
        return OnboardingOtp.builder()
                .userId(userId)
                .role(role)
                .otp(otp)
                .expiry(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
    }

    public static ProfessorMaster createProfessorMaster(String profId, String name, String department, String email) {
        return ProfessorMaster.builder()
                .profId(profId)
                .name(name)
                .department(department)
                .email(email)
                .build();
    }

    public static StudentMaster createStudentMaster(String regNo, String name, String department, String email) {
        return StudentMaster.builder()
                .regNo(regNo)
                .name(name)
                .department(department)
                .email(email)
                .build();
    }

    public static Staff createStaff(String staffId, String name, String department, String email, boolean available, boolean online) {
        return Staff.builder()
                .staffId(staffId)
                .name(name)
                .department(department)
                .email(email)
                .contactNo("1234567890")
                .available(available)
                .online(online)
                .role("STAFF")
                .stars(0)
                .totalDeliveries(0)
                .build();
    }

    public static AmenityRequest createAmenityRequest(Long id, String profId, String department, com.profmojo.models.enums.RequestStatus status) {
        return AmenityRequest.builder()
                .id(id)
                .professorId(profId)
                .professorName("Prof Test")
                .department(department)
                .status(status)
                .classRoom("Room 101")
                .createdAt(LocalDateTime.now())
                .slaDeadline(LocalDateTime.now().plusMinutes(15))
                .slaBreached(false)
                .deliverySlaBreached(false)
                .build();
    }
}
