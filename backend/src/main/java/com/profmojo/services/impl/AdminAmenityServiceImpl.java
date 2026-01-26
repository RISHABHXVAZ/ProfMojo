package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Required for WebSocket
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAmenityServiceImpl implements AdminAmenityService {

    private final AmenityRequestRepository requestRepo;
    private final StaffRepository staffRepo;

    @Override
    @Transactional
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

        LocalDateTime deadline = LocalDateTime.now().plusMinutes(10);
        request.setSlaDeadline(deadline);
        request.setDeliveryDeadline(deadline);

        request.setStatus(RequestStatus.ASSIGNED);

        staffRepo.save(staff);
        AmenityRequest savedRequest = requestRepo.save(request);

        return savedRequest;
    }

    @Override
    public List<AmenityRequest> getPendingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.PENDING);
    }

    @Override
    public List<AmenityRequest> getOngoingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.ASSIGNED);
    }

    @Override
    public List<AmenityRequest> getCompletedRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.DELIVERED);
    }

    @Override
    public List<Staff> getAllStaff(String department) {
        // Change from findAll() to filter by department
        return staffRepo.findByDepartment(department);
    }

    @Override
    public List<Staff> getAvailableStaff(String department) {
        // Change from findByAvailableTrue() to filter by department
        return staffRepo.findByDepartmentAndAvailableTrue(department);
    }


}