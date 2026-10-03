package com.profmojo.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Resolves the client IP address for rate limiting and auditing.
 * <p>
 * <b>Security &amp; Trusted Proxy Architecture:</b>
 * In deployments where the application is directly exposed (such as local Docker
 * Compose where Tomcat listens directly on port 8080), client-supplied HTTP
 * headers like {@code X-Forwarded-For} or {@code X-Real-IP} MUST NOT be trusted
 * because external clients can arbitrarily forge them to bypass IP rate limits.
 * <p>
 * When {@code profmojo.ratelimit.trust-proxy-headers} is {@code false} (the default),
 * this resolver strictly uses {@link HttpServletRequest#getRemoteAddr()}, reflecting
 * the actual TCP connection peer (e.g. Docker bridge gateway or client socket).
 * <p>
 * When deployed behind a verified, trusted reverse proxy (e.g. Nginx, AWS ALB,
 * Cloudflare) configured to strip untrusted headers, set {@code TRUST_PROXY_HEADERS=true}
 * to parse the client IP from {@code X-Forwarded-For} (first non-blank entry) or
 * {@code X-Real-IP}.
 */
@Component
public class ClientIpResolver {

    private static volatile boolean trustProxyHeaders = false;

    private static final String LOOPBACK_V4 = "127.0.0.1";
    private static final String LOOPBACK_V6 = "0:0:0:0:0:0:0:1";
    private static final String LOOPBACK_V6_SHORT = "::1";

    // Basic sanitation pattern for IPv4/IPv6 characters
    private static final Pattern VALID_IP_PATTERN = Pattern.compile("^[0-9a-fA-F.:%]{3,45}$");

    public ClientIpResolver(
            @Value("${profmojo.ratelimit.trust-proxy-headers:false}") boolean trustProxy
    ) {
        setTrustProxyHeaders(trustProxy);
    }

    public static void setTrustProxyHeaders(boolean trust) {
        trustProxyHeaders = trust;
    }

    public static boolean isTrustProxyHeaders() {
        return trustProxyHeaders;
    }

    /**
     * Resolves the client IP from the HTTP request using the configured proxy trust policy.
     *
     * @param request the current HTTP servlet request
     * @return the resolved and sanitized client IP string, never null
     */
    public static String resolve(HttpServletRequest request) {
        return resolve(request, trustProxyHeaders);
    }

    /**
     * Resolves the client IP explicitly specifying whether to trust proxy headers.
     *
     * @param request the current HTTP servlet request
     * @param trustProxy whether to inspect and trust forwarded headers
     * @return the resolved and sanitized client IP string
     */
    public static String resolve(HttpServletRequest request, boolean trustProxy) {
        if (request == null) {
            return LOOPBACK_V4;
        }

        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String firstIp = forwarded.split(",")[0].trim();
                if (isValidIp(firstIp)) {
                    return normalizeIp(firstIp);
                }
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                String trimmed = realIp.trim();
                if (isValidIp(trimmed)) {
                    return normalizeIp(trimmed);
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isBlank()) {
            String trimmed = remoteAddr.trim();
            if (isValidIp(trimmed)) {
                return normalizeIp(trimmed);
            }
        }

        return LOOPBACK_V4;
    }

    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return VALID_IP_PATTERN.matcher(ip).matches();
    }

    private static String normalizeIp(String ip) {
        if (LOOPBACK_V6.equals(ip) || LOOPBACK_V6_SHORT.equals(ip)) {
            return LOOPBACK_V4;
        }
        return ip;
    }
}
