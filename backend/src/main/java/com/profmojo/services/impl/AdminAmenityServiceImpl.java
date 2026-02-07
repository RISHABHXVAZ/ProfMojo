package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AdminAmenityServiceImpl implements AdminAmenityService {

    private final AmenityRequestRepository requestRepo;
    private final StaffRepository staffRepo;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public AmenityRequest assignStaff(Long requestId, String staffId) {
        AmenityRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!staff.isAvailable()) {
            throw new RuntimeException("Staff is not available");
        }

        staff.setAvailable(false);
        request.setAssignedStaff(staff);
        request.setAssignedAt(LocalDateTime.now());

        LocalDateTime deadline = LocalDateTime.now().plusMinutes(5);
        request.setSlaDeadline(deadline);
        request.setDeliveryDeadline(deadline);

        request.setStatus(RequestStatus.ASSIGNED);

        // DON'T set confirmation code here - let controller handle it
        // Or if you want service to generate it:
        // String confirmationCode = String.format("%04d", new Random().nextInt(10000));
        // request.setDeliveryConfirmationCode(confirmationCode);
        // request.setConfirmationCodeExpiry(LocalDateTime.now().plusHours(2));

        staffRepo.save(staff);
        AmenityRequest savedRequest = requestRepo.save(request);

        return savedRequest;
    }
    @Override
    public List<AmenityRequest> getPendingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.PENDING);
    }

    @Override
    public List<AmenityRequest> getOngoingRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.ASSIGNED);
    }

    @Override
    public List<AmenityRequest> getCompletedRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.DELIVERED);
    }

    @Override
    public List<Staff> getAllStaff(String department) {
        // Change from findAll() to filter by department
        return staffRepo.findByDepartment(department);
    }

    @Override
    public List<Staff> getAvailableStaff(String department) {
        // Change from findByAvailableTrue() to filter by department
        return staffRepo.findByDepartmentAndAvailableTrue(department);
    }

    @Override
    @Transactional
    public AmenityRequest addToQueue(Long requestId) {
        AmenityRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be queued");
        }

        // Generate staff confirmation code when queued
        String staffConfirmationCode = String.format("%04d", new Random().nextInt(10000));
        request.setDeliveryConfirmationCode(staffConfirmationCode); // Need to add this field to AmenityRequest
        request.setConfirmationCodeExpiry(LocalDateTime.now().plusHours(24));

        request.setStatus(RequestStatus.QUEUED);

        // Save notification for professor
        Notification notification = Notification.builder()
                .recipientId(request.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your amenity request for " + request.getClassRoom() +
                        " has been queued. Staff Confirmation Code: " + staffConfirmationCode +
                        " (use this to verify staff identity)")
                .type("info")
                .eventType("REQUEST_QUEUED")
                .notificationKey("REQUEST_QUEUED-" + request.getId() + "-" + request.getProfessorId())
                .entityId(request.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        return requestRepo.save(request);
    }

    @Override
    public List<AmenityRequest> getQueuedRequests(String department) {
        return requestRepo
                .findByDepartmentAndStatusOrderByCreatedAtAsc(
                        department, RequestStatus.QUEUED);
    }

    @Transactional
    public void tryAssignQueuedRequest(Staff staff) {
        // 1️⃣ Get oldest queued request for staff's department
        AmenityRequest req = requestRepo
                .findFirstByStatusAndDepartmentOrderByCreatedAtAsc(
                        RequestStatus.QUEUED,
                        staff.getDepartment()
                )
                .orElse(null);

        if (req == null) return;

        // 2️⃣ Generate confirmation code (same as controller)
        String confirmationCode = String.format("%04d", new Random().nextInt(10000));

        // 3️⃣ Assign staff (with confirmation code)
        staff.setAvailable(false);
        req.setAssignedStaff(staff);
        req.setAssignedAt(LocalDateTime.now());
        req.setDeliveryConfirmationCode(confirmationCode); // Add this line
        req.setConfirmationCodeExpiry(LocalDateTime.now().plusHours(2)); // Add this line

        LocalDateTime deadline = LocalDateTime.now().plusMinutes(5);
        req.setSlaDeadline(deadline);
        req.setDeliveryDeadline(deadline);

        req.setStatus(RequestStatus.ASSIGNED);

        // 4️⃣ Save to database
        staffRepo.save(staff);
        requestRepo.save(req);

        // 5️⃣ Create notifications (same as controller)
        // Notification to Staff
        Notification staffNotif = Notification.builder()
                .recipientId(staff.getStaffId())
                .recipientRole("STAFF")
                .message("You have been assigned to Room " + req.getClassRoom() +
                        ". Confirmation Code: " + confirmationCode)
                .type("info")
                .eventType("TASK_ASSIGNED")
                .notificationKey("TASK_ASSIGNED-" + req.getId() + "-" + staff.getStaffId())
                .entityId(req.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(staffNotif);

        // Notification to Professor
        Notification profNotif = Notification.builder()
                .recipientId(req.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your request #" + req.getId() + " has been assigned to " +
                        req.getAssignedStaff().getName() +
                        ". Please share this confirmation code with staff: " + confirmationCode)
                .type("success")
                .eventType("REQUEST_ASSIGNED")
                .notificationKey("REQUEST_ASSIGNED-" + req.getId() + "-" + req.getProfessorId())
                .entityId(req.getId())
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(profNotif);

    }
}