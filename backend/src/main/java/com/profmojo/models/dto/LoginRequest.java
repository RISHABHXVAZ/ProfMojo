package com.profmojo.models.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String profId;
    private String password;
}
