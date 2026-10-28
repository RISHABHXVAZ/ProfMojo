package com.profmojo.unit;

import com.profmojo.metrics.AppMetricsService;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.scheduler.AmenitySlaMonitor;
import com.profmojo.services.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AmenitySlaMonitor Unit Tests")
class AmenitySlaMonitorTest {

    @Mock
    private AmenityRequestRepository repo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AppMetricsService appMetricsService;

    @InjectMocks
    private AmenitySlaMonitor slaMonitor;

    @Test
    @DisplayName("Assignment SLA breach creates notifications for Admin and Professor")
    void assignmentSlaBreach_CreatesAdminAndProfessorNotifications() {
        AmenityRequest breachedReq = AmenityRequest.builder()
                .id(101L)
                .department("CSE")
                .professorId("PROF01")
                .professorName("Dr. Alan Turing")
                .classRoom("LH-101")
                .status(RequestStatus.PENDING)
                .build();

        when(repo.findBreachedDeliverySlaRequests(eq(RequestStatus.ASSIGNED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(repo.markBreachedDeliverySlaRequests(eq(RequestStatus.ASSIGNED), any(LocalDateTime.class)))
                .thenReturn(0);

        when(repo.findBreachedAssignmentSlaRequests(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(breachedReq));
        when(repo.markBreachedAssignmentSlaRequests(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(1);

        slaMonitor.monitorSla();

        verify(appMetricsService).incrementSlaBreaches("assignment", 1);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(2)).saveNotificationIfNotExists(notifCaptor.capture());

        List<Notification> savedNotifications = notifCaptor.getAllValues();
        assertThat(savedNotifications).hasSize(2);

        // 1. Verify Admin notification
        Notification adminNotif = savedNotifications.stream()
                .filter(n -> "ADMIN".equals(n.getRecipientRole()))
                .findFirst()
                .orElseThrow();
        assertThat(adminNotif.getRecipientId()).isEqualTo("ADMIN-CSE");
        assertThat(adminNotif.getRecipientRole()).isEqualTo("ADMIN");
        assertThat(adminNotif.getDepartment()).isEqualTo("CSE");
        assertThat(adminNotif.getEventType()).isEqualTo("SLA_BREACH");
        assertThat(adminNotif.getNotificationKey()).isEqualTo("SLA_BREACH-101-ADMIN-CSE");
        assertThat(adminNotif.getEntityId()).isEqualTo(101L);
        assertThat(adminNotif.getType()).isEqualTo("warning");
        assertThat(adminNotif.getMessage()).contains("Assignment SLA breached").contains("LH-101");
        assertThat(adminNotif.getIsRead()).isFalse();

        // 2. Verify Professor notification
        Notification profNotif = savedNotifications.stream()
                .filter(n -> "PROFESSOR".equals(n.getRecipientRole()))
                .findFirst()
                .orElseThrow();
        assertThat(profNotif.getRecipientId()).isEqualTo("PROF01");
        assertThat(profNotif.getRecipientRole()).isEqualTo("PROFESSOR");
        assertThat(profNotif.getEventType()).isEqualTo("SLA_BREACH");
        assertThat(profNotif.getNotificationKey()).isEqualTo("SLA_BREACH-101-PROF01");
        assertThat(profNotif.getEntityId()).isEqualTo(101L);
        assertThat(profNotif.getType()).isEqualTo("warning");
        assertThat(profNotif.getMessage()).contains("Assignment SLA breached").contains("LH-101");
        assertThat(profNotif.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("Delivery SLA breach creates notifications for Admin and Professor")
    void deliverySlaBreach_CreatesAdminAndProfessorNotifications() {
        AmenityRequest breachedReq = AmenityRequest.builder()
                .id(202L)
                .department("ECE")
                .professorId("PROF02")
                .professorName("Dr. Claude Shannon")
                .classRoom("LH-202")
                .status(RequestStatus.ASSIGNED)
                .build();

        when(repo.findBreachedDeliverySlaRequests(eq(RequestStatus.ASSIGNED), any(LocalDateTime.class)))
                .thenReturn(List.of(breachedReq));
        when(repo.markBreachedDeliverySlaRequests(eq(RequestStatus.ASSIGNED), any(LocalDateTime.class)))
                .thenReturn(1);

        when(repo.findBreachedAssignmentSlaRequests(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(repo.markBreachedAssignmentSlaRequests(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(0);

        slaMonitor.monitorSla();

        verify(appMetricsService).incrementSlaBreaches("delivery", 1);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(2)).saveNotificationIfNotExists(notifCaptor.capture());

        List<Notification> savedNotifications = notifCaptor.getAllValues();
        assertThat(savedNotifications).hasSize(2);

        Notification adminNotif = savedNotifications.stream()
                .filter(n -> "ADMIN".equals(n.getRecipientRole()))
                .findFirst()
                .orElseThrow();
        assertThat(adminNotif.getRecipientId()).isEqualTo("ADMIN-ECE");
        assertThat(adminNotif.getNotificationKey()).isEqualTo("SLA_BREACH-202-ADMIN-ECE");
        assertThat(adminNotif.getDepartment()).isEqualTo("ECE");
        assertThat(adminNotif.getMessage()).contains("Delivery SLA breached");

        Notification profNotif = savedNotifications.stream()
                .filter(n -> "PROFESSOR".equals(n.getRecipientRole()))
                .findFirst()
                .orElseThrow();
        assertThat(profNotif.getRecipientId()).isEqualTo("PROF02");
        assertThat(profNotif.getNotificationKey()).isEqualTo("SLA_BREACH-202-PROF02");
        assertThat(profNotif.getMessage()).contains("Delivery SLA breached");
    }

    @Test
    @DisplayName("When no breaches occur, no notifications are created")
    void noBreaches_NoNotificationsCreated() {
        when(repo.findBreachedDeliverySlaRequests(eq(RequestStatus.ASSIGNED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(repo.markBreachedDeliverySlaRequests(eq(RequestStatus.ASSIGNED), any(LocalDateTime.class)))
                .thenReturn(0);

        when(repo.findBreachedAssignmentSlaRequests(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(repo.markBreachedAssignmentSlaRequests(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(0);

        slaMonitor.monitorSla();

        verify(notificationService, never()).saveNotificationIfNotExists(any(Notification.class));
        verify(appMetricsService, never()).incrementSlaBreaches(anyString(), anyInt());
    }
}
