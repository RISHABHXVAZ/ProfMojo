package com.profmojo.scheduler;

import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmenitySlaMonitor {

    private final AmenityRequestRepository repo;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorSla() {
        try {
            int breached = repo.markBreachedDeliverySlaRequests(RequestStatus.ASSIGNED, LocalDateTime.now());
            if (breached > 0) {
                appMetricsService.incrementSlaBreaches("delivery", breached);
                log.warn("SLA Monitor: Marked {} overdue amenity requests as breached", breached);
            }
        } catch (Exception e) {
            log.error("Error occurred while checking SLA breaches: {}", e.getMessage(), e);
        }
    }
}
