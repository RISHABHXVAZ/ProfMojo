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

    // 3️⃣ Assign staff to request
    @PutMapping("/{requestId}/assign/{staffId}")
    public ResponseEntity<AmenityRequest> assignStaff(
            @PathVariable Long requestId,
            @PathVariable String staffId
    ) {

        AmenityRequest request = amenityRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!staff.isAvailable()) {
            throw new RuntimeException("Staff not available");
        }

        // ✅ CORRECT MAPPING
        request.setAssignedStaff(staff);
        request.setAssignedAt(LocalDateTime.now());
        request.setStatus(RequestStatus.ASSIGNED);

        staff.setAvailable(false);

        staffRepo.save(staff);
        AmenityRequest saved = amenityRepo.save(request);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/staff/all")
    public List<Staff> getAllStaff(
            @AuthenticationPrincipal Admin admin
    ) {
        return staffRepo.findByDepartment(admin.getDepartment());
    }
}
