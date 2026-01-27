package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Staff;
import com.profmojo.models.dto.WsNotificationDTO;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @GetMapping("/pending")
    public List<AmenityRequest> getPendingRequests(@RequestParam String department) {
        return adminAmenityService.getPendingRequests(department);
    }

    @GetMapping("/ongoing")
    public List<AmenityRequest> getOngoingRequests(@RequestParam String department) {
        return adminAmenityService.getOngoingRequests(department);
    }

    @GetMapping("/completed")
    public List<AmenityRequest> getCompletedRequests(@RequestParam String department) {
        return adminAmenityService.getCompletedRequests(department);
    }

    @PutMapping("/{requestId}/assign/{staffId}")
    public AmenityRequest assignStaff(
            @PathVariable Long requestId,
            @PathVariable String staffId
    ) {

        // 1️⃣ Assign staff (DB only, NO WS inside service)
        AmenityRequest request = adminAmenityService.assignStaff(requestId, staffId);

        // 2️⃣ SAVE notification for STAFF (history)
        Notification staffNotif = Notification.builder()
                .message("New Task Assigned: Room " + request.getClassRoom())
                .type("info")
                .recipientId(staffId)
                .recipientRole("STAFF")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        notificationRepository.save(staffNotif);

        // 3️⃣ REAL-TIME STAFF WS (EVENT-BASED)
        WsNotificationDTO staffWs = WsNotificationDTO.builder()
                .event("TASK_ASSIGNED")
                .requestId(request.getId())
                .message(staffNotif.getMessage())
                .type("info")
                .build();

        messagingTemplate.convertAndSendToUser(
                staffId,
                "/queue/notifications",
                staffWs
        );

        // 4️⃣ SAVE + NOTIFY PROFESSOR
        Notification profNotif = Notification.builder()
                .message("Your request #" + request.getId() +
                        " has been assigned to " +
                        request.getAssignedStaff().getName())
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

        messagingTemplate.convertAndSendToUser(
                request.getProfessorId(),
                "/queue/notifications",
                profWs
        );

        // 5️⃣ ADMIN dashboard notification
        Map<String, Object> adminNotif = new HashMap<>();
        adminNotif.put("message",
                "Staff " + request.getAssignedStaff().getName() +
                        " assigned to Room " + request.getClassRoom());
        adminNotif.put("type", "success");
        adminNotif.put("department", request.getDepartment());

        messagingTemplate.convertAndSend("/topic/admin-notifications", (Object) adminNotif);

        return request;
    }

    @GetMapping("/staff/all")
    public List<Staff> getAllStaff(@RequestParam String department) {
        return adminAmenityService.getAllStaff(department);
    }

    @GetMapping("/staff/available")
    public List<Staff> getAvailableStaff(@RequestParam String department) {
        return adminAmenityService.getAvailableStaff(department);
    }

    @PutMapping("/{requestId}/queue")
    public AmenityRequest addToQueue(@PathVariable Long requestId) {
        return adminAmenityService.addToQueue(requestId);
    }

    @GetMapping("/queue")
    public List<AmenityRequest> getQueued(@RequestParam String department) {
        return adminAmenityService.getQueuedRequests(department);
    }

}
