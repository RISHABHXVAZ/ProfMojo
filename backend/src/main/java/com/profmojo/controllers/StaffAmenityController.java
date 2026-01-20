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

    @PutMapping("/{requestId}/delivered")
    public ResponseEntity<?> markDelivered(
            @PathVariable Long requestId,
            @AuthenticationPrincipal Staff staff
    ) {

        AmenityRequest request = amenityRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Safety check
        if (request.getAssignedStaff() == null ||
                !request.getAssignedStaff().getStaffId().equals(staff.getStaffId())) {
            throw new RuntimeException("Not authorized to deliver this request");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1️⃣ Update request
        request.setStatus(RequestStatus.DELIVERED);
        request.setDeliveredAt(now);

        // 2️⃣ Update staff deliveries
        staff.setTotalDeliveries(staff.getTotalDeliveries() + 1);

        // 3️⃣ SLA check
        if (request.getDeliveryDeadline() != null && now.isAfter(request.getDeliveryDeadline())) {
            // SLA breached → lose star
            staff.setStars(Math.max(0, staff.getStars() - 1));
            request.setDeliverySlaBreached(true);
        } else {
            // SLA met → gain star
            staff.setStars(staff.getStars() + 1);
            request.setDeliverySlaBreached(false);
        }

        // 4️⃣ Mark staff available again
        staff.setAvailable(true);

        // 5️⃣ Persist both
        staffRepo.save(staff);
        amenityRepo.save(request);

        return ResponseEntity.ok("Delivered successfully");
    }




    @GetMapping("/my")
    public List<AmenityRequest> getMyAssignedRequests(
            @AuthenticationPrincipal Staff staff
    ) {
        return amenityRepo.findByAssignedStaffAndStatus(
                staff,
                RequestStatus.ASSIGNED
        );
    }

    @GetMapping("/me")
    public Staff getMyProfile(
            @AuthenticationPrincipal Staff staff
    ) {
        return staff;
    }

}
