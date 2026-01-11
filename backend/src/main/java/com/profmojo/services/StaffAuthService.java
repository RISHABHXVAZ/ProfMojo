package com.profmojo.services;

import com.profmojo.models.dto.StaffLoginRequest;
import com.profmojo.models.dto.StaffLoginResponse;
import com.profmojo.models.dto.StaffSetPasswordRequest;

public interface StaffAuthService {
    void setPassword(StaffSetPasswordRequest request);

    StaffLoginResponse login(StaffLoginRequest request);
}
