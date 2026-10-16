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
            LocalDateTime now = LocalDateTime.now();

            // 1️⃣ Monitor Delivery SLA for ASSIGNED requests
            int deliveryBreached = repo.markBreachedDeliverySlaRequests(RequestStatus.ASSIGNED, now);
            if (deliveryBreached > 0) {
                appMetricsService.incrementSlaBreaches("delivery", deliveryBreached);
                log.warn("SLA Monitor: Marked {} overdue assigned requests as delivery SLA breached", deliveryBreached);
            }

            // 2️⃣ Monitor Assignment SLA for PENDING requests
            int assignmentBreached = repo.markBreachedAssignmentSlaRequests(RequestStatus.PENDING, now);
            if (assignmentBreached > 0) {
                appMetricsService.incrementSlaBreaches("assignment", assignmentBreached);
                log.warn("SLA Monitor: Marked {} unassigned requests as assignment SLA breached", assignmentBreached);
            }
        } catch (Exception e) {
            log.error("Error occurred while checking SLA breaches: {}", e.getMessage(), e);
        }
    }
}
