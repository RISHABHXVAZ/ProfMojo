package com.profmojo.models.dto;

import lombok.Data;

@Data
public class StaffSetPasswordRequest {
    private String staffId;
    private String password;
}
