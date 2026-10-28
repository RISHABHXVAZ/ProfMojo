package com.profmojo.controllers;

import com.profmojo.models.*;
import com.profmojo.models.dto.AmenityResponseDTO;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/amenities")
@RequiredArgsConstructor
public class AdminAmenityController {

    private final AdminAmenityService adminAmenityService;
    private final DepartmentSecretRepository departmentSecretRepository;
    private final AmenityRequestRepository amenityRequestRepository;

    // Helper method to get department from authentication
    private String getDepartmentFromAuth(Authentication authentication) {
        if (authentication == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated admin");
        }
        String secretKey = authentication.getName();
        DepartmentSecret secret = departmentSecretRepository.findBySecretKey(secretKey)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Secret not found for key: " + secretKey));
        if (!secret.isActive()) {
            throw new org.springframework.security.access.AccessDeniedException("Secret key is disabled");
        }
        return secret.getDepartment();
    }

    @GetMapping("/pending")
    public List<AmenityResponseDTO> getPending(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getPendingRequests(department);
    }

    @GetMapping("/ongoing")
    public List<AmenityResponseDTO> getOngoingRequests(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getOngoingRequests(department);
    }

    @GetMapping("/completed")
    public List<AmenityResponseDTO> getCompletedRequests(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getCompletedRequests(department);
    }

    @PutMapping("/{requestId}/assign/{staffId}")
    public AmenityResponseDTO assignStaff(
            @PathVariable Long requestId,
            @PathVariable String staffId,
            Authentication authentication) {

        String adminDepartment = getDepartmentFromAuth(authentication);

        // Assign staff (department-isolated, concurrency-safe atomic assignment with secure code generation)
        AmenityRequest request = adminAmenityService.assignStaff(requestId, staffId, adminDepartment);

        return AmenityResponseDTO.fromEntity(request);
    }

    @GetMapping("/staff/all")
    public List<Staff> getAllStaff(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getAllStaff(department);
    }

    @GetMapping("/staff/available")
    public List<Staff> getAvailableStaff(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getAvailableStaff(department);
    }

    @PutMapping("/{requestId}/queue")
    public AmenityResponseDTO addToQueue(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        String adminDepartment = getDepartmentFromAuth(authentication);
        return AmenityResponseDTO.fromEntity(adminAmenityService.addToQueue(requestId, adminDepartment));
    }

    @GetMapping("/queue")
    public List<AmenityResponseDTO> getQueued(Authentication authentication) {
        String department = getDepartmentFromAuth(authentication);
        return adminAmenityService.getQueuedRequests(department);
    }
}
