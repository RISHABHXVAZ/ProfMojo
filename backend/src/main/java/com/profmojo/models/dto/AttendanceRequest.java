package com.profmojo.models.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceRequest {

    private String classCode;
    private String studentRegNo;
    private boolean present;
}
