package com.profmojo.security.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    public static final String BLACKLIST_KEY_PREFIX = "profmojo:token:blacklist:";
    public static final String REVOKED_VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        try {
            Claims claims = jwtUtil.extractAllClaims(cleanToken);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();
            String role = claims.get("role", String.class);

            if (jti == null || jti.isBlank()) {
                log.warn("Cannot revoke token: missing JTI claim");
                return;
            }

            if (expiration == null) {
                log.warn("Cannot revoke token: missing expiration claim");
                return;
            }

            long remainingSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            if (remainingSeconds <= 0) {
                // Token has already expired naturally; no Redis blacklist entry needed
                return;
            }

            revokeJti(jti, remainingSeconds);
            appMetricsService.incrementTokenRevoked(role);

        } catch (Exception e) {
            log.warn("Error parsing token for revocation: {}", e.getMessage());
        }
    }

    @Override
    public void revokeJti(String jti, long remainingTtlSeconds) {
        if (jti == null || jti.isBlank() || remainingTtlSeconds <= 0) {
            return;
        }

        String redisKey = BLACKLIST_KEY_PREFIX + jti;
        try {
            stringRedisTemplate.opsForValue().set(redisKey, REVOKED_VALUE, remainingTtlSeconds, TimeUnit.SECONDS);
            log.info("Token JTI revoked successfully in Redis with TTL {}s", remainingTtlSeconds);
        } catch (Exception e) {
            log.warn("Redis unavailable while attempting to revoke token; proceeding. Reason: {}", e.getMessage());
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        String redisKey = BLACKLIST_KEY_PREFIX + jti;
        try {
            Boolean exists = stringRedisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                appMetricsService.incrementBlacklistHit("jwt");
                return true;
            }
            return false;
        } catch (Exception e) {
            // Controlled FAIL-OPEN: Log safe warning without leaking token or payload, and allow validated token
            log.warn("Redis unavailable during JWT revocation check; allowing validated token to continue.");
            return false;
        }
    }
}
