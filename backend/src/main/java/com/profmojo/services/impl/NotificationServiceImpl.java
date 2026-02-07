package com.profmojo.services.impl;

import com.profmojo.models.Notification;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Send notification with deduplication
     */
    @Override
    @Transactional
    public Notification sendNotification(String recipientId, String recipientRole, String message,
                                         String type, String eventType, Long entityId, Map<String, Object> data) {

        // Create unique key for deduplication
        String notificationKey = eventType + "-" + entityId + "-" + recipientId;

        // Check for duplicate
        Optional<Notification> existing = notificationRepository
                .findByNotificationKeyAndRecipientId(notificationKey, recipientId);

        if (existing.isPresent()) {
            log.debug("Duplicate notification skipped for {}: {}", recipientId, notificationKey);
            return existing.get();
        }

        // Create new notification (Database only)
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

        return notificationRepository.save(notification);
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

    /**
     * Get unread notifications for user
     */
    @Override
    public List<Notification> getUnreadNotifications(String recipientId) {
        return notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get all notifications for user (for history)
     */
    @Override
    public List<Notification> getNotificationHistory(String recipientId) {
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get unread count
     */
    @Override
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    /**
     * Mark notification as read
     */
    @Override
    @Transactional
    public boolean markAsRead(Long notificationId, String recipientId) {
        int updated = notificationRepository.markAsRead(
                notificationId, recipientId, LocalDateTime.now());

        return updated > 0;
    }

    /**
     * Mark all notifications as read for user
     */
    @Override
    @Transactional
    public int markAllAsRead(String recipientId) {
        return notificationRepository.markAllAsRead(recipientId, LocalDateTime.now());
    }

    /**
     * Delete notification (soft delete - archive)
     */
    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId, String recipientId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipientId().equals(recipientId))
                .map(n -> {
                    n.setIsArchived(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }
}