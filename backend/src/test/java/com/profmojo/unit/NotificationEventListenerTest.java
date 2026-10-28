package com.profmojo.unit;

import com.profmojo.events.NotificationSavedEvent;
import com.profmojo.listeners.NotificationEventListener;
import com.profmojo.models.Notification;
import com.profmojo.services.NotificationSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Unit Tests - Transactional Event Listener")
class NotificationEventListenerTest {

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("handleNotificationSaved: Pushes notification to SSE service after commit")
    void handleNotificationSaved_PushesNotificationToSseService() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientRole("ADMIN")
                .department("CSE")
                .message("Test SLA breach")
                .createdAt(LocalDateTime.now())
                .build();

        NotificationSavedEvent event = new NotificationSavedEvent(notification);

        listener.handleNotificationSaved(event);

        verify(notificationSseService).pushNotification(notification);
    }

    @Test
    @DisplayName("handleNotificationSaved: Null event or notification safely does nothing")
    void handleNotificationSaved_NullEvent_DoesNothing() {
        listener.handleNotificationSaved(null);
        listener.handleNotificationSaved(new NotificationSavedEvent(null));

        verify(notificationSseService, never()).pushNotification(any());
    }
}
