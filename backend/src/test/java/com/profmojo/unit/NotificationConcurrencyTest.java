package com.profmojo.unit;

import com.profmojo.models.Notification;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.services.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Concurrency and Unique Constraint Tests")
class NotificationConcurrencyTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private ConcurrentHashMap<String, Notification> dbStorage;
    private List<Notification> allSavedNotifications;
    private AtomicInteger saveCallCount;

    @BeforeEach
    void setUp() {
        dbStorage = new ConcurrentHashMap<>();
        allSavedNotifications = new CopyOnWriteArrayList<>();
        saveCallCount = new AtomicInteger(0);

        lenient().when(notificationRepository.findByNotificationKey(anyString()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    return Optional.ofNullable(dbStorage.get(key));
                });

        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    saveCallCount.incrementAndGet();
                    Notification notif = invocation.getArgument(0);
                    if (notif.getNotificationKey() != null) {
                        Notification existing = dbStorage.putIfAbsent(notif.getNotificationKey(), notif);
                        if (existing != null) {
                            // Simulate database unique index constraint violation
                            throw new DataIntegrityViolationException(
                                    "duplicate key value violates unique constraint \"uq_notifications_notification_key\"");
                        }
                    }
                    allSavedNotifications.add(notif);
                    return notif;
                });
    }

    @Test
    @DisplayName("Many threads concurrently saving with same key results in exactly one saved row without errors")
    void manyThreadsSavingSameKey_ResultsInExactlyOneRow() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        String sharedKey = "SLA_BREACH-42-PROFESSOR";

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // ensure all threads fire concurrently
                    Notification notif = Notification.builder()
                            .id((long) index)
                            .recipientId("PROF01")
                            .recipientRole("PROFESSOR")
                            .message("SLA breached for request #42")
                            .type("error")
                            .eventType("SLA_BREACH")
                            .notificationKey(sharedKey)
                            .createdAt(LocalDateTime.now())
                            .build();

                    notificationService.saveNotificationIfNotExists(notif);
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("All threads should finish within timeout").isTrue();
        assertThat(exceptions).as("No threads should throw uncaught exceptions").isEmpty();

        // Exactly one row exists in the storage for this key
        assertThat(dbStorage).hasSize(1);
        assertThat(dbStorage.containsKey(sharedKey)).isTrue();
        assertThat(dbStorage.get(sharedKey).getNotificationKey()).isEqualTo(sharedKey);
    }

    @Test
    @DisplayName("Many threads concurrently saving with null keys allows multiple rows")
    void manyThreadsSavingNullKey_AllowsMultipleRows() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Notification notif = Notification.builder()
                            .id((long) index)
                            .recipientId("PROF" + index)
                            .recipientRole("PROFESSOR")
                            .message("Notification without key " + index)
                            .type("info")
                            .notificationKey(null)
                            .createdAt(LocalDateTime.now())
                            .build();

                    notificationService.saveNotificationIfNotExists(notif);
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("All threads should finish within timeout").isTrue();
        assertThat(exceptions).as("No threads should throw uncaught exceptions").isEmpty();
        assertThat(allSavedNotifications).hasSize(threadCount);
    }
}
