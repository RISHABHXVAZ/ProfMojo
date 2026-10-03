package com.profmojo.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final long currentCount;
    private final long retryAfterSeconds;

    public static RateLimitResult allowed() {
        return new RateLimitResult(true, 1L, 0L);
    }

    public static RateLimitResult allowed(long currentCount, long remainingTtlSeconds) {
        return new RateLimitResult(true, currentCount, 0L);
    }

    public static RateLimitResult exceeded(long currentCount, long retryAfterSeconds) {
        return new RateLimitResult(false, currentCount, Math.max(retryAfterSeconds, 1L));
    }
}
