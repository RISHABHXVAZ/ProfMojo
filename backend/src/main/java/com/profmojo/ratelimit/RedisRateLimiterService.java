package com.profmojo.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiterService implements RateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String ATOMIC_RATE_LIMIT_LUA = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])

            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end

            local ttl = redis.call('TTL', key)
            if ttl == -1 then
                redis.call('EXPIRE', key, window)
                ttl = window
            end

            local allowed = (current <= limit) and 1 or 0
            return { allowed, current, ttl }
            """;

    @SuppressWarnings("rawtypes")
    private final RedisScript<List> rateLimitScript = new DefaultRedisScript<>(ATOMIC_RATE_LIMIT_LUA, List.class);

    @Override
    public RateLimitResult tryAcquire(String key, int limit, int windowSeconds) {
        if (limit <= 0 || windowSeconds <= 0) {
            return RateLimitResult.allowed();
        }

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(limit),
                    String.valueOf(windowSeconds)
            );

            if (result != null && result.size() >= 3) {
                boolean allowed = result.get(0) != null && result.get(0) == 1L;
                long current = result.get(1) != null ? result.get(1) : 1L;
                long ttl = result.get(2) != null ? result.get(2) : windowSeconds;

                if (allowed) {
                    return RateLimitResult.allowed(current, ttl);
                } else {
                    return RateLimitResult.exceeded(current, ttl);
                }
            }

            return RateLimitResult.allowed();
        } catch (Exception ex) {
            // Controlled fail-open: allow request to proceed if Redis is degraded or down
            log.warn("Redis unavailable during rate limit check for key [{}]: {}. Failing open.",
                    maskKey(key), ex.getMessage());
            return RateLimitResult.allowed();
        }
    }

    @Override
    public void reset(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Redis unavailable while resetting rate limit key [{}]: {}", maskKey(key), ex.getMessage());
        }
    }

    @Override
    public boolean isLimitExceeded(String key, int limit) {
        if (key == null || key.isBlank() || limit <= 0) return false;
        try {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                return Long.parseLong(val) >= limit;
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable while checking rate limit key [{}]: {}. Failing open.", maskKey(key), ex.getMessage());
        }
        return false;
    }

    @Override
    public long getRemainingTtlSeconds(String key) {
        if (key == null || key.isBlank()) return 0;
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception ex) {
            return 0;
        }
    }

    private String maskKey(String key) {
        if (key == null) return "null";
        int lastColon = key.lastIndexOf(':');
        if (lastColon != -1 && lastColon < key.length() - 1) {
            String prefix = key.substring(0, lastColon + 1);
            String id = key.substring(lastColon + 1);
            if (id.length() > 4) {
                return prefix + id.substring(0, 2) + "***" + id.substring(id.length() - 2);
            }
            return prefix + "***";
        }
        return key;
    }
}
