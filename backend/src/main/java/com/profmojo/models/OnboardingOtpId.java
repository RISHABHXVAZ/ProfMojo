package com.profmojo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite identifier for {@link OnboardingOtp} composed of (role, userId).
 * Eliminates cross-role collisions where entities across different roles share identical identifiers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingOtpId implements Serializable {
    private String role;
    private String userId;
}
