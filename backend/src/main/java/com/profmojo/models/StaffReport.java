package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requestId;
    private String staffId;
    private String staffName;
    private String professorId;
    private String professorName;
    private String department;

    @Column(length = 1000)
    private String reason;

    private LocalDateTime createdAt;
    private boolean resolved;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
}