package com.profmojo.listeners;

import com.profmojo.events.NotificationSavedEvent;
import com.profmojo.services.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationSseService notificationSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationSaved(NotificationSavedEvent event) {
        if (event == null || event.getNotification() == null) {
            return;
        }
        log.debug("TransactionalEventListener: Processing NotificationSavedEvent for id={}, role={}, dept={}",
                event.getNotification().getId(),
                event.getNotification().getRecipientRole(),
                event.getNotification().getDepartment());
        notificationSseService.pushNotification(event.getNotification());
    }
}
