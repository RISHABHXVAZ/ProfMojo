package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AmenityRequestController {

    private final AmenityRequestService amenityRequestService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    @PostMapping("/request")
    public ResponseEntity<?> raiseRequest(
            @RequestBody AmenityRequestDTO dto,
            @AuthenticationPrincipal Professor professor
    ) {
        AmenityRequest request = amenityRequestService.raiseRequest(dto, professor);
        sendAdminNotification(request, "info");
        return ResponseEntity.ok(request);
    }

    private void sendAdminNotification(AmenityRequest request, String type) {
        // 1. Create the Notification Entity
        Notification notif = Notification.builder()
                .message("New request from Prof. " + request.getProfessorName() + " in " + request.getClassRoom())
                .type(type)
                .department(request.getDepartment())
                .recipientRole("ADMIN")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        // 2. SAVE TO DATABASE (So it's there when they login later)
        notificationRepository.save(notif);

        // 3. SEND REAL-TIME via WebSocket (with the (Object) cast fix)
        messagingTemplate.convertAndSend("/topic/admin-notifications", (Object) notif);
    }
    @GetMapping("/my")
    public List<AmenityRequest> myRequests(@AuthenticationPrincipal Professor professor) {
        return amenityRequestService.getMyRequests(professor.getProfId());
    }

    @GetMapping("/my/history")
    public List<AmenityRequest> myDeliveredRequests(@AuthenticationPrincipal Professor professor) {
        return amenityRequestService.getMyDeliveredRequests(professor.getProfId());
    }

    @GetMapping("/check-sla/{requestId}")
    public ResponseEntity<?> checkAssignmentSLA(@PathVariable String requestId, @AuthenticationPrincipal Professor professor) {
        try {
            Long id = Long.parseLong(requestId.split(":")[0]);
            AmenityRequest request = amenityRequestService.findById(id);

            if (request == null) return ResponseEntity.status(404).body(Map.of("error", "Request not found"));
            if (!request.getProfessorId().equals(professor.getProfId()))
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized access"));

            if (request.getStatus() != RequestStatus.PENDING) {
                return ResponseEntity.ok(Map.of("isSlaBreached", false, "canReRequest", false));
            }

            long minutesPassed = Duration.between(request.getCreatedAt(), LocalDateTime.now()).toMinutes();
            boolean isSlaBreached = minutesPassed >= 2;

            return ResponseEntity.ok(Map.of("isSlaBreached", isSlaBreached, "canReRequest", isSlaBreached));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{requestId}/re-request")
    public ResponseEntity<?> reRequestAmenity(@PathVariable String requestId, @AuthenticationPrincipal Professor professor) {
        try {
            Long id = Long.parseLong(requestId.split(":")[0]);
            AmenityRequest originalRequest = amenityRequestService.findById(id);

            if (originalRequest == null || !originalRequest.getProfessorId().equals(professor.getProfId())) {
                return ResponseEntity.status(403).build();
            }

            AmenityRequest newRequest = amenityRequestService.reRequestDueToSLABreach(id, professor.getProfId());

            // Notify admin about the Re-Request using the standard format
            Map<String, Object> notification = new HashMap<>();
            notification.put("message", "🚨 SLA BREACH RE-REQUEST: Room " + newRequest.getClassRoom());
            notification.put("type", "warning");
            notification.put("department", newRequest.getDepartment());
            messagingTemplate.convertAndSend("/topic/admin-notifications", (Object) notification);

            return ResponseEntity.ok(Map.of("message", "New request raised", "newRequestId", newRequest.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}