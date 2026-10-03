package com.profmojo.controllers;

import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.Student;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final DepartmentSecretRepository departmentSecretRepository;

    // Helper: Safely resolve userId string across all authenticated principal types
    private String resolveUserId(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Professor prof) {
            return prof.getProfId();
        } else if (principal instanceof Staff staff) {
            return staff.getStaffId();
        } else if (principal instanceof Student student) {
            return student.getRegNo();
        } else if (principal instanceof String str) {
            return str;
        }
        return authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }

    private String getDepartmentFromAdminAuth(Authentication authentication) {
        String secretKey = resolveUserId(authentication);
        if (secretKey == null) return null;
        return departmentSecretRepository.findBySecretKey(secretKey)
                .map(DepartmentSecret::getDepartment)
                .orElse(secretKey);
    }

    private boolean isAuthorizedForNotification(Notification n, String userId, boolean isAdmin, String adminDept) {
        if (isAdmin && "ADMIN".equalsIgnoreCase(n.getRecipientRole())) {
            return adminDept == null || adminDept.equalsIgnoreCase(n.getDepartment());
        }
        return n.getRecipientId() != null && n.getRecipientId().equals(userId);
    }

    // 🔔 GET NOTIFICATIONS FOR ADMIN (Department-specific)
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminNotifications(
            Authentication authentication,
            @RequestParam(required = false) String department) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String targetDepartment = (department != null && !department.isBlank())
                ? department
                : getDepartmentFromAdminAuth(authentication);

        List<Notification> notifications = notificationRepository
                .findTop50ByRecipientRoleAndDepartmentOrderByCreatedAtDesc("ADMIN", targetDepartment);

        long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

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

        String professorId = resolveUserId(authentication);
        List<Notification> notifications = notificationRepository
                .findTop50ByRecipientIdOrderByCreatedAtDesc(professorId);

        long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

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

        String staffId = resolveUserId(authentication);
        List<Notification> notifications = notificationRepository
                .findTop50ByRecipientIdOrderByCreatedAtDesc(staffId);

        long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

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

        String userId = resolveUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        String adminDept = isAdmin ? getDepartmentFromAdminAuth(authentication) : null;

        return notificationRepository.findById(id)
                .filter(n -> isAuthorizedForNotification(n, userId, isAdmin, adminDept))
                .map(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
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

        String userId = resolveUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        String adminDept = isAdmin ? getDepartmentFromAdminAuth(authentication) : null;

        List<Notification> notifications;
        if (isAdmin) {
            notifications = notificationRepository.findByRecipientRoleAndDepartment("ADMIN", adminDept);
        } else {
            notifications = notificationRepository.findByRecipientId(userId);
        }

        int count = 0;
        for (Notification n : notifications) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                n.setReadAt(LocalDateTime.now());
                count++;
            }
        }
        notificationRepository.saveAll(notifications);

        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "count", count
        ));
    }

    // 🗑️ DELETE NOTIFICATION
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = resolveUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        String adminDept = isAdmin ? getDepartmentFromAdminAuth(authentication) : null;

        return notificationRepository.findById(id)
                .filter(n -> isAuthorizedForNotification(n, userId, isAdmin, adminDept))
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return ResponseEntity.ok(Map.of("message", "Notification deleted"));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Notification not found")));
    }
}