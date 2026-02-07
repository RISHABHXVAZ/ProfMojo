package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.StaffReport;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.dto.ReportStaffRequest;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.repositories.StaffReportRepository;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AmenityRequestController {

    private final AmenityRequestService amenityRequestService;
    private final NotificationRepository notificationRepository;
    private final StaffReportRepository staffReportRepository;

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
        // 1. Create the Notification Entity (Database only)
        Notification notif = Notification.builder()
                .recipientRole("ADMIN")
                .department(request.getDepartment())
                .message("New request from Prof. " + request.getProfessorName() + " in " + request.getClassRoom())
                .type(type)
                .eventType("NEW_REQUEST")
                .notificationKey("NEW_REQUEST-" + request.getId() + "-" + request.getDepartment())
                .entityId(request.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 2. SAVE TO DATABASE ONLY (No WebSocket)
        notificationRepository.save(notif);
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

            Notification notif = Notification.builder()
                    .recipientRole("ADMIN")
                    .department(newRequest.getDepartment())
                    .message("🚨 SLA BREACH RE-REQUEST: Room " + newRequest.getClassRoom())
                    .type("warning")
                    .eventType("SLA_RE_REQUEST")
                    .notificationKey("SLA_RE_REQUEST-" + newRequest.getId() + "-" + newRequest.getDepartment())
                    .entityId(newRequest.getId())
                    .isRead(false)
                    .isArchived(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notif);

            return ResponseEntity.ok(Map.of("message", "New request raised", "newRequestId", newRequest.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Add this method
    @PostMapping("/report-staff")
    public ResponseEntity<?> reportStaff(
            @RequestBody ReportStaffRequest request,
            @AuthenticationPrincipal Professor professor) {

        try {
            // Get the request to validate
            AmenityRequest amenityRequest = amenityRequestService.findById(request.getRequestId());

            if (amenityRequest == null || !amenityRequest.getProfessorId().equals(professor.getProfId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
            }

            // Save report to database
            StaffReport report = StaffReport.builder()
                    .requestId(request.getRequestId())
                    .staffId(request.getStaffId())
                    .staffName(request.getStaffName())
                    .professorId(professor.getProfId())
                    .professorName(professor.getName())
                    .department(request.getDepartment())
                    .reason(request.getReason())
                    .createdAt(LocalDateTime.now())
                    .resolved(false)
                    .build();

            staffReportRepository.save(report);

            // Notify admin (Database only)
            Notification adminNotif = Notification.builder()
                    .message("🚨 STAFF REPORT: Prof. " + professor.getName() +
                            " reported staff " + request.getStaffName() +
                            " for request #" + request.getRequestId())
                    .type("error")
                    .department(request.getDepartment())
                    .recipientRole("ADMIN")
                    .createdAt(LocalDateTime.now())
                    .isRead(false)
                    .build();
            notificationRepository.save(adminNotif);

            return ResponseEntity.ok(Map.of("message", "Staff reported successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}