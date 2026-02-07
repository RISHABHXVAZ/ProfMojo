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
@Getter
@Setter
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
    // ===== SLA TRACKING =====

    // Admin must assign within 2 minutes
    private LocalDateTime assignmentDeadline;


    // Staff must deliver before this
    private LocalDateTime deliveryDeadline;

    @Column(nullable = false)
    private Boolean assignmentSlaBreached = false;

    @Column(nullable = false)
    private Boolean deliverySlaBreached = false;


    private LocalDateTime assignedAt;
    private LocalDateTime slaDeadline;

    // ===== STAFF =====
    private LocalDateTime deliveredAt;

    private boolean slaBreached = false;

    @Column(name = "delivery_confirmation_code")
    private String deliveryConfirmationCode;

    @Column(name = "confirmation_code_expiry")
    private LocalDateTime confirmationCodeExpiry;


}
