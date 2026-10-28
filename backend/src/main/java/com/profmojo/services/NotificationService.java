package com.profmojo.services;

import com.profmojo.models.Notification;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NotificationService {

    Notification sendNotification(String recipientId, String recipientRole, String message,
                                  String type, String eventType, Long entityId, Map<String, Object> data);

    void sendTaskAssignmentNotification(String staffId, String message, Long requestId);

    /**
     * Deduplication-safe save for a Notification instance.
     * Checks if notificationKey exists, and safely handles DataIntegrityViolationException on concurrent insert collisions.
     */
    Notification saveNotificationIfNotExists(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> getAdminNotifications(String department);

    List<Notification> getUnreadNotifications(String recipientId);

    List<Notification> getNotificationHistory(String recipientId);

    long getUnreadCount(String recipientId);

    boolean markAsRead(Long notificationId);

    boolean markAsRead(Long notificationId, String recipientId);

    int markAllAsRead(String recipientId);

    int markAllAsReadForAdmin(String department);

    boolean deleteNotification(Long notificationId);

    boolean deleteNotification(Long notificationId, String recipientId);

    int archiveOldNotifications();
}
