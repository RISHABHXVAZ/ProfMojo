package com.profmojo.unit;

import com.profmojo.scheduler.NotificationArchiveScheduler;
import com.profmojo.services.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationArchiveScheduler Unit Tests")
class NotificationArchiveSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationArchiveScheduler scheduler;

    @Test
    @DisplayName("archiveOldNotifications: Invokes notificationService.archiveOldNotifications()")
    void archiveOldNotifications_InvokesService() {
        when(notificationService.archiveOldNotifications()).thenReturn(15);

        scheduler.archiveOldNotifications();

        verify(notificationService).archiveOldNotifications();
    }

    @Test
    @DisplayName("archiveOldNotifications: Safely handles exception from service without crashing")
    void archiveOldNotifications_HandlesExceptionGracefully() {
        when(notificationService.archiveOldNotifications()).thenThrow(new RuntimeException("Database error"));

        // Should not throw
        scheduler.archiveOldNotifications();

        verify(notificationService).archiveOldNotifications();
    }
}
