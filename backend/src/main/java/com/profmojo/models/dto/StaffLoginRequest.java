package com.profmojo.models.dto;

import lombok.Data;

@Data
public class StaffLoginRequest {
    private String staffId;
    private String password;
}

