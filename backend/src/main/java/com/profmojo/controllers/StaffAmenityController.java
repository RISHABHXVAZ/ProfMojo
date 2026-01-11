package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
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

    @PutMapping("/{requestId}/delivered/{staffId}")
    public ResponseEntity<AmenityRequest> markDelivered(
            @PathVariable Long requestId,
            @PathVariable String staffId
    ) {

        AmenityRequest request = amenityRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        // ✅ Safety check
        if (request.getAssignedStaff() == null ||
                !request.getAssignedStaff().getStaffId().equals(staffId)) {
            throw new RuntimeException("This request is not assigned to you");
        }

        request.setStatus(RequestStatus.DELIVERED);
        request.setDeliveredAt(LocalDateTime.now());

        staff.setAvailable(true);

        staffRepo.save(staff);
        AmenityRequest saved = amenityRepo.save(request);

        return ResponseEntity.ok(saved);
    }
    @GetMapping("/my")
    public List<AmenityRequest> myAssignments(
            @AuthenticationPrincipal Staff staff
    ) {
        return amenityRepo.findByAssignedStaff_StaffIdAndStatus(
                staff.getStaffId(),
                RequestStatus.ASSIGNED
        );
    }
}
