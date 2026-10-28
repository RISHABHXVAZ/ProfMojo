package com.profmojo.scheduler;

import com.profmojo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationArchiveScheduler {

    private final NotificationService notificationService;

    /**
     * Daily scheduled job to archive old notifications.
     * Keeps the last 100 notifications per recipient and archives older ones.
     * Scheduled to run once every day at midnight (00:00:00).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void archiveOldNotifications() {
        log.info("Starting daily archival of old notifications...");
        try {
            int archivedCount = notificationService.archiveOldNotifications();
            log.info("Daily notification archival completed. Archived {} notifications.", archivedCount);
        } catch (Exception e) {
            log.error("Error occurred during daily notification archival: {}", e.getMessage(), e);
        }
    }
}
