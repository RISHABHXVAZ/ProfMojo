package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.services.AmenityRequestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Required for WebSocket
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityRequestServiceImpl implements AmenityRequestService {

    private final AmenityRequestRepository repository;
    private final SimpMessagingTemplate messagingTemplate; // Inject Messaging Template

    @Override
    public AmenityRequest raiseRequest(AmenityRequestDTO dto, Professor professor) {
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
                .assignmentSlaBreached(false) // Ensure defaults to avoid NPE
                .deliverySlaBreached(false)
                .build();

        AmenityRequest savedRequest = repository.save(request);

        // REAL-TIME NOTIFICATION TO ADMIN
        // Admin Dashboard will subscribe to this topic
        messagingTemplate.convertAndSend("/topic/admin-notifications",
                "New Amenity Request: Room " + savedRequest.getClassRoom() + " by Prof. " + savedRequest.getProfessorName());

        return savedRequest;
    }

    @Override
    public List<AmenityRequest> getMyRequests(String professorId) {
        return repository.findByProfessorIdAndStatusNot(professorId, RequestStatus.DELIVERED);
    }

    @Override
    public List<AmenityRequest> getMyDeliveredRequests(String professorId) {
        return repository.findByProfessorIdAndStatusOrderByDeliveredAtDesc(professorId, RequestStatus.DELIVERED);
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

        AmenityRequest newReq = AmenityRequest.builder()
                .professorId(profId)
                .professorName(oldReq.getProfessorName())
                .department(oldReq.getDepartment())
                .classRoom(oldReq.getClassRoom())
                .items(new ArrayList<>(oldReq.getItems()))
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .assignmentDeadline(LocalDateTime.now().plusMinutes(2))
                .assignmentSlaBreached(false)
                .deliverySlaBreached(false)
                .build();

        AmenityRequest savedNewReq = repository.save(newReq);

        // NOTIFY ADMIN ABOUT RE-REQUEST
        messagingTemplate.convertAndSend("/topic/admin-notifications",
                "RE-REQUEST (SLA BREACH): Room " + savedNewReq.getClassRoom());

        return savedNewReq;
    }
}