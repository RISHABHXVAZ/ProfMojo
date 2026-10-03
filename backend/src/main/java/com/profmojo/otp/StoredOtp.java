package com.profmojo.otp;

import java.time.LocalDateTime;

/**
 * Immutable representation of a stored OTP and its source metadata.
 */
public record StoredOtp(
        String otp,
        String role,
        String identifier,
        OtpSource source,
        LocalDateTime expiry
) {
    public boolean isExpired() {
        return expiry != null && expiry.isBefore(LocalDateTime.now());
    }
}
