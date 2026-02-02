package com.profmojo.services;

import com.profmojo.models.Admin;
import com.profmojo.models.dto.AdminLoginRequest;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminSetPasswordRequest;
import com.profmojo.models.dto.AdminVerifyOtpRequest;

public interface AdminAuthService {
    void sendOtp(String secretKey);

    AdminLoginResponse verifyOtpAndLogin(AdminVerifyOtpRequest request);
}
