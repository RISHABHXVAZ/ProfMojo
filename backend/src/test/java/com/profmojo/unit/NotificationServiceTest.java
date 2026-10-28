package com.profmojo.unit;

import com.profmojo.models.Notification;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.profmojo.metrics.AppMetricsService appMetricsService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .recipientId("PROF01")
                .recipientRole("PROFESSOR")
                .message("Test notification")
                .type("info")
                .eventType("TEST_EVENT")
                .notificationKey("TEST_KEY_123")
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("saveNotificationIfNotExists: Saves new notification when key is not in repository")
    void saveNotificationIfNotExists_NewNotification_SavesSuccessfully() {
        when(notificationRepository.findByNotificationKey("TEST_KEY_123"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(testNotification)).thenReturn(testNotification);

        Notification saved = notificationService.saveNotificationIfNotExists(testNotification);

        assertNotNull(saved);
        assertEquals("TEST_KEY_123", saved.getNotificationKey());
        verify(notificationRepository).save(testNotification);
        verify(eventPublisher).publishEvent(any(com.profmojo.events.NotificationSavedEvent.class));
    }

    @Test
    @DisplayName("saveNotificationIfNotExists: Skips save when key already exists in repository")
    void saveNotificationIfNotExists_ExistingKey_SkipsSave() {
        when(notificationRepository.findByNotificationKey("TEST_KEY_123"))
                .thenReturn(Optional.of(testNotification));

        Notification result = notificationService.saveNotificationIfNotExists(testNotification);

        assertNotNull(result);
        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("saveNotificationIfNotExists: Safely catches DataIntegrityViolationException on concurrent insert")
    void saveNotificationIfNotExists_DataIntegrityViolation_CatchesAndReturnsExisting() {
        when(notificationRepository.findByNotificationKey("TEST_KEY_123"))
                .thenReturn(Optional.empty()) // first check: not found
                .thenReturn(Optional.of(testNotification)); // lookup on collision catch

        when(notificationRepository.save(testNotification))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates unique constraint"));

        Notification result = notificationService.saveNotificationIfNotExists(testNotification);

        assertNotNull(result);
        assertEquals("TEST_KEY_123", result.getNotificationKey());
    }

    @Test
    @DisplayName("deleteNotification: Archives notification (soft delete) instead of calling repository.delete")
    void deleteNotification_ArchivesInsteadOfHardDelete() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        boolean success = notificationService.deleteNotification(1L);

        assertTrue(success);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsArchived(), "Notification MUST be archived, not hard deleted");
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteNotification: With recipientId validates ownership before archiving")
    void deleteNotification_WithRecipientId_ArchivesWhenMatches() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        boolean success = notificationService.deleteNotification(1L, "PROF01");

        assertTrue(success);
        assertTrue(testNotification.getIsArchived());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("deleteNotification: With non-matching recipientId does not archive")
    void deleteNotification_NonMatchingRecipient_Fails() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        boolean success = notificationService.deleteNotification(1L, "WRONG_RECIPIENT");

        assertFalse(success);
        assertFalse(testNotification.getIsArchived());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAdminNotifications: Queries using unarchived filter")
    void getAdminNotifications_ExcludesArchivedRows() {
        when(notificationRepository.findTop50ByRecipientRoleAndDepartmentAndIsArchivedFalseOrderByCreatedAtDesc("ADMIN", "CSE"))
                .thenReturn(List.of(testNotification));

        List<Notification> result = notificationService.getAdminNotifications("CSE");

        assertEquals(1, result.size());
        verify(notificationRepository).findTop50ByRecipientRoleAndDepartmentAndIsArchivedFalseOrderByCreatedAtDesc("ADMIN", "CSE");
    }

    @Test
    @DisplayName("getNotificationHistory: Queries using unarchived filter")
    void getNotificationHistory_ExcludesArchivedRows() {
        when(notificationRepository.findTop50ByRecipientIdAndIsArchivedFalseOrderByCreatedAtDesc("PROF01"))
                .thenReturn(List.of(testNotification));

        List<Notification> result = notificationService.getNotificationHistory("PROF01");

        assertEquals(1, result.size());
        verify(notificationRepository).findTop50ByRecipientIdAndIsArchivedFalseOrderByCreatedAtDesc("PROF01");
    }

    @Test
    @DisplayName("getUnreadNotifications: Queries using unarchived filter")
    void getUnreadNotifications_ExcludesArchivedRows() {
        when(notificationRepository.findByRecipientIdAndIsReadFalseAndIsArchivedFalseOrderByCreatedAtDesc("PROF01"))
                .thenReturn(List.of(testNotification));

        List<Notification> result = notificationService.getUnreadNotifications("PROF01");

        assertEquals(1, result.size());
        verify(notificationRepository).findByRecipientIdAndIsReadFalseAndIsArchivedFalseOrderByCreatedAtDesc("PROF01");
    }

    @Test
    @DisplayName("markAllAsRead: Uses existing bulk update query directly")
    void markAllAsRead_UsesBulkQuery() {
        when(notificationRepository.markAllAsRead(eq("PROF01"), any(LocalDateTime.class)))
                .thenReturn(5);

        int updated = notificationService.markAllAsRead("PROF01");

        assertEquals(5, updated);
        verify(notificationRepository).markAllAsRead(eq("PROF01"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("markAllAsReadForAdmin: Uses existing bulk update query directly")
    void markAllAsReadForAdmin_UsesBulkQuery() {
        when(notificationRepository.markAllAsReadForAdmin(eq("ADMIN"), eq("CSE"), eq("ADMIN-CSE"), any(LocalDateTime.class)))
                .thenReturn(3);

        int updated = notificationService.markAllAsReadForAdmin("CSE");

        assertEquals(3, updated);
        verify(notificationRepository).markAllAsReadForAdmin(eq("ADMIN"), eq("CSE"), eq("ADMIN-CSE"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("archiveOldNotifications: Delegates to repository archiveOldNotifications")
    void archiveOldNotifications_DelegatesToRepository() {
        when(notificationRepository.archiveOldNotifications()).thenReturn(12);

        int count = notificationService.archiveOldNotifications();

        assertEquals(12, count);
        verify(notificationRepository).archiveOldNotifications();
    }
}
