package com.profmojo.repositories;

import com.profmojo.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get unread notifications for specific user
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(String recipientId);

    // Get all notifications for user (with limit)
    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(String recipientId);

    // Get notifications by role and department (for admins)
    List<Notification> findTop50ByRecipientRoleAndDepartmentOrderByCreatedAtDesc(String role, String department);

    // Check for duplicate notification
    Optional<Notification> findByNotificationKeyAndRecipientId(String notificationKey, String recipientId);

    // Get unread count
    long countByRecipientIdAndIsReadFalse(String recipientId);

    // Get notifications for professor/staff by role and recipientId
    List<Notification> findTop20ByRecipientRoleAndRecipientIdAndIsArchivedFalseOrderByCreatedAtDesc(
            String role, String recipientId);

    // Mark all notifications as read for user
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsRead(@Param("recipientId") String recipientId, @Param("now") LocalDateTime now);

    // Mark single notification as read
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.id = :id AND n.recipientId = :recipientId")
    int markAsRead(@Param("id") Long id, @Param("recipientId") String recipientId, @Param("now") LocalDateTime now);

    // Archive old notifications (keep only last 100 per user)
    @Modifying
    @Transactional
    @Query(value = "UPDATE notifications SET is_archived = true WHERE id IN (" +
            "SELECT id FROM (" +
            "  SELECT id, ROW_NUMBER() OVER (PARTITION BY recipient_id ORDER BY created_at DESC) as row_num " +
            "  FROM notifications WHERE is_archived = false" +
            ") ranked WHERE row_num > 100)", nativeQuery = true)
    int archiveOldNotifications();


    List<Notification> findByRecipientRole(String recipientRole);

    List<Notification> findByRecipientId(String userId);
}