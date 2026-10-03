package com.profmojo.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    @DisplayName("Untrusted proxy: ignores attacker-controlled X-Forwarded-For and returns remoteAddr")
    void untrustedProxy_ignoresAttackerForwardedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195");
        request.addHeader("X-Real-IP", "198.51.100.1");

        // Explicitly untrusted (default)
        String resolved = ClientIpResolver.resolve(request, false);

        assertEquals("172.18.0.1", resolved, "Must ignore forwarded headers when proxy is not trusted");
    }

    @Test
    @DisplayName("Trusted proxy: extracts first client IP from X-Forwarded-For")
    void trustedProxy_honorsXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.2, 10.0.0.3");

        String resolved = ClientIpResolver.resolve(request, true);

        assertEquals("203.0.113.195", resolved);
    }

    @Test
    @DisplayName("Trusted proxy: falls back to X-Real-IP if X-Forwarded-For is missing")
    void trustedProxy_fallsBackToXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.50");

        String resolved = ClientIpResolver.resolve(request, true);

        assertEquals("198.51.100.50", resolved);
    }

    @Test
    @DisplayName("Trusted proxy: falls back to remoteAddr if proxy headers are missing")
    void trustedProxy_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        String resolved = ClientIpResolver.resolve(request, true);

        assertEquals("192.168.1.100", resolved);
    }

    @Test
    @DisplayName("IPv6 loopback is normalized to 127.0.0.1")
    void normalizesIpv6Loopback() {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("::1");
        assertEquals("127.0.0.1", ClientIpResolver.resolve(request1, false));

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("0:0:0:0:0:0:0:1");
        assertEquals("127.0.0.1", ClientIpResolver.resolve(request2, false));
    }

    @Test
    @DisplayName("Null or blank request returns 127.0.0.1")
    void nullOrBlankRequest_returnsDefault() {
        assertEquals("127.0.0.1", ClientIpResolver.resolve(null, false));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("");
        assertEquals("127.0.0.1", ClientIpResolver.resolve(request, false));
    }

    @Test
    @DisplayName("Malformed header value rejected and safely falls back to remote address")
    void malformedHeader_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.1");
        request.addHeader("X-Forwarded-For", "malicious-header-value<script>");

        String resolved = ClientIpResolver.resolve(request, true);

        assertEquals("172.18.0.1", resolved);
    }
}
