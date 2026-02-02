package com.profmojo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class OnboardingOtp {

    @Id
    private String userId;

    private String role;

    private String otp;

    private LocalDateTime expiry;
}
