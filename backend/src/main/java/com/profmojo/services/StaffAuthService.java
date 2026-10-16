package com.profmojo.services;

import com.profmojo.models.dto.StaffLoginRequest;
import com.profmojo.models.dto.StaffLoginResponse;

public interface StaffAuthService {
    StaffLoginResponse login(StaffLoginRequest request);
    void logout(String authHeader);
}
