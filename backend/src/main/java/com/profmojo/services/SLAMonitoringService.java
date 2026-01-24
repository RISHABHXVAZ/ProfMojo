package com.profmojo.services;

import com.profmojo.models.AmenityRequest;

import java.util.List;

public interface SLAMonitoringService {

    List<AmenityRequest> checkAndMarkAssignmentSLA();

    List<AmenityRequest> checkAndMarkDeliverySLA(String department);

    List<AmenityRequest> getAllBreachedAssignmentRequests();

    List<AmenityRequest> getAllBreachedDeliveryRequests(String department);

    void triggerSLACheck();
}