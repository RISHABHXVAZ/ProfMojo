package com.profmojo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingOtp {

    @Id
    private String userId;

    private String role;

    private String otp;

    private LocalDateTime expiry;
}
