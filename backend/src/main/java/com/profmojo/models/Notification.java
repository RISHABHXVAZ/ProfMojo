package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private String type; // info, success, warning, error
    private String department;
    private String recipientId; // For private notifications (Prof ID or Staff ID)
    private String recipientRole; // ADMIN, PROFESSOR, STAFF

    private LocalDateTime createdAt;
    private boolean isRead = false;
}