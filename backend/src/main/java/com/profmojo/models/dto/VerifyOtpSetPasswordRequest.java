package com.profmojo.models.dto;

import lombok.Data;

@Data
public class VerifyOtpSetPasswordRequest {
    private String userId;
    private String otp;
    private String password;
}
