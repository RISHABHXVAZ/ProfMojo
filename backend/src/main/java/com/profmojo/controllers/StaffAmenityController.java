package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.models.Notification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class StaffAmenityController {

    private final AmenityRequestRepository amenityRepo;
    private final StaffRepository staffRepo;
    private final NotificationRepository notificationRepository;

    @PutMapping("/{requestId}/delivered")
    @Transactional
    public ResponseEntity<?> markAsDelivered(
            @PathVariable Long requestId,
            @RequestParam String confirmationCode,
            @AuthenticationPrincipal Staff staff) {

        try {
            // 1. Get the request
            AmenityRequest request = amenityRepo.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            // 2. Verify staff is assigned to this request
            if (request.getAssignedStaff() == null ||
                    !request.getAssignedStaff().getStaffId().equals(staff.getStaffId())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "You are not assigned to this request")
                );
            }

            // 3. Verify confirmation code
            if (request.getDeliveryConfirmationCode() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "No confirmation code set for this request")
                );
            }

            if (!request.getDeliveryConfirmationCode().equals(confirmationCode.trim())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid confirmation code")
                );
            }

            // 4. Check if code is expired (optional - if you added expiry field)
            if (request.getConfirmationCodeExpiry() != null &&
                    request.getConfirmationCodeExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Confirmation code has expired")
                );
            }

            // 5. Check if already delivered
            if (request.getStatus() == RequestStatus.DELIVERED) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Request is already marked as delivered")
                );
            }

            LocalDateTime now = LocalDateTime.now();
            boolean slaBreached = false;
            String message;

            // 6. CHECK SLA BREACH FOR STAR PENALTY/REWARD
            if (request.getSlaDeadline() != null && now.isAfter(request.getSlaDeadline())) {
                // Penalty: Decrease stars by 1 (minimum 0)
                staff.setStars(Math.max(0, staff.getStars() - 1));
                request.setDeliverySlaBreached(true);
                request.setSlaBreached(true);
                slaBreached = true;
                message = "Package marked as delivered but SLA breached. Star decreased by 1.";
            } else {
                // Reward: On-time delivery earns 1 star
                staff.setStars(staff.getStars() + 1);
                request.setDeliverySlaBreached(false);
                slaBreached = false;
                message = "Package marked as delivered successfully! Star increased by 1.";
            }

            // 7. Update Staff Stats
            staff.setTotalDeliveries(staff.getTotalDeliveries() + 1);
            staff.setAvailable(true);

            // 8. Clear confirmation code (one-time use)
            request.setDeliveryConfirmationCode(null);
            request.setConfirmationCodeExpiry(null);

            // 9. Update Request Status
            request.setStatus(RequestStatus.DELIVERED);
            request.setDeliveredAt(now);

            // 10. Save changes
            staffRepo.save(staff);
            amenityRepo.save(request);

            // 11. Create notifications
            Notification staffNotif = Notification.builder()
                    .recipientId(staff.getStaffId())
                    .recipientRole("STAFF")
                    .message(slaBreached ?
                            "Task #" + requestId + " delivered but SLA breached. Star decreased by 1." :
                            "Task #" + requestId + " delivered successfully! Star increased by 1.")
                    .type(slaBreached ? "warning" : "success")
                    .eventType("TASK_DELIVERED")
                    .notificationKey("TASK_DELIVERED-" + requestId + "-" + staff.getStaffId())
                    .entityId(requestId)
                    .isRead(false)
                    .isArchived(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(staffNotif);

            Notification profNotif = Notification.builder()
                    .recipientId(request.getProfessorId())
                    .recipientRole("PROFESSOR")
                    .message("Your items have been delivered to " + request.getClassRoom())
                    .type("success")
                    .eventType("DELIVERY_COMPLETED")
                    .notificationKey("DELIVERY_COMPLETED-" + requestId + "-" + request.getProfessorId())
                    .entityId(requestId)
                    .isRead(false)
                    .isArchived(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(profNotif);

            // 12. Auto-assign next queued request (your existing logic)
            amenityRepo
                    .findFirstByDepartmentAndStatusOrderByCreatedAtAsc(
                            staff.getDepartment(), RequestStatus.QUEUED
                    )
                    .ifPresent(queuedRequest -> {
                        queuedRequest.setAssignedStaff(staff);
                        queuedRequest.setAssignedAt(LocalDateTime.now());

                        LocalDateTime deadline = LocalDateTime.now().plusMinutes(5);
                        queuedRequest.setSlaDeadline(deadline);
                        queuedRequest.setDeliveryDeadline(deadline);

                        queuedRequest.setStatus(RequestStatus.ASSIGNED);
                        staff.setAvailable(false);

                        amenityRepo.save(queuedRequest);
                        staffRepo.save(staff);
                    });

            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "slaBreached", slaBreached,
                    "newStars", staff.getStars()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @GetMapping("/my")
    public List<AmenityRequest> getMyAssignedRequests(@AuthenticationPrincipal Staff staff) {
        return amenityRepo.findByAssignedStaffAndStatus(staff, RequestStatus.ASSIGNED);
    }

    @GetMapping("/me")
    public Staff getMyProfile(@AuthenticationPrincipal Staff staff) {
        // Return fresh data from DB to show updated stars/deliveries
        return staffRepo.findById(staff.getStaffId()).orElse(staff);
    }
}