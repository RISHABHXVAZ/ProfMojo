package com.profmojo.ratelimit;

public interface RateLimiter {

    /**
     * Attempts to acquire an execution permit for the given key.
     *
     * @param key           the unique Redis key identifying the rate limit bucket
     * @param limit         the maximum number of requests allowed in the window
     * @param windowSeconds the time window duration in seconds
     * @return the outcome containing allowed status, current counter, and remaining retry window
     */
    RateLimitResult tryAcquire(String key, int limit, int windowSeconds);

    /**
     * Resets/clears the rate limit counter for the given key.
     *
     * @param key the unique Redis key to reset
     */
    void reset(String key);

    /**
     * Checks if the counter for the given key is currently at or above the limit without incrementing.
     *
     * @param key   the unique Redis key
     * @param limit the limit threshold
     * @return true if currently exceeded, false otherwise
     */
    boolean isLimitExceeded(String key, int limit);

    /**
     * Gets the remaining TTL in seconds for the given key.
     *
     * @param key the unique Redis key
     * @return remaining seconds or 0 if expired/absent
     */
    long getRemainingTtlSeconds(String key);
}
