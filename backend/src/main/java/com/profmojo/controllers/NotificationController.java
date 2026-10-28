package com.profmojo.controllers;

import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.Student;
import com.profmojo.models.dto.NotificationResponseDTO;
import com.profmojo.models.dto.StreamTicketData;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.services.NotificationService;
import com.profmojo.services.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final DepartmentSecretRepository departmentSecretRepository;
    private final NotificationSseService notificationSseService;

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
                .filter(DepartmentSecret::isActive)
                .map(DepartmentSecret::getDepartment)
                .orElse(null);
    }

    private String getRoleFromAuth(Authentication authentication) {
        if (authentication == null) return "UNKNOWN";
        if (isAdmin(authentication)) return "ADMIN";
        Object principal = authentication.getPrincipal();
        if (principal instanceof Professor) return "PROFESSOR";
        if (principal instanceof Staff) return "STAFF";
        if (principal instanceof Student) return "STUDENT";
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .findFirst()
                .orElse("USER");
    }

    // 🎫 GET STREAM TICKET (Authenticated normally)
    @PostMapping("/stream-ticket")
    public ResponseEntity<?> createStreamTicket(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = isAdmin(authentication);
        String adminDept = null;
        if (isAdmin) {
            adminDept = getDepartmentFromAdminAuth(authentication);
            if (adminDept == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin department could not be resolved or secret is inactive"));
            }
        }

        String userId = resolveUserId(authentication);
        String role = isAdmin ? "ADMIN" : getRoleFromAuth(authentication);

        String ticket = notificationSseService.createStreamTicket(userId, role, adminDept);
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    // 📡 STREAM NOTIFICATIONS VIA SSE (Accepts ?ticket=..., consumes it once, never accepts JWT)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(@RequestParam(value = "ticket", required = false) String ticket) {
        // Never accept JWT in the URL (which contains dots) or empty ticket
        if (ticket == null || ticket.isBlank() || ticket.contains(".")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StreamTicketData ticketData = notificationSseService.consumeStreamTicket(ticket);
        if (ticketData == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SseEmitter emitter = notificationSseService.createEmitter(ticketData);
        return ResponseEntity.ok(emitter);
    }

    private boolean isAuthorizedForNotification(Notification n, String userId, boolean isAdmin, String adminDept) {
        if (isAdmin && "ADMIN".equalsIgnoreCase(n.getRecipientRole())) {
            return adminDept != null && adminDept.equalsIgnoreCase(n.getDepartment());
        }
        return n.getRecipientId() != null && n.getRecipientId().equals(userId);
    }

    // 🔔 GET NOTIFICATIONS FOR ADMIN (Department-specific, excluding archived)
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminNotifications(
            Authentication authentication,
            @RequestParam(required = false) String department) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String adminDept = getDepartmentFromAdminAuth(authentication);
        if (adminDept == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin department could not be resolved or secret is inactive"));
        }

        // Reject attempts to query a foreign department
        if (department != null && !department.isBlank()) {
            if (!adminDept.equalsIgnoreCase(department.trim())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized: Cannot access notifications for another department"));
            }
        }

        List<NotificationResponseDTO> dtos = notificationService.getAdminNotifications(adminDept)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();

        long unreadCount = dtos.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

        return ResponseEntity.ok(Map.of(
                "notifications", dtos,
                "unreadCount", unreadCount
        ));
    }

    // 🔔 GET NOTIFICATIONS FOR PROFESSOR (excluding archived)
    @GetMapping("/professor")
    public ResponseEntity<?> getProfessorNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String professorId = resolveUserId(authentication);
        List<NotificationResponseDTO> dtos = notificationService.getNotificationHistory(professorId)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();

        long unreadCount = dtos.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

        return ResponseEntity.ok(Map.of(
                "notifications", dtos,
                "unreadCount", unreadCount
        ));
    }

    // 🔔 GET NOTIFICATIONS FOR STAFF (excluding archived)
    @GetMapping("/staff")
    public ResponseEntity<?> getStaffNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String staffId = resolveUserId(authentication);
        List<NotificationResponseDTO> dtos = notificationService.getNotificationHistory(staffId)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();

        long unreadCount = dtos.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

        return ResponseEntity.ok(Map.of(
                "notifications", dtos,
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
        if (isAdmin && adminDept == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin department could not be resolved or secret is inactive"));
        }

        Notification notification = notificationService.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notification not found"));
        }

        if (!isAuthorizedForNotification(notification, userId, isAdmin, adminDept)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Unauthorized: You do not have permission to modify this notification"));
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
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
        if (isAdmin && adminDept == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin department could not be resolved or secret is inactive"));
        }

        int count;
        if (isAdmin) {
            count = notificationService.markAllAsReadForAdmin(adminDept);
        } else {
            count = notificationService.markAllAsRead(userId);
        }

        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "count", count
        ));
    }

    // 🗑️ DELETE NOTIFICATION (Archive, not hard delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = resolveUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        String adminDept = isAdmin ? getDepartmentFromAdminAuth(authentication) : null;
        if (isAdmin && adminDept == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin department could not be resolved or secret is inactive"));
        }

        Notification notification = notificationService.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notification not found"));
        }

        if (!isAuthorizedForNotification(notification, userId, isAdmin, adminDept)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Unauthorized: You do not have permission to delete this notification"));
        }

        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }
}
