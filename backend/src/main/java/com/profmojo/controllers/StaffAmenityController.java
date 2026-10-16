package com.profmojo.controllers;

import com.profmojo.models.Staff;
import com.profmojo.models.dto.AmenityResponseDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/amenities")
@RequiredArgsConstructor
public class StaffAmenityController {

    private final AmenityRequestRepository amenityRepo;
    private final StaffRepository staffRepo;
    private final AmenityRequestService amenityRequestService;

    @PutMapping("/{requestId}/delivered")
    public ResponseEntity<?> markAsDelivered(
            @PathVariable Long requestId,
            @RequestParam String confirmationCode,
            @AuthenticationPrincipal Staff staff) {

        try {
            Map<String, Object> result = amenityRequestService.confirmDelivery(requestId, confirmationCode, staff);
            return ResponseEntity.ok(result);
        } catch (com.profmojo.exception.DeliveryLockoutException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                    .body(Map.of("error", e.getMessage(), "retryAfter", e.getRetryAfterSeconds()));
        } catch (com.profmojo.exception.SecurityInfrastructureUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getMyAssignedRequests(@AuthenticationPrincipal Staff staff) {
        return amenityRepo.findByAssignedStaffAndStatus(staff, RequestStatus.ASSIGNED)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/me")
    public Staff getMyProfile(@AuthenticationPrincipal Staff staff) {
        // Return fresh data from DB to show updated stars/deliveries
        return staffRepo.findById(staff.getStaffId()).orElse(staff);
    }
}