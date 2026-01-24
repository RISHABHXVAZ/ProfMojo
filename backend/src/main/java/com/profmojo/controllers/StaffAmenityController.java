package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class StaffAmenityController {

    private final AmenityRequestRepository amenityRepo;
    private final StaffRepository staffRepo;

    @PutMapping("/{requestId}/delivered")
    @Transactional
    public ResponseEntity<?> markAsDelivered(@PathVariable Long requestId) {
        AmenityRequest request = amenityRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Staff staff = request.getAssignedStaff();
        if (staff == null) {
            return ResponseEntity.badRequest().body("No staff assigned to this request");
        }

        LocalDateTime now = LocalDateTime.now();

        // CHECK SLA BREACH FOR STAR PENALTY
        if (request.getSlaDeadline() != null && now.isAfter(request.getSlaDeadline())) {
            // Penalty: Decrease stars by 1 (minimum 0)
            staff.setStars(Math.max(0, staff.getStars() - 1));
            request.setDeliverySlaBreached(true);
            request.setSlaBreached(true);
        } else {
            // Reward: On-time delivery earns 1 star
            staff.setStars(staff.getStars() + 1);
            request.setDeliverySlaBreached(false);
        }

        // Update Staff Stats
        staff.setTotalDeliveries(staff.getTotalDeliveries() + 1);
        staff.setAvailable(true); // Staff is free to take new requests

        // Update Request Status
        request.setStatus(RequestStatus.DELIVERED);
        request.setDeliveredAt(now);

        staffRepo.save(staff);
        amenityRepo.save(request);

        return ResponseEntity.ok("Package marked as delivered. Stars updated.");
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