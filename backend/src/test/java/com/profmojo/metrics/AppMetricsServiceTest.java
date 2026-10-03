package com.profmojo.metrics;

import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppMetricsService Unit Tests")
class AppMetricsServiceTest {

    private MeterRegistry meterRegistry;

    @Mock
    private AmenityRequestRepository amenityRequestRepository;

    private AppMetricsService appMetricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        when(amenityRequestRepository.countByStatus(RequestStatus.QUEUED)).thenReturn(5L);
        appMetricsService = new AppMetricsService(meterRegistry, amenityRequestRepository);
        appMetricsService.init();
    }

    @Test
    @DisplayName("Amenity queue depth gauge is registered and reflects current count")
    void amenityQueueDepthGauge_RegistersAndReflectsValue() {
        Gauge gauge = meterRegistry.find("profmojo.amenity.requests.queued").gauge();
        assertNotNull(gauge, "Queue depth gauge should be registered");
        assertEquals(5.0, gauge.value(), 0.001);

        // Update count via refresh
        when(amenityRequestRepository.countByStatus(RequestStatus.QUEUED)).thenReturn(8L);
        appMetricsService.refreshQueueDepth();
        assertEquals(8.0, gauge.value(), 0.001);
    }

    @Test
    @DisplayName("OTP request metrics increment with correct tags and normalization")
    void otpRequestMetrics_IncrementWithNormalizedTags() {
        appMetricsService.incrementOtpRequest("ADMIN", "SUCCESS");
        appMetricsService.incrementOtpRequest("ADMIN", "RATE_LIMITED");
        appMetricsService.incrementOtpRequest("ADMIN", "FAILED");

        Counter successCounter = meterRegistry.find("profmojo.auth.otp.requests")
                .tag("role", "admin")
                .tag("status", "success")
                .counter();
        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());

        Counter rateLimitedCounter = meterRegistry.find("profmojo.auth.otp.requests")
                .tag("role", "admin")
                .tag("status", "rate_limited")
                .counter();
        assertNotNull(rateLimitedCounter);
        assertEquals(1.0, rateLimitedCounter.count());

        Counter failedCounter = meterRegistry.find("profmojo.auth.otp.requests")
                .tag("role", "admin")
                .tag("status", "failed")
                .counter();
        assertNotNull(failedCounter);
        assertEquals(1.0, failedCounter.count());
    }

    @Test
    @DisplayName("OTP verification metrics increment with correct tags")
    void otpVerificationMetrics_IncrementCorrectly() {
        appMetricsService.incrementOtpVerification("onboarding", "success");
        appMetricsService.incrementOtpVerification("onboarding", "invalid");
        appMetricsService.incrementOtpVerification("onboarding", "expired");

        assertEquals(1.0, meterRegistry.find("profmojo.auth.otp.verifications")
                .tag("role", "onboarding")
                .tag("status", "success")
                .counter().count());

        assertEquals(1.0, meterRegistry.find("profmojo.auth.otp.verifications")
                .tag("role", "onboarding")
                .tag("status", "invalid")
                .counter().count());

        assertEquals(1.0, meterRegistry.find("profmojo.auth.otp.verifications")
                .tag("role", "onboarding")
                .tag("status", "expired")
                .counter().count());
    }

    @Test
    @DisplayName("JWT token revocation and blacklist hit metrics increment correctly")
    void jwtRevocationMetrics_IncrementCorrectly() {
        appMetricsService.incrementTokenRevoked("STUDENT");
        appMetricsService.incrementTokenRevoked("PROFESSOR");
        appMetricsService.incrementBlacklistHit("jwt");

        Counter studentRevoked = meterRegistry.find("profmojo.auth.tokens.revoked")
                .tag("role", "student")
                .counter();
        assertNotNull(studentRevoked);
        assertEquals(1.0, studentRevoked.count());

        Counter hitCounter = meterRegistry.find("profmojo.auth.tokens.blacklist_hits")
                .tag("role", "jwt")
                .counter();
        assertNotNull(hitCounter);
        assertEquals(1.0, hitCounter.count());
    }

    @Test
    @DisplayName("SLA breach metrics increment with valid type and positive count")
    void slaBreachMetrics_IncrementCorrectly() {
        appMetricsService.incrementSlaBreaches("delivery", 3);
        appMetricsService.incrementSlaBreaches("delivery", 0); // Should be ignored

        Counter breachCounter = meterRegistry.find("profmojo.amenity.sla.breaches")
                .tag("type", "delivery")
                .counter();
        assertNotNull(breachCounter);
        assertEquals(3.0, breachCounter.count());
    }

    @Test
    @DisplayName("Email delivery metrics increment with correct status tags")
    void emailDeliveryMetrics_IncrementCorrectly() {
        appMetricsService.incrementEmailDelivery("SUCCESS");
        appMetricsService.incrementEmailDelivery("failed");

        Counter successCounter = meterRegistry.find("profmojo.email.delivery")
                .tag("status", "success")
                .counter();
        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());

        Counter failedCounter = meterRegistry.find("profmojo.email.delivery")
                .tag("status", "failed")
                .counter();
        assertNotNull(failedCounter);
        assertEquals(1.0, failedCounter.count());
    }

    @Test
    @DisplayName("Strict low-cardinality enforcement: No PII or forbidden tag keys exist")
    void strictLowCardinality_NoPiiTagsExist() {
        appMetricsService.incrementOtpRequest("admin", "success");
        appMetricsService.incrementOtpVerification("admin", "success");
        appMetricsService.incrementTokenRevoked("admin");

        Set<String> allowedTagKeys = Set.of("role", "status", "type");

        meterRegistry.getMeters().forEach(meter -> {
            for (Tag tag : meter.getId().getTags()) {
                assertTrue(allowedTagKeys.contains(tag.getKey()),
                        "Metric " + meter.getId().getName() + " contains unexpected tag: " + tag.getKey());
                assertFalse(tag.getValue().contains("@"), "Tag value must not contain email or PII");
            }
        });
    }
}
