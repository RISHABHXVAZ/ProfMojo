package com.profmojo.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpRateLimiterImpl implements OtpRateLimiter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    private static final String KEY_PREFIX_IP = "profmojo:ratelimit:send-otp:ip:";
    private static final String KEY_PREFIX_USER = "profmojo:ratelimit:send-otp:user:";

    @Override
    public void checkSendOtpRateLimit(String clientIp, String targetUserId) {
        // Dimension 1: IP-based rate limiting (e.g. 3 requests / 60 seconds)
        RateLimitProperties.Dimension ipConfig = properties.getSendOtp().getIp();
        String ipKey = KEY_PREFIX_IP + sanitizeIp(clientIp);
        RateLimitResult ipResult = rateLimiter.tryAcquire(ipKey, ipConfig.getLimit(), ipConfig.getWindowSeconds());

        if (!ipResult.isAllowed()) {
            log.warn("Rate limit exceeded for IP [{}]: {} requests in {}s window",
                    sanitizeIp(clientIp), ipResult.getCurrentCount(), ipConfig.getWindowSeconds());
            throw new RateLimitExceededException(
                    "Too many OTP requests from your IP. Please try again in " + ipResult.getRetryAfterSeconds() + " seconds.",
                    ipResult.getRetryAfterSeconds()
            );
        }

        // Dimension 2: Target-user rate limiting (e.g. 2 requests / 300 seconds)
        RateLimitProperties.Dimension userConfig = properties.getSendOtp().getUser();
        String userKey = KEY_PREFIX_USER + sanitizeUserId(targetUserId);
        RateLimitResult userResult = rateLimiter.tryAcquire(userKey, userConfig.getLimit(), userConfig.getWindowSeconds());

        if (!userResult.isAllowed()) {
            log.warn("Rate limit exceeded for user identifier [{}]: {} requests in {}s window",
                    maskUser(targetUserId), userResult.getCurrentCount(), userConfig.getWindowSeconds());
            throw new RateLimitExceededException(
                    "Too many OTP requests for this account. Please wait " + userResult.getRetryAfterSeconds() + " seconds before requesting another.",
                    userResult.getRetryAfterSeconds()
            );
        }
    }

    private static final String KEY_PREFIX_VERIFY = "profmojo:ratelimit:verify-otp:";
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int VERIFY_WINDOW_SECONDS = 300;

    @Override
    public void checkVerifyAttemptLimit(String role, String targetUserId) {
        String verifyKey = KEY_PREFIX_VERIFY + role.toLowerCase() + ":" + sanitizeUserId(targetUserId);
        if (rateLimiter.isLimitExceeded(verifyKey, MAX_VERIFY_ATTEMPTS)) {
            long remaining = rateLimiter.getRemainingTtlSeconds(verifyKey);
            log.warn("Exceeded max verification attempts for role [{}] user [{}]", role, maskUser(targetUserId));
            throw new RateLimitExceededException(
                    "Too many failed OTP verification attempts. Please wait " + (remaining > 0 ? remaining : VERIFY_WINDOW_SECONDS) + " seconds or request a new OTP.",
                    remaining > 0 ? remaining : VERIFY_WINDOW_SECONDS
            );
        }
    }

    @Override
    public void recordFailedVerifyAttempt(String role, String targetUserId) {
        String verifyKey = KEY_PREFIX_VERIFY + role.toLowerCase() + ":" + sanitizeUserId(targetUserId);
        RateLimitResult result = rateLimiter.tryAcquire(verifyKey, MAX_VERIFY_ATTEMPTS, VERIFY_WINDOW_SECONDS);
        if (!result.isAllowed()) {
            log.warn("User breached max verification attempts ({}) for role [{}] user [{}]",
                    result.getCurrentCount(), role, maskUser(targetUserId));
            throw new RateLimitExceededException(
                    "Too many failed OTP verification attempts. This OTP has been invalidated. Please wait " + result.getRetryAfterSeconds() + " seconds or request a new OTP.",
                    result.getRetryAfterSeconds()
            );
        }
    }

    @Override
    public void resetVerifyAttempts(String role, String targetUserId) {
        String verifyKey = KEY_PREFIX_VERIFY + role.toLowerCase() + ":" + sanitizeUserId(targetUserId);
        rateLimiter.reset(verifyKey);
    }

    private String sanitizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        // Normalize colons in IPv6 (e.g. 0:0:0:0:0:0:0:1) to underscores so key namespaces remain clean
        return ip.trim().replace(':', '_').replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private String sanitizeUserId(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            return "unknown";
        }
        String trimmed = targetUserId.trim();
        // If the identifier appears to be an admin secret key (only digits or secret key pattern), hash it to avoid leaking raw secret keys in Redis keys
        if (trimmed.length() >= 8 && trimmed.matches("^\\d+$")) {
            return "admin_" + sha256Prefix(trimmed);
        }
        // Standard usernames/IDs (e.g., PROF01, 21BCE1001, STAFF01)
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

    private String maskUser(String user) {
        if (user == null || user.isBlank()) return "unknown";
        if (user.length() <= 4) return "****";
        return user.substring(0, 2) + "***" + user.substring(user.length() - 2);
    }
}
