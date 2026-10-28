package com.profmojo.scheduler;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmenitySlaMonitor {

    private final AmenityRequestRepository repo;
    private final NotificationService notificationService;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorSla() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 1️⃣ Monitor Delivery SLA for ASSIGNED requests
            List<AmenityRequest> breachedDeliveryRequests = repo.findBreachedDeliverySlaRequests(RequestStatus.ASSIGNED, now);
            int deliveryBreached = repo.markBreachedDeliverySlaRequests(RequestStatus.ASSIGNED, now);
            if (deliveryBreached > 0) {
                appMetricsService.incrementSlaBreaches("delivery", deliveryBreached);
                log.warn("SLA Monitor: Marked {} overdue assigned requests as delivery SLA breached", deliveryBreached);
                createSlaBreachNotifications(breachedDeliveryRequests, "delivery", now);
            }

            // 2️⃣ Monitor Assignment SLA for PENDING requests
            List<AmenityRequest> breachedAssignmentRequests = repo.findBreachedAssignmentSlaRequests(RequestStatus.PENDING, now);
            int assignmentBreached = repo.markBreachedAssignmentSlaRequests(RequestStatus.PENDING, now);
            if (assignmentBreached > 0) {
                appMetricsService.incrementSlaBreaches("assignment", assignmentBreached);
                log.warn("SLA Monitor: Marked {} unassigned requests as assignment SLA breached", assignmentBreached);
                createSlaBreachNotifications(breachedAssignmentRequests, "assignment", now);
            }
        } catch (Exception e) {
            log.error("Error occurred while checking SLA breaches: {}", e.getMessage(), e);
        }
    }

    private void createSlaBreachNotifications(List<AmenityRequest> requests, String breachType, LocalDateTime now) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        for (AmenityRequest req : requests) {
            String roomInfo = (req.getClassRoom() != null && !req.getClassRoom().isBlank())
                    ? " in Room " + req.getClassRoom()
                    : "";

            // 1. Department Admin Notification
            if (req.getDepartment() != null && !req.getDepartment().isBlank()) {
                String adminRecipient = "ADMIN-" + req.getDepartment();
                String adminKey = "SLA_BREACH-" + req.getId() + "-" + adminRecipient;
                Notification adminNotif = Notification.builder()
                        .recipientId(adminRecipient)
                        .recipientRole("ADMIN")
                        .department(req.getDepartment())
                        .message("🚨 SLA BREACH: " + capitalize(breachType) + " SLA breached for request #" + req.getId() + roomInfo)
                        .type("warning")
                        .eventType("SLA_BREACH")
                        .notificationKey(adminKey)
                        .entityId(req.getId())
                        .isRead(false)
                        .isArchived(false)
                        .createdAt(now)
                        .build();
                notificationService.saveNotificationIfNotExists(adminNotif);
            }

            // 2. Professor Notification
            if (req.getProfessorId() != null && !req.getProfessorId().isBlank()) {
                String profRecipient = req.getProfessorId();
                String profKey = "SLA_BREACH-" + req.getId() + "-" + profRecipient;
                Notification profNotif = Notification.builder()
                        .recipientId(profRecipient)
                        .recipientRole("PROFESSOR")
                        .message("🚨 SLA BREACH: " + capitalize(breachType) + " SLA breached for your request #" + req.getId() + roomInfo)
                        .type("warning")
                        .eventType("SLA_BREACH")
                        .notificationKey(profKey)
                        .entityId(req.getId())
                        .isRead(false)
                        .isArchived(false)
                        .createdAt(now)
                        .build();
                notificationService.saveNotificationIfNotExists(profNotif);
            }
        }
    }

    public void saveNotificationIfNotExists(Notification notif) {
        notificationService.saveNotificationIfNotExists(notif);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
