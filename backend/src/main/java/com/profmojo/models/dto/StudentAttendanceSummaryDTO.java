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
}
