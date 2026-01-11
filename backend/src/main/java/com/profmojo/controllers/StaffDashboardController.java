package com.profmojo.controllers;


import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/dashboard")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class StaffDashboardController {

    private final AmenityRequestRepository amenityRepo;

    @GetMapping("/assigned")
    public List<AmenityRequest> getAssignedRequests(
            @AuthenticationPrincipal Staff staff
    ) {
        return amenityRepo.findByAssignedStaff_StaffIdAndStatus(
                staff.getStaffId(),
                RequestStatus.ASSIGNED
        );
    }
}

