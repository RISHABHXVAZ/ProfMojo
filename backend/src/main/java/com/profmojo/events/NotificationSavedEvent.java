package com.profmojo.events;

import com.profmojo.models.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSavedEvent {
    private final Notification notification;
}
