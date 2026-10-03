package com.profmojo.otp;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage abstraction for persisting, retrieving, and invalidating one-time passwords (OTPs).
 */
public interface OtpStore {

    /**
     * Stores an OTP for the given role and user identifier with a specified TTL.
     * Overwrites any existing OTP for that identity atomically.
     *
     * @param role       the user role (ADMIN, PROFESSOR, STUDENT, STAFF)
     * @param identifier user identifier or admin secret key
     * @param otp        the 6-digit OTP code
     * @param ttl        time-to-live duration (typically 300 seconds)
     */
    void saveOtp(String role, String identifier, String otp, Duration ttl);

    /**
     * Retrieves the stored OTP for the given role and user identifier if present.
     *
     * @param role       the user role
     * @param identifier user identifier or admin secret key
     * @return the stored OTP with its source and expiry, or empty if absent/expired
     */
    Optional<StoredOtp> findOtp(String role, String identifier);

    /**
     * Deletes the stored OTP for the given role and user identifier.
     *
     * @param role       the user role
     * @param identifier user identifier or admin secret key
     */
    void deleteOtp(String role, String identifier);
}
