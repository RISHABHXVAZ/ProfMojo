package com.profmojo.models.dto;

import lombok.Data;

@Data
public class AdminSetPasswordRequest {
    private String adminId;
    private String password;
}
