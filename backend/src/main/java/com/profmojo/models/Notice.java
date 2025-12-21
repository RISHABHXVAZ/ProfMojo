package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String message;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by_prof_id")
    private Professor createdBy;
}
