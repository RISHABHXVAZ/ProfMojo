package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.services.AmenityRequestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Override
    public AmenityRequest findById(Long requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }

    @Override
    @Transactional
    public AmenityRequest reRequestDueToSLABreach(Long originalId, String profId) {
        AmenityRequest oldReq = repository.findById(originalId)
                .orElseThrow(() -> new RuntimeException("Request not found"));


        oldReq.setStatus(RequestStatus.CANCELLED_SLA_BREACH);
        repository.save(oldReq);

        // 2. CREATE NEW REQUEST
        AmenityRequest newReq = AmenityRequest.builder()
                .professorId(profId)
                .professorName(oldReq.getProfessorName())
                .department(oldReq.getDepartment())
                .classRoom(oldReq.getClassRoom())
                .items(new ArrayList<>(oldReq.getItems()))
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .assignmentSlaBreached(false)
                .deliverySlaBreached(false)
                .build();

        return repository.save(newReq);
    }
}

