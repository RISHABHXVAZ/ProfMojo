package com.profmojo.unit;

import com.profmojo.config.AsyncConfig;
import com.profmojo.metrics.AppMetricsService;
import com.profmojo.security.CorrelationIdFilter;
import com.profmojo.services.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncEmailService Unit Tests")
class AsyncEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AppMetricsService appMetricsService;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, appMetricsService);
    }

    @Test
    @DisplayName("Email send succeeds, dispatches message and increments success metric")
    void send_Success_DispatchesAndIncrementsMetric() {
        emailService.send("test@example.com", "Test Subject", "Test Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertNotNull(sent.getTo());
        assertEquals("test@example.com", sent.getTo()[0]);
        assertEquals("Test Subject", sent.getSubject());
        assertEquals("Test Body", sent.getText());

        verify(appMetricsService, times(1)).incrementEmailDelivery("success");
    }

    @Test
    @DisplayName("Email send MailException is caught, logged, and increments failed metric")
    void send_MailException_HandlesAndIncrementsFailedMetric() {
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.send("test@example.com", "Test Subject", "Test Body"));

        verify(appMetricsService, times(1)).incrementEmailDelivery("failed");
        verify(appMetricsService, never()).incrementEmailDelivery("success");
    }

    @Test
    @DisplayName("Email send unexpected Exception is caught, logged, and increments failed metric")
    void send_UnexpectedException_HandlesAndIncrementsFailedMetric() {
        doThrow(new RuntimeException("Unexpected socket timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.send("test@example.com", "Test Subject", "Test Body"));

        verify(appMetricsService, times(1)).incrementEmailDelivery("failed");
        verify(appMetricsService, never()).incrementEmailDelivery("success");
    }

    @Test
    @DisplayName("mailTaskExecutor bean is properly configured with bounds and naming prefix")
    void mailTaskExecutor_ConfigurationParameters() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.mailTaskExecutor();

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(5, executor.getMaxPoolSize());
        assertEquals(50, executor.getQueueCapacity());
        assertEquals("mail-exec-", executor.getThreadNamePrefix());

        executor.destroy();
    }

    @Test
    @DisplayName("CorrelationIdTaskDecorator propagates correlationId to task and cleans up thread MDC")
    void correlationIdTaskDecorator_PropagatesAndCleansMdc() throws Exception {
        AsyncConfig.CorrelationIdTaskDecorator decorator = new AsyncConfig.CorrelationIdTaskDecorator();

        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "test-corr-abc-123");

        AtomicReference<String> capturedInTask = new AtomicReference<>();
        AtomicReference<String> capturedAfterRunOnAsyncThread = new AtomicReference<>();

        Runnable task = decorator.decorate(() -> {
            capturedInTask.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        });

        // Run on a separate thread to simulate async executor worker
        Thread worker = new Thread(() -> {
            task.run();
            capturedAfterRunOnAsyncThread.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        });

        worker.start();
        worker.join(3000);

        assertEquals("test-corr-abc-123", capturedInTask.get(), "Task should receive caller correlationId");
        assertNull(capturedAfterRunOnAsyncThread.get(), "Worker thread MDC must be cleared after task execution");

        MDC.clear();
    }

    @Test
    @DisplayName("mailTaskExecutor saturation triggers RejectedExecutionException (AbortPolicy)")
    void mailTaskExecutor_SaturationRejection() throws Exception {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.mailTaskExecutor();

        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(2);

        try {
            // Core pool size = 2, max = 5, queue = 50.
            // 1) Submit 2 tasks to occupy core threads
            for (int i = 0; i < 2; i++) {
                executor.execute(() -> {
                    startedLatch.countDown();
                    try {
                        blockerLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                });
            }

            assertTrue(startedLatch.await(5, TimeUnit.SECONDS), "Core worker threads should start");

            // 2) Fill the queue (50 items)
            for (int i = 0; i < 50; i++) {
                executor.execute(() -> {
                    try {
                        blockerLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                });
            }

            // 3) Fill extra threads up to maxPoolSize (5 - 2 = 3 items)
            for (int i = 0; i < 3; i++) {
                executor.execute(() -> {
                    try {
                        blockerLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                });
            }

            // Total active + queued = 55. The 56th task must fail with RejectedExecutionException
            assertThrows(RejectedExecutionException.class, () -> {
                executor.execute(() -> {});
            }, "Executor should reject tasks when maxPoolSize and queueCapacity are saturated");

        } finally {
            blockerLatch.countDown();
            executor.destroy();
        }
    }
}
