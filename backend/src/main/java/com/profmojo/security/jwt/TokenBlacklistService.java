package com.profmojo.security.jwt;

import com.profmojo.exception.SecurityInfrastructureUnavailableException;

/**
 * Service abstraction for managing JWT revocation and denylisting in Redis.
 * Enforces a fail-closed outage policy for security-sensitive operations:
 * when the revocation store cannot be reached, methods throw
 * {@link SecurityInfrastructureUnavailableException} to prevent accepting revoked tokens
 * or silently ignoring failed revocation writes.
 */
public interface TokenBlacklistService {

    /**
     * Revokes a valid token by calculating its remaining lifetime from claims
     * and storing its JTI in the Redis denylist.
     *
     * @param token the JWT string (with or without 'Bearer ' prefix)
     * @throws SecurityInfrastructureUnavailableException if the backing store is unreachable
     */
    void revokeToken(String token);

    /**
     * Directly revokes a specific JTI for a specified TTL in seconds.
     *
     * @param jti the unique JWT identifier
     * @param remainingTtlSeconds remaining validity period in seconds
     * @throws SecurityInfrastructureUnavailableException if the backing store is unreachable
     */
    void revokeJti(String jti, long remainingTtlSeconds);

    /**
     * Checks if a JTI has been revoked.
     * Implements fail-closed policy: throws {@link SecurityInfrastructureUnavailableException}
     * if the revocation store is unreachable.
     *
     * @param jti the unique JWT identifier
     * @return true if explicitly blacklisted, false if verified active in the store
     * @throws SecurityInfrastructureUnavailableException if the backing store is unreachable
     */
    boolean isRevoked(String jti);
}
