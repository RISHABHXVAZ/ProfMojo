package com.profmojo.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link OtpStore}.
 * Stores short-lived OTP codes with an atomic TTL of 300 seconds.
 * Sensitive identifiers (like admin secret keys) are hashed using SHA-256 before key generation.
 */
@Component("redisOtpStore")
@RequiredArgsConstructor
@Slf4j
public class RedisOtpStore implements OtpStore {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "profmojo:otp:";

    @Override
    public void saveOtp(String role, String identifier, String otp, Duration ttl) {
        String key = buildKey(role, identifier);
        // Atomic SET with EX
        redisTemplate.opsForValue().set(key, otp, ttl);
    }

    @Override
    public Optional<StoredOtp> findOtp(String role, String identifier) {
        String key = buildKey(role, identifier);
        String otp = redisTemplate.opsForValue().get(key);
        if (otp == null || otp.isBlank()) {
            return Optional.empty();
        }

        Long expireSeconds = redisTemplate.getExpire(key);
        LocalDateTime expiry = (expireSeconds != null && expireSeconds > 0)
                ? LocalDateTime.now().plusSeconds(expireSeconds)
                : LocalDateTime.now().plusMinutes(5);

        return Optional.of(new StoredOtp(otp, role.toUpperCase(), identifier, OtpSource.REDIS, expiry));
    }

    @Override
    public void deleteOtp(String role, String identifier) {
        String key = buildKey(role, identifier);
        redisTemplate.delete(key);
    }

    /**
     * Builds the Redis key for the specified role and identifier.
     * Formats:
     * - profmojo:otp:admin:<sha256-hashed-secret>
     * - profmojo:otp:professor:<sanitized-id>
     * - profmojo:otp:student:<sanitized-id>
     * - profmojo:otp:staff:<sanitized-id>
     */
    public String buildKey(String role, String identifier) {
        String roleLower = role != null ? role.trim().toLowerCase() : "unknown";
        String sanitizedId = sanitizeIdentifier(roleLower, identifier);
        return KEY_PREFIX + roleLower + ":" + sanitizedId;
    }

    private String sanitizeIdentifier(String roleLower, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "unknown";
        }
        String trimmed = identifier.trim();
        if ("admin".equals(roleLower)) {
            return sha256Prefix(trimmed);
        }
        return trimmed.replace(':', '_').replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
