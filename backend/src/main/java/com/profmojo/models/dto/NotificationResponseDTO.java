package com.profmojo.models.dto;

import com.profmojo.models.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private String message;
    private String type;
    private String eventType;
    private Long entityId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String department;

    public static NotificationResponseDTO fromEntity(Notification n) {
        if (n == null) {
            return null;
        }
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .eventType(n.getEventType())
                .entityId(n.getEntityId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .department(n.getDepartment())
                .build();
    }
}
