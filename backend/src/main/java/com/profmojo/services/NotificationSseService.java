package com.profmojo.services;

import com.profmojo.models.Notification;
import com.profmojo.models.dto.StreamTicketData;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationSseService {

    String createStreamTicket(String userId, String role, String department);

    StreamTicketData consumeStreamTicket(String ticket);

    SseEmitter createEmitter(StreamTicketData ticketData);

    void pushNotification(Notification notification);

    void sendHeartbeat();
}
