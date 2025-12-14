package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"class_code", "student_reg_no", "attendance_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", nullable = false)
    private String classCode;

    @Column(name = "student_reg_no", nullable = false)
    private String studentRegNo;

    @Column(nullable = false)
    private boolean present;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;
}

