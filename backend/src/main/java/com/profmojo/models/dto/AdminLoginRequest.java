package com.profmojo.models.dto;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String adminId;
    private String password;
}
