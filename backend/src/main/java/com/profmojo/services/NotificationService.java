package com.profmojo.services;

import com.profmojo.models.Notification;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    Notification sendNotification(String recipientId, String recipientRole, String message,
                                  String type, String eventType, Long entityId, Map<String, Object> data);

    void sendTaskAssignmentNotification(String staffId, String message, Long requestId);

    List<Notification> getUnreadNotifications(String recipientId);

    List<Notification> getNotificationHistory(String recipientId);

    long getUnreadCount(String recipientId);

    boolean markAsRead(Long notificationId, String recipientId);

    int markAllAsRead(String recipientId);

    boolean deleteNotification(Long notificationId, String recipientId);
}