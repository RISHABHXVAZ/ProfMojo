package com.profmojo.services.impl;

import com.profmojo.events.NotificationSavedEvent;
import com.profmojo.models.Notification;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    /**
     * Deduplication-safe save for a Notification instance.
     * Checks if notificationKey exists, and safely handles DataIntegrityViolationException on concurrent insert collisions.
     */
    @Override
    @Transactional
    public Notification saveNotificationIfNotExists(Notification notification) {
        if (notification == null) {
            return null;
        }
        if (notification.getNotificationKey() != null &&
                notificationRepository.findByNotificationKey(notification.getNotificationKey()).isPresent()) {
            if (appMetricsService != null) {
                appMetricsService.incrementNotificationDuplicatesSkipped(notification.getRecipientRole());
            }
            return notificationRepository.findByNotificationKey(notification.getNotificationKey()).orElse(null);
        }
        try {
            Notification saved = notificationRepository.save(notification);
            if (appMetricsService != null) {
                appMetricsService.incrementNotificationsCreated(saved.getRecipientRole());
            }
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new NotificationSavedEvent(saved));
            }
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent notification insert skipped for key: {}", notification.getNotificationKey());
            if (appMetricsService != null) {
                appMetricsService.incrementNotificationDuplicatesSkipped(notification.getRecipientRole());
            }
            return notificationRepository.findByNotificationKey(notification.getNotificationKey()).orElse(null);
        }
    }

    /**
     * Send notification with deduplication
     */
    @Override
    @Transactional
    public Notification sendNotification(String recipientId, String recipientRole, String message,
                                         String type, String eventType, Long entityId, Map<String, Object> data) {

        String notificationKey = eventType + "-" + entityId + "-" + recipientId;

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .recipientRole(recipientRole)
                .message(message)
                .type(type)
                .eventType(eventType)
                .entityId(entityId)
                .notificationKey(notificationKey)
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        return saveNotificationIfNotExists(notification);
    }

    /**
     * Send task assignment notification to staff
     */
    @Override
    public void sendTaskAssignmentNotification(String staffId, String message, Long requestId) {
        Map<String, Object> data = Map.of(
                "requestId", requestId,
                "timestamp", LocalDateTime.now().toString()
        );

        sendNotification(staffId, "STAFF", message, "info", "TASK_ASSIGNED", requestId, data);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * Get unarchived notifications for department admin
     */
    @Override
    public List<Notification> getAdminNotifications(String department) {
        return notificationRepository
                .findTop50ByRecipientRoleAndDepartmentAndIsArchivedFalseOrderByCreatedAtDesc("ADMIN", department);
    }

    /**
     * Get unread notifications for user (excluding archived)
     */
    @Override
    public List<Notification> getUnreadNotifications(String recipientId) {
        return notificationRepository
                .findByRecipientIdAndIsReadFalseAndIsArchivedFalseOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get all notifications for user (for history, excluding archived)
     */
    @Override
    public List<Notification> getNotificationHistory(String recipientId) {
        return notificationRepository
                .findTop50ByRecipientIdAndIsArchivedFalseOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get unread count (excluding archived)
     */
    @Override
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalseAndIsArchivedFalse(recipientId);
    }

    /**
     * Mark single notification as read
     */
    @Override
    @Transactional
    public boolean markAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(n -> {
                    n.setIsRead(true);
                    n.setReadAt(LocalDateTime.now());
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Mark notification as read for a specific recipient
     */
    @Override
    @Transactional
    public boolean markAsRead(Long notificationId, String recipientId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> recipientId == null || recipientId.equals(n.getRecipientId()))
                .map(n -> {
                    n.setIsRead(true);
                    n.setReadAt(LocalDateTime.now());
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Mark all notifications as read for user using the bulk update query
     */
    @Override
    @Transactional
    public int markAllAsRead(String recipientId) {
        return notificationRepository.markAllAsRead(recipientId, LocalDateTime.now());
    }

    /**
     * Mark all notifications as read for admin department using the bulk update query
     */
    @Override
    @Transactional
    public int markAllAsReadForAdmin(String department) {
        return notificationRepository.markAllAsReadForAdmin("ADMIN", department, "ADMIN-" + department, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int archiveOldNotifications() {
        return notificationRepository.archiveOldNotifications();
    }

    /**
     * Delete notification (soft delete - archive, not hard delete)
     */
    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(n -> {
                    n.setIsArchived(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Delete notification for a specific recipient (soft delete - archive)
     */
    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId, String recipientId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> recipientId == null || recipientId.equals(n.getRecipientId()))
                .map(n -> {
                    n.setIsArchived(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }
}
