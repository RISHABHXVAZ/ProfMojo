package com.profmojo.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profmojo.models.Notification;
import com.profmojo.models.dto.NotificationResponseDTO;
import com.profmojo.models.dto.StreamTicketData;
import com.profmojo.services.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationSseServiceImpl implements NotificationSseService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.profmojo.metrics.AppMetricsService appMetricsService;

    @Autowired
    public NotificationSseServiceImpl(
            StringRedisTemplate redisTemplate,
            @Autowired(required = false) ObjectMapper objectMapper,
            @Autowired(required = false) com.profmojo.metrics.AppMetricsService appMetricsService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper().findAndRegisterModules();
        this.appMetricsService = appMetricsService;
    }

    private static final String TICKET_KEY_PREFIX = "profmojo:sse:ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final Long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    // Registry of active emitters:
    // Admin emitters grouped by uppercase department (e.g. "CSE")
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> adminDepartmentEmitters = new ConcurrentHashMap<>();
    // User emitters grouped by recipientId (e.g. professorId, staffId, studentId)
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    @Override
    public String createStreamTicket(String userId, String role, String department) {
        String ticket = UUID.randomUUID().toString();
        String redisKey = TICKET_KEY_PREFIX + ticket;

        StreamTicketData ticketData = StreamTicketData.builder()
                .userId(userId)
                .role(role != null ? role.toUpperCase() : "USER")
                .department(department != null ? department.trim().toUpperCase() : null)
                .build();

        try {
            String json = objectMapper.writeValueAsString(ticketData);
            redisTemplate.opsForValue().set(redisKey, json, TICKET_TTL);
            log.debug("Created stream ticket {} for user={}, role={}, dept={}", ticket, userId, role, department);
            return ticket;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize StreamTicketData for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error generating stream ticket", e);
        }
    }

    @Override
    public StreamTicketData consumeStreamTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        String redisKey = TICKET_KEY_PREFIX + ticket.trim();
        String json = redisTemplate.opsForValue().getAndDelete(redisKey);
        if (json == null || json.isBlank()) {
            log.warn("Stream ticket {} is invalid, expired, or already consumed", ticket);
            return null;
        }

        try {
            return objectMapper.readValue(json, StreamTicketData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize StreamTicketData from Redis: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public SseEmitter createEmitter(StreamTicketData ticketData) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(ticketData.getRole());
        String adminDept = ticketData.getDepartment();

        if (isAdmin && adminDept != null && !adminDept.isBlank()) {
            String deptKey = adminDept.trim().toUpperCase();
            adminDepartmentEmitters.computeIfAbsent(deptKey, k -> new CopyOnWriteArrayList<>()).add(emitter);
            log.info("Registered SSE emitter for admin department: {}", deptKey);

            Runnable cleanup = () -> removeEmitter(adminDepartmentEmitters, deptKey, emitter);
            emitter.onCompletion(cleanup);
            emitter.onTimeout(cleanup);
            emitter.onError(e -> cleanup.run());
        } else {
            String userIdKey = ticketData.getUserId();
            if (userIdKey != null && !userIdKey.isBlank()) {
                userEmitters.computeIfAbsent(userIdKey, k -> new CopyOnWriteArrayList<>()).add(emitter);
                log.info("Registered SSE emitter for user: {}", userIdKey);

                Runnable cleanup = () -> removeEmitter(userEmitters, userIdKey, emitter);
                emitter.onCompletion(cleanup);
                emitter.onTimeout(cleanup);
                emitter.onError(e -> cleanup.run());
            }
        }

        // Send initial connection comment to establish HTTP stream and flush headers
        try {
            emitter.send(SseEmitter.event().comment("connected"));
            if (appMetricsService != null) {
                appMetricsService.incrementActiveSseConnections();
            }
        } catch (IOException e) {
            log.warn("Failed to send initial SSE comment to emitter: {}", e.getMessage());
        }

        return emitter;
    }

    private void removeEmitter(ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> registry, String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = registry.get(key);
        if (list != null) {
            boolean removed = list.remove(emitter);
            if (removed && appMetricsService != null) {
                appMetricsService.decrementActiveSseConnections();
            }
            if (list.isEmpty()) {
                registry.remove(key, list);
            }
        }
    }

    @Override
    public void pushNotification(Notification notification) {
        if (notification == null) {
            return;
        }

        NotificationResponseDTO dto = NotificationResponseDTO.fromEntity(notification);
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name("notification")
                .id(String.valueOf(dto.getId()))
                .data(dto);

        if ("ADMIN".equalsIgnoreCase(notification.getRecipientRole())) {
            String dept = notification.getDepartment();
            if ((dept == null || dept.isBlank()) && notification.getRecipientId() != null && notification.getRecipientId().startsWith("ADMIN-")) {
                dept = notification.getRecipientId().substring("ADMIN-".length());
            }

            if (dept != null && !dept.isBlank()) {
                String deptKey = dept.trim().toUpperCase();
                CopyOnWriteArrayList<SseEmitter> emitters = adminDepartmentEmitters.get(deptKey);
                sendToEmitters(adminDepartmentEmitters, deptKey, emitters, eventBuilder);
            }
        } else {
            String recipientId = notification.getRecipientId();
            if (recipientId != null && !recipientId.isBlank()) {
                CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(recipientId);
                sendToEmitters(userEmitters, recipientId, emitters, eventBuilder);
            }
        }
    }

    private void sendToEmitters(ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> registry,
                                String key,
                                CopyOnWriteArrayList<SseEmitter> emitters,
                                SseEmitter.SseEventBuilder eventBuilder) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(eventBuilder);
            } catch (Exception ex) {
                log.debug("Error delivering SSE to emitter under key {}, scheduling removal: {}", key, ex.getMessage());
                deadEmitters.add(emitter);
            }
        }

        for (SseEmitter dead : deadEmitters) {
            removeEmitter(registry, key, dead);
            try {
                dead.complete();
            } catch (Exception ignored) {}
        }
    }

    @Override
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        log.trace("Executing SSE heartbeat to active emitters...");
        sendHeartbeatToMap(adminDepartmentEmitters);
        sendHeartbeatToMap(userEmitters);
    }

    private void sendHeartbeatToMap(ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> registry) {
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : registry.entrySet()) {
            String key = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception ex) {
                    log.debug("Heartbeat delivery failed for emitter under key {}, scheduling removal: {}", key, ex.getMessage());
                    deadEmitters.add(emitter);
                }
            }

            for (SseEmitter dead : deadEmitters) {
                emitters.remove(dead);
                try {
                    dead.complete();
                } catch (Exception ignored) {}
            }

            if (emitters.isEmpty()) {
                registry.remove(key, emitters);
            }
        }
    }
}
