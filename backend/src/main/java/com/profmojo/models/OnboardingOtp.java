package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "onboarding_otp",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_onboarding_otp_role_user", columnNames = {"role", "user_id"})
        }
)
@IdClass(OnboardingOtpId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingOtp {

    @Id
    @Column(name = "role", nullable = false)
    private String role;

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime expiry;
}
