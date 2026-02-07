package com.profmojo.controllers;

import com.profmojo.models.*;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/admin/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AdminAmenityController {

    private final AdminAmenityService adminAmenityService;
    private final NotificationRepository notificationRepository;
    private final DepartmentSecretRepository departmentSecretRepository;
    private final AmenityRequestRepository amenityRequestRepository;

    // Helper method to get department from authentication
    private String getDepartmentFromAuth(Authentication authentication) {
        String secretKey = authentication.getName();
        DepartmentSecret secret = departmentSecretRepository.findBySecretKey(secretKey)
                .orElseThrow(() -> new RuntimeException("Secret not found for key: " + secretKey));
        return secret.getDepartment();
    }

    @GetMapping("/pending")
    public List<AmenityRequest> getPending(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getPendingRequests(department);
    }

    @GetMapping("/ongoing")
    public List<AmenityRequest> getOngoingRequests(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getOngoingRequests(department);
    }

    @GetMapping("/completed")
    public List<AmenityRequest> getCompletedRequests(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getCompletedRequests(department);
    }

    @PutMapping("/{requestId}/assign/{staffId}")
    public AmenityRequest assignStaff(
            @PathVariable Long requestId,
            @PathVariable String staffId,
            Authentication authentication) {

        String adminDepartment = getDepartmentFromAuth(authentication);
        String adminId = authentication.getName();

        // 1️⃣ Assign staff (make sure this generates confirmation code)
        AmenityRequest request = adminAmenityService.assignStaff(requestId, staffId);

        // 2️⃣ Generate 4-digit confirmation code
        String confirmationCode = String.format("%04d", new Random().nextInt(10000));
        request.setDeliveryConfirmationCode(confirmationCode);
        request.setConfirmationCodeExpiry(LocalDateTime.now().plusHours(2));

        // Save the updated request
        request = amenityRequestRepository.save(request);

        // 3️⃣ SAVE notification for STAFF
        Notification staffNotif = Notification.builder()
                .recipientId(staffId)
                .recipientRole("STAFF")
                .message("You have been assigned to Room " + request.getClassRoom() +
                        ". Confirmation Code: " + confirmationCode)
                .type("info")
                .eventType("TASK_ASSIGNED")
                .notificationKey("TASK_ASSIGNED-" + request.getId() + "-" + staffId)
                .entityId(request.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(staffNotif);

        // 4️⃣ SAVE notification for PROFESSOR (with confirmation code)
        Notification profNotif = Notification.builder()
                .recipientId(request.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your request #" + request.getId() + " has been assigned to " +
                        request.getAssignedStaff().getName() +
                        ". Please share this confirmation code with staff: " + confirmationCode)
                .type("success")
                .eventType("REQUEST_ASSIGNED")
                .notificationKey("REQUEST_ASSIGNED-" + request.getId() + "-" + request.getProfessorId())
                .entityId(request.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(profNotif);

        // 5️⃣ SAVE ADMIN NOTIFICATION
        Notification adminNotif = Notification.builder()
                .recipientId(adminId)
                .recipientRole("ADMIN")
                .department(adminDepartment)
                .message("Staff " + request.getAssignedStaff().getName() +
                        " assigned to Room " + request.getClassRoom() +
                        ". Confirmation Code: " + confirmationCode)
                .type("success")
                .eventType("STAFF_ASSIGNED")
                .notificationKey("STAFF_ASSIGNED-" + request.getId() + "-" + adminDepartment)
                .entityId(request.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(adminNotif);

        return request;
    }

    @GetMapping("/staff/all")
    public List<Staff> getAllStaff(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getAllStaff(department);
    }

    @GetMapping("/staff/available")
    public List<Staff> getAvailableStaff(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getAvailableStaff(department);
    }

    @PutMapping("/{requestId}/queue")
    public AmenityRequest addToQueue(@PathVariable Long requestId) {
        return adminAmenityService.addToQueue(requestId);
    }

    @GetMapping("/queue")
    public List<AmenityRequest> getQueued(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getQueuedRequests(department);
    }
}