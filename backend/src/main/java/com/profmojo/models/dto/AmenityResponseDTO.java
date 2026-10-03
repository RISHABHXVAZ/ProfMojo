package com.profmojo.models.dto;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityResponseDTO {
    private Long id;
    private String professorId;
    private String professorName;
    private String department;
    private String classRoom;
    private List<String> items;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private StaffSummaryDTO assignedStaff;
    private LocalDateTime assignmentDeadline;
    private LocalDateTime deliveryDeadline;
    private Boolean assignmentSlaBreached;
    private Boolean deliverySlaBreached;
    private LocalDateTime assignedAt;
    private LocalDateTime slaDeadline;
    private LocalDateTime deliveredAt;
    private boolean slaBreached;
    private String deliveryConfirmationCode;
    private LocalDateTime confirmationCodeExpiry;

    public static AmenityResponseDTO fromEntity(AmenityRequest req) {
        if (req == null) {
            return null;
        }
        return AmenityResponseDTO.builder()
                .id(req.getId())
                .professorId(req.getProfessorId())
                .professorName(req.getProfessorName())
                .department(req.getDepartment())
                .classRoom(req.getClassRoom())
                .items(req.getItems() != null ? new ArrayList<>(req.getItems()) : new ArrayList<>())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .assignedStaff(StaffSummaryDTO.fromEntity(req.getAssignedStaff()))
                .assignmentDeadline(req.getAssignmentDeadline())
                .deliveryDeadline(req.getDeliveryDeadline())
                .assignmentSlaBreached(req.getAssignmentSlaBreached())
                .deliverySlaBreached(req.getDeliverySlaBreached())
                .assignedAt(req.getAssignedAt())
                .slaDeadline(req.getSlaDeadline())
                .deliveredAt(req.getDeliveredAt())
                .slaBreached(req.isSlaBreached())
                .deliveryConfirmationCode(req.getDeliveryConfirmationCode())
                .confirmationCodeExpiry(req.getConfirmationCodeExpiry())
                .build();
    }
}
