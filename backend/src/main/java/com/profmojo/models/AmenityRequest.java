package com.profmojo.models;

import com.profmojo.models.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String professorId;
    private String professorName;

    private String classRoom;
    private String department;

    @ElementCollection
    private List<String> items; // chalk, duster, projector

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;

    private Long assignedStaffId;
}
