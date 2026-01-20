package com.profmojo.controllers;

import com.profmojo.models.Admin;
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
@RequestMapping("/api/admin/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AdminAmenityController {

    private final AmenityRequestRepository amenityRepo;
    private final StaffRepository staffRepo;

    // 1️⃣ View pending requests
    @GetMapping("/pending")
    public List<AmenityRequest> getPending(
            @AuthenticationPrincipal Admin admin
    ) {
        return amenityRepo.findByDepartmentAndStatus(
                admin.getDepartment(),
                RequestStatus.PENDING
        );
    }

    // 2️⃣ View available staff
    @GetMapping("/staff/available")
    public List<Staff> getAvailableStaff(
            @AuthenticationPrincipal Admin admin
    ) {
        return staffRepo.findByDepartmentAndAvailableTrue(
                admin.getDepartment()
        );
    }

    @PutMapping("/{id}/assign/{staffId}")
    public ResponseEntity<?> assignStaff(
            @PathVariable Long id,
            @PathVariable String staffId
    ) {

        AmenityRequest request = amenityRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        LocalDateTime now = LocalDateTime.now();

        request.setAssignedStaff(staff);
        request.setAssignedAt(now);
        request.setStatus(RequestStatus.ASSIGNED);

        // ✅ CHECK ADMIN ASSIGNMENT SLA
        if (request.getAssignmentDeadline() != null &&
                now.isAfter(request.getAssignmentDeadline())) {
            request.setAssignmentSlaBreached(true);
        }

        // ✅ START DELIVERY SLA (example: 10 mins)
        request.setDeliveryDeadline(now.plusMinutes(10));

        amenityRepo.save(request);

        return ResponseEntity.ok("Staff assigned");
    }


    @GetMapping("/staff/all")
    public List<Staff> getAllStaff(
            @AuthenticationPrincipal Admin admin
    ) {
        return staffRepo.findByDepartment(admin.getDepartment());
    }
}
