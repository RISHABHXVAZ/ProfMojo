package com.profmojo.models.dto;

import lombok.Data;

@Data
public class AddStaffRequest {
    private String staffId;
    private String name;
    private String department;
    private String email;
    private String contactNo;
}

