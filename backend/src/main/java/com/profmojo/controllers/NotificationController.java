package com.profmojo.controllers;

import com.profmojo.models.Notification;
import com.profmojo.models.Staff;
import com.profmojo.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    // 🔔 GET NOTIFICATIONS FOR ADMIN (Department-specific)
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminNotifications(
            Authentication authentication,
            @RequestParam String department) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        List<Notification> notifications = notificationRepository
                .findTop50ByRecipientRoleAndDepartmentOrderByCreatedAtDesc("ADMIN", department);

        long unreadCount = notifications.stream().filter(n -> !n.getIsRead()).count();

        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    // 🔔 GET NOTIFICATIONS FOR PROFESSOR
    @GetMapping("/professor")
    public ResponseEntity<?> getProfessorNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String professorId = authentication.getName();
        List<Notification> notifications = notificationRepository
                .findTop50ByRecipientIdOrderByCreatedAtDesc(professorId);

        long unreadCount = notifications.stream().filter(n -> !n.getIsRead()).count();

        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    // 🔔 GET NOTIFICATIONS FOR STAFF
    @GetMapping("/staff")
    public ResponseEntity<?> getStaffNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        System.out.println("=== STAFF NOTIFICATION ENDPOINT ===");

        // Get the principal (Staff object)
        Object principal = authentication.getPrincipal();
        System.out.println("Principal class: " + principal.getClass().getName());
        System.out.println("Principal: " + principal);

        String staffId;

        if (principal instanceof Staff) {
            // Cast to Staff object and get the staffId
            Staff staff = (Staff) principal;
            staffId = staff.getStaffId();
            System.out.println("Extracted staffId from Staff object: " + staffId);
        } else if (principal instanceof String) {
            // If it's already a string (username), use it directly
            staffId = (String) principal;
            System.out.println("Principal is string: " + staffId);
        } else {
            // Fallback to authentication name
            staffId = authentication.getName();
            System.out.println("Using authentication name as fallback: " + staffId);
        }

        System.out.println("Final staffId for query: " + staffId);

        List<Notification> notifications = notificationRepository
                .findTop50ByRecipientIdOrderByCreatedAtDesc(staffId);

        long unreadCount = notifications.stream().filter(n -> !n.getIsRead()).count();

        System.out.println("Found " + notifications.size() + " notifications for staff ID: " + staffId);

        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    // 📌 MARK AS READ
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();

        return notificationRepository.findById(id)
                .filter(n -> n.getRecipientId().equals(userId))
                .map(notification -> {
                    notification.setIsRead(true);
                    notificationRepository.save(notification);
                    return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Notification not found")));
    }

    // 📌 MARK ALL AS READ
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();

        List<Notification> notifications = notificationRepository.findByRecipientId(userId);

        notifications.forEach(n -> {
            if (!n.getIsRead()) {
                n.setIsRead(true);
            }
        });

        notificationRepository.saveAll(notifications);

        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "count", notifications.size()
        ));
    }

    // 🗑️ DELETE NOTIFICATION
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();

        return notificationRepository.findById(id)
                .filter(n -> n.getRecipientId().equals(userId))
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return ResponseEntity.ok(Map.of("message", "Notification deleted"));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Notification not found")));
    }
}