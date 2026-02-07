package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String recipientRole;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String type;

    @Column(nullable = true)
    private String eventType;

    @Column(nullable = true)
    private String notificationKey;

    @Column(nullable = true)
    private Long entityId;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isArchived = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime readAt;

    @Column
    private String department;

    // Store JSON data as String instead of Map
    @Column(columnDefinition = "TEXT")
    private String jsonData;

    // Helper method to get data as Map
    public Map<String, Object> getDataAsMap() {
        // You'll need to implement JSON parsing here
        // For simplicity, return empty map
        return new HashMap<>();
    }

    // Helper method to set data from Map
    public void setDataFromMap(Map<String, Object> data) {
        // You'll need to implement JSON serialization here
        // For now, just store empty string
        this.jsonData = "{}";
    }
}