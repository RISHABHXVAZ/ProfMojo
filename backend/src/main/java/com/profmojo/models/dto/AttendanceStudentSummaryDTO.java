package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceStudentSummaryDTO {

    private String studentRegNo;
    private long totalLectures;
    private long presentCount;
    private double attendancePercentage;
    private boolean lowAttendance;
}
