package com.profmojo.unit;

import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.security.jwt.RedisTokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Unit Tests")
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private com.profmojo.metrics.AppMetricsService appMetricsService;

    private RedisTokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new RedisTokenBlacklistService(stringRedisTemplate, jwtUtil, appMetricsService);
    }

    @Test
    @DisplayName("revokeToken: Successfully extracts JTI and calculates TTL for Redis storage")
    void revokeToken_CalculatesTtlAndStoresInRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = "valid.jwt.token";
        String jti = "test-jti-12345";
        Date futureExp = new Date(System.currentTimeMillis() + 60_000); // 60 seconds remaining

        Claims claims = Jwts.claims();
        claims.setId(jti);
        claims.setExpiration(futureExp);

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        blacklistService.revokeToken(token);

        verify(valueOperations).set(
                eq("profmojo:token:blacklist:" + jti),
                eq("1"),
                longThat(ttl -> ttl > 0 && ttl <= 60),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("revokeToken: Already expired token does not create a Redis blacklist entry")
    void revokeToken_AlreadyExpired_DoesNotCreateKey() {
        String token = "expired.jwt.token";
        String jti = "expired-jti-123";
        Date pastExp = new Date(System.currentTimeMillis() - 5_000); // Expired 5 seconds ago

        Claims claims = Jwts.claims();
        claims.setId(jti);
        claims.setExpiration(pastExp);

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        blacklistService.revokeToken(token);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("isRevoked: Returns true when key exists in Redis")
    void isRevoked_TrueWhenKeyExists() {
        String jti = "revoked-jti";
        when(stringRedisTemplate.hasKey("profmojo:token:blacklist:" + jti)).thenReturn(true);

        assertTrue(blacklistService.isRevoked(jti));
    }

    @Test
    @DisplayName("isRevoked: Returns false when key does not exist in Redis")
    void isRevoked_FalseWhenKeyDoesNotExist() {
        String jti = "active-jti";
        when(stringRedisTemplate.hasKey("profmojo:token:blacklist:" + jti)).thenReturn(false);

        assertFalse(blacklistService.isRevoked(jti));
    }

    @Test
    @DisplayName("redisDown_FailOpen: Returns false when Redis throws connection exception during check")
    void redisDown_FailOpen_ReturnsFalse() {
        String jti = "any-jti";
        when(stringRedisTemplate.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        // Must fail-open and return false
        assertFalse(blacklistService.isRevoked(jti));
    }

    @Test
    @DisplayName("redisDown_FailOpen: Revoke handles Redis exception without throwing")
    void redisDown_Revoke_HandlesExceptionGracefully() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Connection refused"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertDoesNotThrow(() -> blacklistService.revokeJti("test-jti", 100));
    }
}
