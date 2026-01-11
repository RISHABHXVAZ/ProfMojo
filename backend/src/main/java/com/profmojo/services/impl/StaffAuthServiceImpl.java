package com.profmojo.services.impl;

import com.profmojo.models.Staff;
import com.profmojo.models.dto.StaffLoginRequest;
import com.profmojo.models.dto.StaffLoginResponse;
import com.profmojo.models.dto.StaffSetPasswordRequest;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.StaffAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffAuthServiceImpl implements StaffAuthService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void setPassword(StaffSetPasswordRequest request) {

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Invalid Staff ID"));

        if (staff.getPassword() != null) {
            throw new RuntimeException("Password already set");
        }

        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staffRepository.save(staff);
    }

    @Override
    public StaffLoginResponse login(StaffLoginRequest request) {

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Invalid Staff ID"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                staff.getStaffId(),
                staff.getRole()   // "STAFF"
        );

        return new StaffLoginResponse(
                token,
                staff.getStaffId(),
                staff.getDepartment()
        );
    }
}
