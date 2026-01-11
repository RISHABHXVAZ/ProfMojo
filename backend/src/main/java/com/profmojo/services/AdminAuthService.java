package com.profmojo.services;

import com.profmojo.models.Admin;
import com.profmojo.models.dto.AdminLoginRequest;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminSetPasswordRequest;

public interface AdminAuthService {
    AdminLoginResponse login(AdminLoginRequest request);
    void setPassword(AdminSetPasswordRequest request);
}
