package com.profmojo.models.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AttendanceRequest {

    private String classCode;
    private String studentRegNo;
    private boolean present;
    private LocalDate attendanceDate;
}
