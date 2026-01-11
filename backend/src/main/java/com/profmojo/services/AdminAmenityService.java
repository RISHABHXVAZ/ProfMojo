package com.profmojo.services;

import com.profmojo.models.AmenityRequest;

import java.util.List;

public interface AdminAmenityService {

    /**
     * Fetch all pending amenity requests
     * for the admin's department
     */
    List<AmenityRequest> getPendingRequests(String department);

    /**
     * Assign a staff member to an amenity request
     */
    AmenityRequest assignStaff(Long requestId, String staffId);
}
