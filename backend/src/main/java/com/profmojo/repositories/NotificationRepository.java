package com.profmojo.repositories;

import com.profmojo.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Fetch last 20 notifications for a specific department's admin
    List<Notification> findTop20ByRecipientRoleAndDepartmentOrderByCreatedAtDesc(String role, String department);

    // Fetch for specific user (Professor/Staff)
    List<Notification> findTop20ByRecipientIdOrderByCreatedAtDesc(String recipientId);
}