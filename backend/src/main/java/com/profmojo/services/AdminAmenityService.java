package com.profmojo.services;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;

import java.util.List;

public interface AdminAmenityService {


    List<AmenityRequest> getPendingRequests(String department);

    AmenityRequest assignStaff(Long requestId, String staffId);

    List<AmenityRequest> getOngoingRequests(String department);

    List<AmenityRequest> getCompletedRequests(String department);

    List<Staff> getAllStaff(String department);
    List<Staff> getAvailableStaff(String department);

    AmenityRequest addToQueue(Long requestId);
    List<AmenityRequest> getQueuedRequests(String department);

    void tryAssignQueuedRequest(Staff staff);
}
