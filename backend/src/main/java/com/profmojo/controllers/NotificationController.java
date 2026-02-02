package com.profmojo.controllers;

import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/admin/history")
    public List<Notification> getAdminHistory(@RequestParam String department) {
        return notificationRepository.findTop20ByRecipientRoleAndDepartmentOrderByCreatedAtDesc("ADMIN", department);
    }

    @GetMapping("/staff/history")
    public List<Notification> getStaffHistory(@AuthenticationPrincipal Staff staff) {
        // Assuming you have staffId from the authenticated principal
        return notificationRepository.findTop20ByRecipientIdOrderByCreatedAtDesc(staff.getStaffId());
    }

    @GetMapping("/professor/history")
    public List<Notification> getProfessorHistory(@AuthenticationPrincipal Professor professor) {
        return notificationRepository.findTop20ByRecipientRoleAndRecipientIdOrderByCreatedAtDesc("PROFESSOR", professor.getProfId());
    }
}