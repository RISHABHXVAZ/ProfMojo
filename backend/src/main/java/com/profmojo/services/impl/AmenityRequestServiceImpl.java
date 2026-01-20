package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityRequestServiceImpl
        implements AmenityRequestService {

    private final AmenityRequestRepository repository;

    @Override
    public AmenityRequest raiseRequest(
            AmenityRequestDTO dto,
            Professor professor
    ) {

        LocalDateTime now = LocalDateTime.now();

        AmenityRequest request = AmenityRequest.builder()
                .professorId(professor.getProfId())
                .professorName(professor.getName())
                .classRoom(dto.getClassRoom())
                .department(dto.getDepartment())
                .items(dto.getItems())
                .status(RequestStatus.PENDING)
                .createdAt(now)

                .assignmentDeadline(now.plusMinutes(2))

                .build();

        return repository.save(request);
    }

    @Override
    public List<AmenityRequest> getMyRequests(String professorId) {
        return repository
                .findByProfessorIdAndStatusNot(
                        professorId,
                        RequestStatus.DELIVERED
                );

    }

    @Override
    public List<AmenityRequest> getMyDeliveredRequests(String professorId) {
        return repository
                .findByProfessorIdAndStatusOrderByDeliveredAtDesc(
                        professorId,
                        RequestStatus.DELIVERED
                );
    }


}
