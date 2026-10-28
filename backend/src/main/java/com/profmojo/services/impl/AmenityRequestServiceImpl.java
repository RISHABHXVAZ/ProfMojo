package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.models.dto.AmenityResponseDTO;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.services.NotificationService;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmenityRequestServiceImpl implements AmenityRequestService {

    private final AmenityRequestRepository repository;
    private final StaffRepository staffRepository;
    private final NotificationService notificationService;
    private final AdminAmenityService adminAmenityService;
    private final StringRedisTemplate redisTemplate;


    private void checkDeliveryLockout(Long requestId) {
        if (requestId == null) return;
        if (redisTemplate == null) {
            log.error("RedisTemplate bean unavailable while validating delivery lockout for request [{}]. Failing closed.", requestId);
            throw new com.profmojo.exception.SecurityInfrastructureUnavailableException(
                    "Security infrastructure unavailable: unable to enforce delivery confirmation protections");
        }
        try {
            Boolean locked = redisTemplate.hasKey("profmojo:delivery:lockout:" + requestId);
            if (Boolean.TRUE.equals(locked)) {
                Long ttl = null;
                try {
                    ttl = redisTemplate.getExpire("profmojo:delivery:lockout:" + requestId, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
                long retryAfter = (ttl != null && ttl > 0) ? ttl : 900L;
                throw new com.profmojo.exception.DeliveryLockoutException(
                        "Maximum failed confirmation attempts exceeded. Locked for 15 minutes.", retryAfter);
            }
        } catch (com.profmojo.exception.DeliveryLockoutException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable while checking delivery lockout for request [{}]: {}. Failing closed.", requestId, e.getMessage());
            throw new com.profmojo.exception.SecurityInfrastructureUnavailableException(
                    "Security infrastructure unavailable: unable to check delivery lockout state", e);
        }
    }

    private void recordFailedAttempt(Long requestId) {
        if (requestId == null) return;
        if (redisTemplate == null) {
            log.error("RedisTemplate bean unavailable while recording failed delivery attempt for request [{}]. Failing closed.", requestId);
            throw new com.profmojo.exception.SecurityInfrastructureUnavailableException(
                    "Security infrastructure unavailable: unable to record failed delivery attempt");
        }
        try {
            String key = "profmojo:delivery:failed:" + requestId;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(15));
            }
            if (count != null && count >= 5L) {
                redisTemplate.opsForValue().set("profmojo:delivery:lockout:" + requestId, "LOCKED", Duration.ofMinutes(15));
                log.warn("Delivery verification locked out for request [{}] after {} failed attempts", requestId, count);
            }
        } catch (Exception e) {
            log.error("Redis unavailable while recording failed delivery attempt for request [{}]: {}. Failing closed.", requestId, e.getMessage());
            throw new com.profmojo.exception.SecurityInfrastructureUnavailableException(
                    "Security infrastructure unavailable: unable to record failed delivery attempt", e);
        }
    }

    private void clearAttemptCounter(Long requestId) {
        if (redisTemplate == null || requestId == null) return;
        try {
            redisTemplate.delete(List.of(
                    "profmojo:delivery:failed:" + requestId,
                    "profmojo:delivery:lockout:" + requestId
            ));
        } catch (Exception e) {
            log.warn("Redis unavailable while clearing delivery attempt counter for request [{}]: {}", requestId, e.getMessage());
        }
    }

    @Override
    public AmenityRequest raiseRequest(AmenityRequestDTO dto, Professor professor) {
        LocalDateTime now = LocalDateTime.now();

        AmenityRequest request = AmenityRequest.builder()
                .professorId(professor.getProfId())
                .professorName(professor.getName())
                .classRoom(dto.getClassRoom())
                .department(dto.getDepartment())
                .items(dto.getItems())
                .status(RequestStatus.PENDING)
                .createdAt(now)
                .assignmentDeadline(now.plusMinutes(2))
                .assignmentSlaBreached(false) // Ensure defaults to avoid NPE
                .deliverySlaBreached(false)
                .build();

        return repository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getMyRequests(String professorId) {
        return repository.findByProfessorIdAndStatusNot(professorId, RequestStatus.DELIVERED)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getMyDeliveredRequests(String professorId) {
        return repository.findByProfessorIdAndStatusOrderByDeliveredAtDesc(professorId, RequestStatus.DELIVERED)
                .stream()
                .map(AmenityResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public AmenityRequest findById(Long requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }

    @Override
    @Transactional
    public AmenityRequest reRequestDueToSLABreach(Long originalId, String profId) {
        // 1️⃣ Lock the existing request
        AmenityRequest oldReq = repository.findByIdForUpdate(originalId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!oldReq.getProfessorId().equals(profId)) {
            throw new AccessDeniedException("Unauthorized: Request does not belong to you");
        }

        if (oldReq.getStatus() == RequestStatus.DELIVERED) {
            throw new IllegalStateException("Delivered request cannot be cancelled or re-requested");
        }

        if (oldReq.getStatus() == RequestStatus.CANCELLED_SLA_BREACH) {
            throw new IllegalStateException("Request is already cancelled due to SLA breach");
        }

        // Release assigned staff if previously assigned to this request
        if (oldReq.getAssignedStaff() != null) {
            Staff assignedStaff = staffRepository.findByIdForUpdate(oldReq.getAssignedStaff().getStaffId()).orElse(null);
            if (assignedStaff != null) {
                assignedStaff.setAvailable(true);
                staffRepository.save(assignedStaff);
            }
        }

        // Invalidate confirmation code on old request
        oldReq.setDeliveryConfirmationCode(null);
        oldReq.setConfirmationCodeExpiry(null);
        oldReq.setStatus(RequestStatus.CANCELLED_SLA_BREACH);
        repository.save(oldReq);

        LocalDateTime now = LocalDateTime.now();
        AmenityRequest newReq = AmenityRequest.builder()
                .professorId(profId)
                .professorName(oldReq.getProfessorName())
                .department(oldReq.getDepartment())
                .classRoom(oldReq.getClassRoom())
                .items(new ArrayList<>(oldReq.getItems()))
                .status(RequestStatus.PENDING)
                .createdAt(now)
                .assignmentDeadline(now.plusMinutes(2))
                .assignmentSlaBreached(false)
                .deliverySlaBreached(false)
                .build();

        return repository.save(newReq);
    }

    @Override
    @Transactional
    public Map<String, Object> confirmDelivery(Long requestId, String confirmationCode, Staff staff) {
        if (staff == null || staff.getStaffId() == null) {
            throw new AccessDeniedException("Unauthenticated staff");
        }

        // 1. Check bounded invalid-attempt rate limiting and lockout (fail-closed if Redis unavailable)
        checkDeliveryLockout(requestId);

        // 2. Lock request first (consistent lock ordering: AmenityRequest then Staff)
        AmenityRequest request = repository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // 3. Verify legal state transition
        if (request.getStatus() == RequestStatus.DELIVERED) {
            throw new IllegalStateException("Request is already marked as delivered");
        }
        if (request.getStatus() != RequestStatus.ASSIGNED) {
            throw new IllegalStateException("Only assigned requests can be delivered");
        }

        // 4. Verify assigned staff
        if (request.getAssignedStaff() == null ||
                !request.getAssignedStaff().getStaffId().equals(staff.getStaffId())) {
            throw new AccessDeniedException("You are not assigned to this request");
        }

        // 5. Verify confirmation code & expiry
        if (request.getDeliveryConfirmationCode() == null) {
            throw new IllegalStateException("No confirmation code set for this request");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getConfirmationCodeExpiry() != null &&
                request.getConfirmationCodeExpiry().isBefore(now)) {
            throw new IllegalArgumentException("Confirmation code has expired");
        }

        String inputCode = confirmationCode != null ? confirmationCode.trim() : "";
        if (!request.getDeliveryConfirmationCode().equals(inputCode)) {
            recordFailedAttempt(requestId);
            throw new IllegalArgumentException("Invalid confirmation code");
        }

        // Code matches successfully! Clear lockout / attempt counters
        clearAttemptCounter(requestId);

        // 6. Lock staff second (monotonic lock order: AmenityRequest then Staff)
        Staff lockedStaff = staffRepository.findByIdForUpdate(staff.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        // 7. Check SLA deadline for star penalty/reward
        boolean slaBreached;
        String message;
        if (request.getSlaDeadline() != null && now.isAfter(request.getSlaDeadline())) {
            lockedStaff.setStars(Math.max(0, lockedStaff.getStars() - 1));
            request.setDeliverySlaBreached(true);
            request.setSlaBreached(true);
            slaBreached = true;
            message = "Package marked as delivered but SLA breached. Star decreased by 1.";
        } else {
            lockedStaff.setStars(lockedStaff.getStars() + 1);
            request.setDeliverySlaBreached(false);
            slaBreached = false;
            message = "Package marked as delivered successfully! Star increased by 1.";
        }

        // 8. Update staff stats & availability
        lockedStaff.setTotalDeliveries(lockedStaff.getTotalDeliveries() + 1);
        lockedStaff.setAvailable(true);

        // 9. Invalidate confirmation code (single use)
        request.setDeliveryConfirmationCode(null);
        request.setConfirmationCodeExpiry(null);

        // 10. Update request status
        request.setStatus(RequestStatus.DELIVERED);
        request.setDeliveredAt(now);

        staffRepository.save(lockedStaff);
        repository.save(request);

        // 11. Create notifications
        Notification staffNotif = Notification.builder()
                .recipientId(lockedStaff.getStaffId())
                .recipientRole("STAFF")
                .message(slaBreached ?
                        "Task #" + requestId + " delivered but SLA breached. Star decreased by 1." :
                        "Task #" + requestId + " delivered successfully! Star increased by 1.")
                .type(slaBreached ? "warning" : "success")
                .eventType("TASK_DELIVERED")
                .notificationKey("TASK_DELIVERED-" + requestId + "-" + lockedStaff.getStaffId())
                .entityId(requestId)
                .isRead(false)
                .isArchived(false)
                .createdAt(now)
                .build();
        notificationService.saveNotificationIfNotExists(staffNotif);

        Notification profNotif = Notification.builder()
                .recipientId(request.getProfessorId())
                .recipientRole("PROFESSOR")
                .message("Your items have been delivered to " + request.getClassRoom())
                .type("success")
                .eventType("DELIVERY_COMPLETED")
                .notificationKey("DELIVERY_COMPLETED-" + requestId + "-" + request.getProfessorId())
                .entityId(requestId)
                .isRead(false)
                .isArchived(false)
                .createdAt(now)
                .build();
        notificationService.saveNotificationIfNotExists(profNotif);

        // 12. Auto-assign next queued request via atomic FIFO service
        adminAmenityService.tryAssignQueuedRequest(lockedStaff);

        return Map.of(
                "message", message,
                "slaBreached", slaBreached,
                "newStars", lockedStaff.getStars()
        );
    }
}