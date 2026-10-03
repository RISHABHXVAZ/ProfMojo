package com.profmojo.metrics;

import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized service for publishing safe, low-cardinality application metrics.
 * <p>
 * Enforces strict tag restrictions: NO PII (email, user ID, student ID, IP)
 * and NO high-cardinality values (JWT, tokens, secret keys).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppMetricsService {

    private final MeterRegistry meterRegistry;
    private final AmenityRequestRepository amenityRequestRepository;

    private final AtomicLong queuedAmenityRequestsCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        Gauge.builder("profmojo.amenity.requests.queued", queuedAmenityRequestsCount, AtomicLong::get)
                .description("Number of queued amenity requests waiting for staff assignment")
                .register(meterRegistry);

        refreshQueueDepth();
    }

    /**
     * Refreshes the queued amenity requests count from PostgreSQL.
     * Scheduled periodically to avoid polling the database on every Prometheus scrape.
     */
    @Scheduled(fixedRate = 15000)
    public void refreshQueueDepth() {
        try {
            long count = amenityRequestRepository.countByStatus(RequestStatus.QUEUED);
            queuedAmenityRequestsCount.set(count);
        } catch (Exception e) {
            log.warn("Failed to refresh amenity queue depth gauge: {}", e.getMessage());
        }
    }

    public void incrementOtpRequest(String role, String status) {
        meterRegistry.counter("profmojo.auth.otp.requests",
                "role", normalize(role),
                "status", normalize(status)
        ).increment();
    }

    public void incrementOtpVerification(String role, String status) {
        meterRegistry.counter("profmojo.auth.otp.verifications",
                "role", normalize(role),
                "status", normalize(status)
        ).increment();
    }

    public void incrementTokenRevoked(String role) {
        meterRegistry.counter("profmojo.auth.tokens.revoked",
                "role", normalize(role)
        ).increment();
    }

    public void incrementBlacklistHit(String role) {
        meterRegistry.counter("profmojo.auth.tokens.blacklist_hits",
                "role", normalize(role)
        ).increment();
    }

    public void incrementSlaBreaches(String type, long count) {
        if (count > 0) {
            meterRegistry.counter("profmojo.amenity.sla.breaches",
                    "type", normalize(type)
            ).increment(count);
        }
    }

    public void incrementEmailDelivery(String status) {
        meterRegistry.counter("profmojo.email.delivery",
                "status", normalize(status)
        ).increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }
}
