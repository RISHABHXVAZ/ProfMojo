package com.profmojo.services.impl;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.services.SLAMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SLAMonitoringServiceImpl implements SLAMonitoringService {

    private final AmenityRequestRepository requestRepo;

    @Override
    @Transactional
    public List<AmenityRequest> checkAndMarkAssignmentSLA() {
        log.debug("Checking assignment SLA for all pending requests");

        LocalDateTime now = LocalDateTime.now();
        List<AmenityRequest> breachedRequests = new ArrayList<>();
        String[] departments = {"CSE", "ECE", "ME"};

        for (String dept : departments) {
            List<AmenityRequest> deptRequests = requestRepo.findByDepartmentAndStatus(dept, RequestStatus.PENDING);
            for (AmenityRequest request : deptRequests) {
                // Null-safe check using Boolean.TRUE.equals() to prevent NullPointerException
                if (request.getAssignmentDeadline() != null &&
                        request.getAssignmentDeadline().isBefore(now) &&
                        !Boolean.TRUE.equals(request.getAssignmentSlaBreached())) {

                    request.setAssignmentSlaBreached(true);
                    request.setSlaBreached(true);
                    breachedRequests.add(requestRepo.save(request));

                    log.warn("Assignment SLA breached for request #{}", request.getId());
                }
            }
        }
        return breachedRequests;
    }

    @Override
    @Transactional
    public List<AmenityRequest> checkAndMarkDeliverySLA(String department) {
        log.debug("Checking delivery SLA for {} department", department);

        LocalDateTime now = LocalDateTime.now();
        List<AmenityRequest> breachedRequests = new ArrayList<>();
        List<AmenityRequest> assignedRequests = requestRepo.findByDepartmentAndStatus(department, RequestStatus.ASSIGNED);

        for (AmenityRequest request : assignedRequests) {
            // Null-safe check using Boolean.TRUE.equals() to prevent NullPointerException
            if (request.getSlaDeadline() != null &&
                    request.getSlaDeadline().isBefore(now) &&
                    !Boolean.TRUE.equals(request.getDeliverySlaBreached())) {

                request.setDeliverySlaBreached(true);
                request.setSlaBreached(true);
                breachedRequests.add(requestRepo.save(request));

                log.warn("Delivery SLA breached for request #{}", request.getId());
            }
        }
        return breachedRequests;
    }

    @Override
    public List<AmenityRequest> getAllBreachedAssignmentRequests() {
        return requestRepo.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAssignmentSlaBreached()))
                .toList();
    }

    @Override
    public List<AmenityRequest> getAllBreachedDeliveryRequests(String department) {
        return requestRepo.findByDepartmentAndStatus(department, RequestStatus.ASSIGNED).stream()
                .filter(r -> Boolean.TRUE.equals(r.getDeliverySlaBreached()))
                .toList();
    }

    @Override
    public void triggerSLACheck() {
        checkAndMarkAssignmentSLA();
        String[] departments = {"CSE", "ECE", "ME"};
        for (String dept : departments) {
            checkAndMarkDeliverySLA(dept);
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void scheduledCheckAssignmentSLA() {
        List<AmenityRequest> breached = checkAndMarkAssignmentSLA();
        if (!breached.isEmpty()) sendAssignmentSlaBreachNotifications(breached);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduledCheckDeliverySLA() {
        String[] departments = {"CSE", "ECE", "ME"};
        for (String dept : departments) {
            List<AmenityRequest> breached = checkAndMarkDeliverySLA(dept);
            if (!breached.isEmpty()) sendDeliverySlaBreachNotifications(breached, dept);
        }
    }

    private void sendAssignmentSlaBreachNotifications(List<AmenityRequest> breached) {
        breached.forEach(r -> log.info("Notification: Assignment Breach #{}", r.getId()));
    }

    private void sendDeliverySlaBreachNotifications(List<AmenityRequest> breached, String dept) {
        breached.forEach(r -> log.info("Notification: Delivery Breach #{} in {}", r.getId(), dept));
    }
}