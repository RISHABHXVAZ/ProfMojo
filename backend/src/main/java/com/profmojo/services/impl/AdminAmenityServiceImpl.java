package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Staff;
import com.profmojo.models.dto.AmenityResponseDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAmenityServiceImpl implements AdminAmenityService {

    private final AmenityRequestRepository requestRepo;
    private final StaffRepository staffRepo;
    private final NotificationService notificationService;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @Override
    @Transactional
    public AmenityRequest assignStaff(Long requestId, String staffId, String department) {
        // 1️⃣ Lock request first (consistent lock ordering: AmenityRequest then Staff)
        AmenityRequest request = requestRepo.findByIdForUpdate(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (department != null && !department.equalsIgnoreCase(request.getDepartment())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: Amenity request does not belong to your department"
            );
        }

        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.QUEUED) {
            throw new RuntimeException("Only pending or queued requests can be assigned");
        }

        // 2️⃣ Lock staff second
        Staff staff = staffRepo.findByIdForUpdate(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (department != null && !department.equalsIgnoreCase(staff.getDepartment())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: Staff does not belong to your department"
            );
        }

        if (!request.getDepartment().equalsIgnoreCase(staff.getDepartment())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: Staff and request departments do not match"
            );
        }

        if (!staff.isAvailable()) {
            throw new RuntimeException("Staff is not available");
        }

        // 3️⃣ Atomic state transition
        staff.setAvailable(false);
        request.setAssignedStaff(staff);
        LocalDateTime now = LocalDateTime.now();
        request.setAssignedAt(now);

        LocalDateTime deadline = now.plusMinutes(5);
        request.setSlaDeadline(deadline);
        request.setDeliveryDeadline(deadline);

        request.setStatus(RequestStatus.ASSIGNED);

        // 4️⃣ Cryptographically secure confirmation code (valid for 2 hours)
        String confirmationCode = com.profmojo.util.SecureCodeGenerator.generate4DigitCode();
        request.setDeliveryConfirmationCode(confirmationCode);
        request.setConfirmationCodeExpiry(now.plusHours(2));

        staffRepo.save(staff);
        AmenityRequest savedRequest = requestRepo.save(request);
        appMetricsService.refreshQueueDepth();

        // 5️⃣ Notifications: Code ONLY to professor, never leaked to staff or admin
        Notification staffNotif = Notification.builder()
                .recipientId(staff.getStaffId())
                .recipientRole("STAFF")
                .message("You have been assigned to Room " + savedRequest.getClassRoom() +
                        ". Collect confirmation code from professor upon delivery.")
                .type("info")
                .eventType("TASK_ASSIGNED")
                .notificationKey("TASK_ASSIGNED-" + savedRequest.getId() + "-" + staff.getStaffId())
                .entityId(savedRequest.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(now)
                .build();
        notificationService.saveNotificationIfNotExists(staffNotif);

        Notification profNotif = Notification.builder()
                .recipientId(savedRequest.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your request #" + savedRequest.getId() + " has been assigned to " +
                        staff.getName() +
                        ". Please share this confirmation code with staff: " + confirmationCode)
                .type("success")
                .eventType("REQUEST_ASSIGNED")
                .notificationKey("REQUEST_ASSIGNED-" + savedRequest.getId() + "-" + savedRequest.getProfessorId())
                .entityId(savedRequest.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(now)
                .build();
        notificationService.saveNotificationIfNotExists(profNotif);

        if (department != null) {
            Notification adminNotif = Notification.builder()
                    .recipientId("ADMIN-" + department)
                    .recipientRole("ADMIN")
                    .department(department)
                    .message("Staff " + staff.getName() + " assigned to Room " + savedRequest.getClassRoom() + ".")
                    .type("success")
                    .eventType("STAFF_ASSIGNED")
                    .notificationKey("STAFF_ASSIGNED-" + savedRequest.getId() + "-" + department)
                    .entityId(savedRequest.getId())
                    .isRead(false)
                    .isArchived(false)
                    .createdAt(now)
                    .build();
            notificationService.saveNotificationIfNotExists(adminNotif);
        }

        return savedRequest;
    }

    @Override
    @Transactional
    public AmenityRequest assignStaff(Long requestId, String staffId) {
        return assignStaff(requestId, staffId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getPendingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.PENDING)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getOngoingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.ASSIGNED)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getCompletedRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.DELIVERED)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public List<Staff> getAllStaff(String department) {
        return staffRepo.findByDepartment(department);
    }

    @Override
    public List<Staff> getAvailableStaff(String department) {
        return staffRepo.findByDepartmentAndAvailableTrue(department);
    }

    @Override
    @Transactional
    public AmenityRequest addToQueue(Long requestId, String department) {
        AmenityRequest request = requestRepo.findByIdForUpdate(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (department != null && !department.equalsIgnoreCase(request.getDepartment())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: Amenity request does not belong to your department"
            );
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be queued");
        }

        request.setStatus(RequestStatus.QUEUED);

        // Save notification for professor
        Notification notification = Notification.builder()
                .recipientId(request.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your amenity request for " + request.getClassRoom() +
                        " has been queued. A confirmation code will be sent when staff is assigned.")
                .type("info")
                .eventType("REQUEST_QUEUED")
                .notificationKey("REQUEST_QUEUED-" + request.getId() + "-" + request.getProfessorId())
                .entityId(request.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationService.saveNotificationIfNotExists(notification);

        AmenityRequest saved = requestRepo.save(request);
        appMetricsService.refreshQueueDepth();
        return saved;
    }

    @Override
    @Transactional
    public AmenityRequest addToQueue(Long requestId) {
        return addToQueue(requestId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getQueuedRequests(String department) {
        return requestRepo
                .findByDepartmentAndStatusOrderByCreatedAtAsc(
                        department, RequestStatus.QUEUED)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void tryAssignQueuedRequest(Staff staff) {
        if (staff == null || staff.getStaffId() == null) {
            return;
        }

        // 1️⃣ Atomically claim the oldest QUEUED request for staff's department.
        //    FOR UPDATE SKIP LOCKED ensures concurrent callers skip already-locked
        //    rows rather than blocking, so each transaction claims a distinct request.
        AmenityRequest req = requestRepo
                .findOldestQueuedForUpdate(
                        staff.getDepartment(),
                        RequestStatus.QUEUED.name()
                )
                .orElse(null);

        if (req == null) return;

        // 2️⃣ Lock staff second (consistent lock ordering: AmenityRequest then Staff)
        Staff lockedStaff = staffRepo.findByIdForUpdate(staff.getStaffId()).orElse(null);
        if (lockedStaff == null || !lockedStaff.isAvailable()) {
            // Staff is no longer available; abort assignment so request remains safely QUEUED
            return;
        }

        if (req.getStatus() != RequestStatus.QUEUED) {
            return;
        }

        // 3️⃣ Generate SecureRandom confirmation code
        String confirmationCode = com.profmojo.util.SecureCodeGenerator.generate4DigitCode();

        // 4️⃣ Assign staff (with confirmation code)
        lockedStaff.setAvailable(false);
        req.setAssignedStaff(lockedStaff);
        LocalDateTime now = LocalDateTime.now();
        req.setAssignedAt(now);
        req.setDeliveryConfirmationCode(confirmationCode);
        req.setConfirmationCodeExpiry(now.plusHours(2));

        LocalDateTime deadline = now.plusMinutes(5);
        req.setSlaDeadline(deadline);
        req.setDeliveryDeadline(deadline);

        req.setStatus(RequestStatus.ASSIGNED);

        // 5️⃣ Save to database
        staffRepo.save(lockedStaff);
        requestRepo.save(req);
        appMetricsService.refreshQueueDepth();

        // 6️⃣ Notifications: Code ONLY to professor, never leaked to staff
        Notification staffNotif = Notification.builder()
                .recipientId(lockedStaff.getStaffId())
                .recipientRole("STAFF")
                .message("You have been assigned to Room " + req.getClassRoom() +
                        ". Collect confirmation code from professor upon delivery.")
                .type("info")
                .eventType("TASK_ASSIGNED")
                .notificationKey("TASK_ASSIGNED-" + req.getId() + "-" + lockedStaff.getStaffId())
                .entityId(req.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(now)
                .build();
        notificationService.saveNotificationIfNotExists(staffNotif);

        Notification profNotif = Notification.builder()
                .recipientId(req.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your request #" + req.getId() + " has been assigned to " +
                        lockedStaff.getName() +
                        ". Please share this confirmation code with staff: " + confirmationCode)
                .type("success")
                .eventType("REQUEST_ASSIGNED")
                .notificationKey("REQUEST_ASSIGNED-" + req.getId() + "-" + req.getProfessorId())
                .entityId(req.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(now)
                .build();
        notificationService.saveNotificationIfNotExists(profNotif);
    }
}
