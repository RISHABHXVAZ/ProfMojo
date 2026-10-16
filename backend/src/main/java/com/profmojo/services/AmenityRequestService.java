package com.profmojo.services;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.dto.AmenityResponseDTO;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;

import java.util.List;
import java.util.Map;

public interface AmenityRequestService {

    AmenityRequest raiseRequest(
            AmenityRequestDTO dto,
            Professor professor
    );

    List<AmenityResponseDTO> getMyRequests(String professorId);

    List<AmenityResponseDTO> getMyDeliveredRequests(String professorId);

    AmenityRequest findById(Long requestId);

    AmenityRequest reRequestDueToSLABreach(Long requestId, String professorId);

    Map<String, Object> confirmDelivery(Long requestId, String confirmationCode, Staff staff);
}
