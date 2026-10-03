package com.profmojo.security.jwt;

/**
 * Service abstraction for managing JWT revocation and denylisting in Redis.
 */
public interface TokenBlacklistService {

    /**
     * Revokes a valid token by calculating its remaining lifetime from claims
     * and storing its JTI in the Redis denylist.
     *
     * @param token the JWT string (with or without 'Bearer ' prefix)
     */
    void revokeToken(String token);

    /**
     * Directly revokes a specific JTI for a specified TTL in seconds.
     *
     * @param jti the unique JWT identifier
     * @param remainingTtlSeconds remaining validity period in seconds
     */
    void revokeJti(String jti, long remainingTtlSeconds);

    /**
     * Checks if a JTI has been revoked.
     * Implements controlled fail-open: returns false and logs a WARN if Redis is unreachable.
     *
     * @param jti the unique JWT identifier
     * @return true if explicitly blacklisted, false otherwise
     */
    boolean isRevoked(String jti);
}
