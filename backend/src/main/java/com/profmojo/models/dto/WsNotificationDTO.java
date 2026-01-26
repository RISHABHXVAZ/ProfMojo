package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WsNotificationDTO {
    private String event;      // TASK_ASSIGNED, TASK_DELIVERED
    private Long requestId;
    private String message;
    private String type;
}
