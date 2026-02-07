package com.profmojo.models.dto;

import lombok.Data;

@Data
public class ReportStaffRequest {
    private Long requestId;
    private String staffId;
    private String staffName;
    private String department;
    private String reason;
}