package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "student_classes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_reg_no", "class_code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_reg_no", nullable = false)
    private String studentRegNo;

    @Column(name = "class_code", nullable = false)
    private String classCode;
}
