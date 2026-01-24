package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // PROFESSOR → Raise request
    @PostMapping("/request")
    public ResponseEntity<?> raiseRequest(
            @RequestBody AmenityRequestDTO dto,
            @AuthenticationPrincipal Professor professor
    ) {
        return ResponseEntity.ok(
                amenityRequestService.raiseRequest(dto, professor)
        );
    }

    // PROFESSOR → View my requests
    @GetMapping("/my")
    public List<AmenityRequest> myRequests(
            @AuthenticationPrincipal Professor professor
    ) {
        return amenityRequestService.getMyRequests(
                professor.getProfId()
        );
    }

    // PROFESSOR → View delivered (history)
    @GetMapping("/my/history")
    public List<AmenityRequest> myDeliveredRequests(
            @AuthenticationPrincipal Professor professor
    ) {
        return amenityRequestService.getMyDeliveredRequests(
                professor.getProfId()
        );
    }

    // PROFESSOR → Check if SLA is breached for a request
    @GetMapping("/check-sla/{requestId}")
    public ResponseEntity<?> checkAssignmentSLA(
            @PathVariable String requestId, // Changed to String to handle IDs like "24:1"
            @AuthenticationPrincipal Professor professor
    ) {
        try {
            // 1. Handle potential formatting issues in the ID (like "24:1")
            Long id;
            try {
                id = Long.parseLong(requestId.split(":")[0]);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID format"));
            }

            AmenityRequest request = amenityRequestService.findById(id);

            // 2. Safety Check: Request existence
            if (request == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Request not found"));
            }

            // 3. Robust Ownership Check
            // Use String.valueOf to ensure we aren't comparing different Object types
            String ownerId = String.valueOf(request.getProfessorId());
            String currentUserId = String.valueOf(professor.getProfId());

            if (!ownerId.equals(currentUserId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Unauthorized access",
                        "isSlaBreached", false // Explicitly return false to hide the button
                ));
            }

            // 4. Status Check
            if (request.getStatus() != RequestStatus.PENDING) {
                return ResponseEntity.ok(Map.of(
                        "isSlaBreached", false,
                        "canReRequest", false,
                        "reason", "Request is " + request.getStatus()
                ));
            }

            // 5. SLA Calculation
            LocalDateTime requestTime = request.getCreatedAt();
            LocalDateTime now = LocalDateTime.now();
            long minutesPassed = Duration.between(requestTime, now).toMinutes();

            boolean isSlaBreached = minutesPassed >= 2;

            return ResponseEntity.ok(Map.of(
                    "isSlaBreached", isSlaBreached,
                    "canReRequest", isSlaBreached,
                    "minutesPassed", minutesPassed,
                    "requestStatus", request.getStatus().toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{requestId}/re-request")
    public ResponseEntity<?> reRequestAmenity(
            @PathVariable String requestId, // Handle "24:1" format
            @AuthenticationPrincipal Professor professor
    ) {
        try {
            // 1. Parse ID safely
            Long id = Long.parseLong(requestId.split(":")[0]);
            AmenityRequest originalRequest = amenityRequestService.findById(id);

            if (originalRequest == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. Ownership Check
            if (!originalRequest.getProfessorId().equals(professor.getProfId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
            }

            // 3. Server-side SLA Validation (Crucial!)
            long minutesPassed = Duration.between(originalRequest.getCreatedAt(), LocalDateTime.now()).toMinutes();
            if (minutesPassed < 2 && originalRequest.getStatus() == RequestStatus.PENDING) {
                return ResponseEntity.badRequest().body(Map.of("error", "SLA has not been breached yet."));
            }

            // 4. Execute Service Logic
            // This should: Mark old as CANCELLED_BY_SLA and create a NEW one
            AmenityRequest newRequest = amenityRequestService.reRequestDueToSLABreach(id, professor.getProfId());

            return ResponseEntity.ok(Map.of(
                    "message", "New request raised due to SLA breach",
                    "newRequestId", newRequest.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}