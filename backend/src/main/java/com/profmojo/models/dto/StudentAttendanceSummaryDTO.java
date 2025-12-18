package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentAttendanceSummaryDTO {

    private String classCode;
    private String className;
    private String professorName;

    private long totalLectures;
    private long presentCount;
    private double percentage;

    // ✅ ADD THIS CONSTRUCTOR
    public StudentAttendanceSummaryDTO(
            String classCode,
            String className,
            String professorName,
            long totalLectures,
            long presentCount
    ) {
        this.classCode = classCode;
        this.className = className;
        this.professorName = professorName;
        this.totalLectures = totalLectures;
        this.presentCount = presentCount;
        this.percentage =
                totalLectures == 0 ? 0.0 : (presentCount * 100.0 / totalLectures);
    }
}

