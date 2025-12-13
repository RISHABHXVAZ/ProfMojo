package com.profmojo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "class_rooms")
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String className;   // C Section, D Section

    @Column(unique = true, nullable = false)
    private String classCode;   // Auto-generated (e.g. CSE-C-AX92)

    @ManyToOne
    @JoinColumn(name = "prof_id", nullable = false)
    private Professor professor;

    private LocalDateTime createdAt = LocalDateTime.now();
}
