package com.profmojo.ratelimit;

import com.profmojo.integration.BasePostgresContainerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("Redis Rate Limiter Integration Tests (Testcontainers Redis)")
class RedisRateLimiterIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private RedisRateLimiterService rateLimiter;

    @Autowired
    private OtpRateLimiter otpRateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedis() {
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }
    }

    @Test
    @DisplayName("1. First request is allowed with count = 1")
    void firstRequest_isAllowed() {
        String key = "profmojo:ratelimit:test:first:user1";
        RateLimitResult result = rateLimiter.tryAcquire(key, 3, 60);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCurrentCount()).isEqualTo(1L);
        assertThat(result.getRetryAfterSeconds()).isEqualTo(0L);
    }

    @Test
    @DisplayName("2. Requests below the limit are allowed")
    void requestsBelowLimit_areAllowed() {
        String key = "profmojo:ratelimit:test:below:user2";

        RateLimitResult r1 = rateLimiter.tryAcquire(key, 3, 60);
        RateLimitResult r2 = rateLimiter.tryAcquire(key, 3, 60);
        RateLimitResult r3 = rateLimiter.tryAcquire(key, 3, 60);

        assertThat(r1.isAllowed()).isTrue();
        assertThat(r1.getCurrentCount()).isEqualTo(1L);

        assertThat(r2.isAllowed()).isTrue();
        assertThat(r2.getCurrentCount()).isEqualTo(2L);

        assertThat(r3.isAllowed()).isTrue();
        assertThat(r3.getCurrentCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("3. Request exceeding the limit returns rate-limited state with Retry-After > 0")
    void requestExceedingLimit_isBlocked() {
        String key = "profmojo:ratelimit:test:exceed:user3";

        // Consume quota (limit = 2)
        rateLimiter.tryAcquire(key, 2, 60);
        rateLimiter.tryAcquire(key, 2, 60);

        // 3rd attempt exceeds limit
        RateLimitResult r3 = rateLimiter.tryAcquire(key, 2, 60);

        assertThat(r3.isAllowed()).isFalse();
        assertThat(r3.getCurrentCount()).isEqualTo(3L);
        assertThat(r3.getRetryAfterSeconds()).isGreaterThan(0L).isLessThanOrEqualTo(60L);
    }

    @Test
    @DisplayName("4 & 5. Counter expires after TTL and new window allows requests again")
    void counterExpiresAfterTtl_allowsRequestsInNewWindow() throws InterruptedException {
        String key = "profmojo:ratelimit:test:ttl:user4";
        int windowSeconds = 2; // short window for testing

        // Consume limit = 1
        RateLimitResult r1 = rateLimiter.tryAcquire(key, 1, windowSeconds);
        assertThat(r1.isAllowed()).isTrue();

        // Immediate 2nd attempt blocked
        RateLimitResult r2 = rateLimiter.tryAcquire(key, 1, windowSeconds);
        assertThat(r2.isAllowed()).isFalse();

        // Wait for TTL expiry
        Thread.sleep(2200);

        // In new window, request is allowed again
        RateLimitResult r3 = rateLimiter.tryAcquire(key, 1, windowSeconds);
        assertThat(r3.isAllowed()).isTrue();
        assertThat(r3.getCurrentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("6. Concurrent requests cannot bypass the limit (race-condition safety)")
    void concurrentRequests_cannotBypassLimit() throws InterruptedException {
        String key = "profmojo:ratelimit:test:concurrency:user5";
        int limit = 5;
        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startGate.await();
                    RateLimitResult res = rateLimiter.tryAcquire(key, limit, 60);
                    if (res.isAllowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        blockedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore for test
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startGate.countDown(); // fire all threads simultaneously
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(allowedCount.get())
                .as("Exactly 'limit' (5) requests must be allowed under concurrent load")
                .isEqualTo(limit);
        assertThat(blockedCount.get())
                .as("All excess concurrent requests must be rejected")
                .isEqualTo(threadCount - limit);
    }

    @Test
    @DisplayName("7. Redis failure triggers fail-open behavior (controlled graceful degradation)")
    void redisFailure_triggersFailOpenBehavior() {
        // Service with simulated failing RedisTemplate
        RedisRateLimiterService failingService = new RedisRateLimiterService(null);

        // When Redis client is null or fails, tryAcquire must fail-open and return allowed=true
        RateLimitResult result = failingService.tryAcquire("profmojo:ratelimit:test:failopen", 3, 60);

        assertThat(result.isAllowed())
                .as("Must fail open when Redis encounters an exception")
                .isTrue();
    }

    @Test
    @DisplayName("8. OtpRateLimiter enforces both IP and Target-User dimensions")
    void otpRateLimiter_enforcesBothDimensions() {
        String ip1 = "192.168.1.100";
        String user1 = "PROF101";

        // IP limit is 3, User limit is 2 (from test configuration)
        // Request 1: OK
        otpRateLimiter.checkSendOtpRateLimit(ip1, user1);

        // Request 2: OK
        otpRateLimiter.checkSendOtpRateLimit(ip1, user1);

        // Request 3: User limit (2) exceeded!
        RateLimitExceededException userEx = assertThrows(RateLimitExceededException.class, () ->
                otpRateLimiter.checkSendOtpRateLimit(ip1, user1)
        );
        assertThat(userEx.getMessage()).contains("Too many OTP requests for this account");
        assertThat(userEx.getRetryAfterSeconds()).isGreaterThan(0L);

        // Request from different user on same IP:
        // Note: Requests 1, 2, and 3 already incremented IP counter to 3 (the IP limit).
        // Therefore, any 4th request from ip1 immediately breaches the IP limit!
        String user2 = "PROF102";
        RateLimitExceededException ipEx = assertThrows(RateLimitExceededException.class, () ->
                otpRateLimiter.checkSendOtpRateLimit(ip1, user2)
        );
        assertThat(ipEx.getMessage()).contains("Too many OTP requests from your IP");
        assertThat(ipEx.getRetryAfterSeconds()).isGreaterThan(0L);

        // However, from a different IP (ip2), user2 can still request!
        String ip2 = "192.168.1.101";
        // User2 on IP2 has count 1 on both -> allowed!
        otpRateLimiter.checkSendOtpRateLimit(ip2, user2);
    }
}
