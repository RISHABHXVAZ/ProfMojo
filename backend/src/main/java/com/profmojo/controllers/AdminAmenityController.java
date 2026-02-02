package com.profmojo.controllers;

import com.profmojo.models.*;
import com.profmojo.models.dto.WsNotificationDTO;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AdminAmenityController {

    private final AdminAmenityService adminAmenityService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final DepartmentSecretRepository departmentSecretRepository;

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

        // First, get the request to validate department
        // You need to add findById method to AdminAmenityService or use repository directly
        // For now, let's assume you have access to AmenityRequestRepository

        // ADD THIS VALIDATION LOGIC:
        // 1. Get the request
        // 2. Check if request.department equals adminDepartment
        // 3. If not, throw unauthorized exception

        // 1️⃣ Assign staff (service should handle department validation internally)
        AmenityRequest request = adminAmenityService.assignStaff(requestId, staffId);


        // 2️⃣ SAVE notification for STAFF (Database + WebSocket)
        Notification staffNotif = Notification.builder()
                .message("New Task Assigned: Room " + request.getClassRoom())
                .type("info")
                .recipientId(staffId)
                .recipientRole("STAFF")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
        notificationRepository.save(staffNotif);

        WsNotificationDTO staffWs = WsNotificationDTO.builder()
                .event("TASK_ASSIGNED")
                .requestId(request.getId())
                .message(staffNotif.getMessage())
                .type("info")
                .build();
        messagingTemplate.convertAndSendToUser(staffId, "/queue/notifications", staffWs);

        // 3️⃣ SAVE + NOTIFY PROFESSOR (Database + WebSocket)
        Notification profNotif = Notification.builder()
                .message("Your request #" + request.getId() + " has been assigned to " + request.getAssignedStaff().getName())
                .type("success")
                .recipientId(request.getProfessorId())
                .recipientRole("PROFESSOR")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
        notificationRepository.save(profNotif);

        WsNotificationDTO profWs = WsNotificationDTO.builder()
                .event("REQUEST_ASSIGNED")
                .requestId(request.getId())
                .message(profNotif.getMessage())
                .type("success")
                .build();
        messagingTemplate.convertAndSendToUser(request.getProfessorId(), "/queue/notifications", profWs);

        // 4️⃣ FIXED: SAVE ADMIN NOTIFICATION TO DATABASE
        Notification adminNotif = Notification.builder()
                .message("Staff " + request.getAssignedStaff().getName() +
                        " assigned to Room " + request.getClassRoom())
                .type("success")
                .department(adminDepartment)
                .recipientRole("ADMIN")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
        notificationRepository.save(adminNotif);

        // 5️⃣ SEND ADMIN WEB SOCKET NOTIFICATION
        // Create a simple map for admin notification (frontend expects this format)
        Map<String, Object> adminWsNotif = new HashMap<>();
        adminWsNotif.put("message", adminNotif.getMessage());
        adminWsNotif.put("type", "success");
        adminWsNotif.put("department", adminDepartment);

        messagingTemplate.convertAndSend("/topic/admin-notifications", (Object) adminWsNotif);

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