package com.profmojo.unit;

import com.profmojo.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secretKey = "test-super-secret-jwt-key-2026-profmojo-secure-signing-token-must-be-long-enough";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("generateToken and extractClaims: Extracts subject, role, and department accurately")
    void tokenGenerationAndExtraction_Success() {
        String token = jwtUtil.generateToken("USER001", "PROFESSOR", "CSE");

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals("USER001", jwtUtil.extractUsername(token));
        assertEquals("PROFESSOR", jwtUtil.extractRole(token));
        assertEquals("CSE", jwtUtil.extractDepartment(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken: Correctly identifies expired tokens")
    void expiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // Expired 1 second ago
        String expiredToken = jwtUtil.generateToken("USER_EXP", "STUDENT", "ECE");

        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    @DisplayName("validateToken: Malformed token returns false")
    void malformedToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken("not-a-valid-jwt-token"));
    }

    @Test
    @DisplayName("jwtContainsJti: Newly generated tokens contain valid UUIDv4 JTI")
    void jwtContainsJti() {
        String token = jwtUtil.generateToken("USER002", "STUDENT");
        String jti = jwtUtil.extractJti(token);

        assertNotNull(jti);
        assertFalse(jti.isBlank());
        // Verify UUID format
        assertDoesNotThrow(() -> java.util.UUID.fromString(jti));
    }

    @Test
    @DisplayName("multiDevice: Successive token generations produce distinct JTIs")
    void tokensForSameUser_ProduceDistinctJtis() {
        String tokenA = jwtUtil.generateToken("USER002", "STUDENT");
        String tokenB = jwtUtil.generateToken("USER002", "STUDENT");

        String jtiA = jwtUtil.extractJti(tokenA);
        String jtiB = jwtUtil.extractJti(tokenB);

        assertNotNull(jtiA);
        assertNotNull(jtiB);
        assertNotEquals(jtiA, jtiB);
    }

    @Test
    @DisplayName("validateSecretKey: Rejects missing or weak signing secret")
    void secretKeyValidation_RejectsWeakSecret() {
        JwtUtil weakUtil = new JwtUtil();
        ReflectionTestUtils.setField(weakUtil, "secretKey", "too-short");
        assertThrows(IllegalStateException.class, weakUtil::validateSecretKey);

        ReflectionTestUtils.setField(weakUtil, "secretKey", null);
        assertThrows(IllegalStateException.class, weakUtil::validateSecretKey);
    }
}
