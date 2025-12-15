package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceSummaryDTO {
    private long totalLectures;
    private double averageAttendance;
    private long lowAttendanceCount;
}
