package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAmenityServiceImpl implements AdminAmenityService {

    private final AmenityRequestRepository requestRepo;
    private final StaffRepository staffRepo;

    @Override
    public List<AmenityRequest> getPendingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(
                department,
                RequestStatus.PENDING
        );
    }

    @Override
    public AmenityRequest assignStaff(Long requestId, String staffId) {

        AmenityRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!staff.isAvailable()) {
            throw new RuntimeException("Staff is not available");
        }

        staff.setAvailable(false);

        request.setAssignedStaff(staff);
        request.setAssignedAt(LocalDateTime.now());
        request.setStatus(RequestStatus.ASSIGNED);

        staffRepo.save(staff);
        return requestRepo.save(request);
    }
}
