package com.profmojo.services;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.dto.AmenityResponseDTO;

import java.util.List;

public interface AdminAmenityService {

    List<AmenityResponseDTO> getPendingRequests(String department);

    AmenityRequest assignStaff(Long requestId, String staffId);

    List<AmenityResponseDTO> getOngoingRequests(String department);

    List<AmenityResponseDTO> getCompletedRequests(String department);

    List<Staff> getAllStaff(String department);
    List<Staff> getAvailableStaff(String department);

    AmenityRequest addToQueue(Long requestId);
    List<AmenityResponseDTO> getQueuedRequests(String department);

    void tryAssignQueuedRequest(Staff staff);

}
