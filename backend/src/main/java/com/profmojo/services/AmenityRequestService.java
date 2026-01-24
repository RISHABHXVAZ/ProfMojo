package com.profmojo.services;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.Professor;
import com.profmojo.models.enums.RequestStatus;

import java.util.List;

public interface AmenityRequestService {

    AmenityRequest raiseRequest(
            AmenityRequestDTO dto,
            Professor professor
    );


    List<AmenityRequest> getMyRequests(String professorId);

    List<AmenityRequest> getMyDeliveredRequests(String professorId);
    AmenityRequest findById(Long requestId);
    AmenityRequest reRequestDueToSLABreach(Long requestId, String professorId);




}
