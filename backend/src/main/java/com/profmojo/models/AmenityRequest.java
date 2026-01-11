package com.profmojo.models;

import com.profmojo.models.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "amenity_request")
public class AmenityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String professorId;
    private String professorName;

    private String department;
    private String classRoom;

    @ElementCollection
    @CollectionTable(
            name = "amenity_request_items",
            joinColumns = @JoinColumn(name = "amenity_request_id")
    )
    @Column(name = "item")
    private List<String> items;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdAt;

    // ===== ADMIN =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_staff_id",
            referencedColumnName = "staffId"
    )
    private Staff assignedStaff;

    private LocalDateTime assignedAt;
    private LocalDateTime slaDeadline;

    // ===== STAFF =====
    private LocalDateTime deliveredAt;

    private boolean slaBreached = false;
}
