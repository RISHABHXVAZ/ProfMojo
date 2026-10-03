package com.profmojo.ratelimit;

public interface OtpRateLimiter {

    /**
     * Checks rate limits for OTP generation across both IP and target-user dimensions.
     * Throws {@link RateLimitExceededException} if either limit is breached.
     *
     * @param clientIp     the caller's IP address
     * @param targetUserId the target user identifier or admin secret key
     */
    void checkSendOtpRateLimit(String clientIp, String targetUserId);

    /**
     * Checks whether the user has exceeded the maximum allowed failed verification attempts.
     * Throws {@link RateLimitExceededException} if max attempts are exceeded.
     *
     * @param role         the user role (e.g. ADMIN, PROFESSOR, STUDENT, STAFF)
     * @param targetUserId the target user identifier or admin secret key
     */
    void checkVerifyAttemptLimit(String role, String targetUserId);

    /**
     * Records a failed OTP verification attempt for the given user.
     *
     * @param role         the user role
     * @param targetUserId the target user identifier or admin secret key
     */
    void recordFailedVerifyAttempt(String role, String targetUserId);

    /**
     * Resets/clears failed verification attempts upon successful verification or new OTP generation.
     *
     * @param role         the user role
     * @param targetUserId the target user identifier or admin secret key
     */
    void resetVerifyAttempts(String role, String targetUserId);
}
