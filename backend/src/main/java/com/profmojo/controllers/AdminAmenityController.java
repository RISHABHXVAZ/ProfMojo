package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff; // Assuming you have a Staff model
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AdminAmenityController {

    private final AdminAmenityService adminAmenityService;

    // ================= PENDING REQUESTS =================
    @GetMapping("/pending")
    public List<AmenityRequest> getPendingRequests(@RequestParam String department) {
        return adminAmenityService.getPendingRequests(department);
    }

    // ================= ONGOING REQUESTS =================
    @GetMapping("/ongoing")
    public List<AmenityRequest> getOngoingRequests(@RequestParam String department) {
        return adminAmenityService.getOngoingRequests(department);
    }

    // ================= COMPLETED REQUESTS =================
    @GetMapping("/completed")
    public List<AmenityRequest> getCompletedRequests(@RequestParam String department) {
        return adminAmenityService.getCompletedRequests(department);
    }

    // ================= ASSIGN STAFF =================
    @PutMapping("/{requestId}/assign/{staffId}")
    public AmenityRequest assignStaff(
            @PathVariable Long requestId,
            @PathVariable String staffId
    ) {
        return adminAmenityService.assignStaff(requestId, staffId);
    }

    // ================= STAFF MANAGEMENT (ADDED) =================

    @GetMapping("/staff/all")
    public List<Staff> getAllStaff() {
        // Since your frontend fetchAllStaff() doesn't pass a param,
        // the service should ideally fetch all or handle filtering.
        return adminAmenityService.getAllStaff();
    }

    @GetMapping("/staff/available")
    public List<Staff> getAvailableStaff() {
        // Used by your Assign Staff Modal
        return adminAmenityService.getAvailableStaff();
    }
}