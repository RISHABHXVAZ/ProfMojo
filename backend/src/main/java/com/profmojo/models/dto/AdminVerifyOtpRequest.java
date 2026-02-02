package com.profmojo.models.dto;

import lombok.Data;

@Data
public class AdminVerifyOtpRequest {
    private String secretKey;
    private String otp;
}

