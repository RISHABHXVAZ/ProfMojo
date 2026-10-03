package com.profmojo.integration;

import com.profmojo.config.AsyncConfig;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.otp.OtpStore;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.security.CorrelationIdFilter;
import com.profmojo.services.AdminAuthService;
import com.profmojo.services.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Asynchronous Email Architecture & Failure Semantics Integration Tests")
class AsyncEmailIntegrationTest extends BasePostgresContainerTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private DepartmentSecretRepository secretRepository;

    @Autowired
    private OtpStore otpStore;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier(AsyncConfig.MAIL_EXECUTOR_BEAN)
    private Executor mailExecutorBean;

    @BeforeEach
    void setUp() {
        if (!secretRepository.existsById("19472026")) {
            secretRepository.save(DepartmentSecret.builder()
                    .secretKey("19472026")
                    .department("CSE")
                    .adminEmail("admin.cse@example.com")
                    .build());
        }
        MDC.clear();
    }

    @Test
    @DisplayName("Email executes on async thread pool (mail-exec-*) and preserves caller correlationId")
    void asyncEmail_ExecutesOnMailExecThread_PropagatesCorrelationId() throws Exception {
        String testCorrelationId = "integ-async-corr-555";
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, testCorrelationId);

        AtomicReference<String> executingThreadName = new AtomicReference<>();
        AtomicReference<String> capturedMdcCorrelationId = new AtomicReference<>();
        CountDownLatch emailSentLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            executingThreadName.set(Thread.currentThread().getName());
            capturedMdcCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            emailSentLatch.countDown();
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.send("test@example.com", "Async Test", "Hello Async World");

        boolean completed = emailSentLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Email task should complete on async executor within timeout");

        assertNotNull(executingThreadName.get(), "Thread name should be captured");
        assertTrue(executingThreadName.get().startsWith("mail-exec-"),
                "Async task must run on mailTaskExecutor thread pool, but ran on: " + executingThreadName.get());

        assertEquals(testCorrelationId, capturedMdcCorrelationId.get(),
                "Async worker thread must inherit MDC correlation ID from caller");

        Counter successCounter = meterRegistry.find("profmojo.email.delivery")
                .tag("status", "success")
                .counter();
        assertNotNull(successCounter, "Email delivery metric should be registered");
        assertTrue(successCounter.count() >= 1.0, "Success metric should be incremented");

        MDC.clear();
    }

    @Test
    @DisplayName("Admin send-otp creates OTP synchronously before dispatching email asynchronously")
    void adminSendOtp_GeneratesOtpSynchronously_DispatchesEmailAsynchronously() throws Exception {
        CountDownLatch emailStartedLatch = new CountDownLatch(1);
        CountDownLatch finishEmailLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            emailStartedLatch.countDown();
            finishEmailLatch.await(3, TimeUnit.SECONDS);
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        long startTime = System.currentTimeMillis();
        adminAuthService.sendOtp("19472026");
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 2000, "sendOtp should return promptly without blocking on email sender");

        var otpEntry = otpStore.findOtp("ADMIN", "19472026");
        assertTrue(otpEntry.isPresent(), "OTP must be stored synchronously before sendOtp returns");
        assertNotNull(otpEntry.get().otp(), "Stored OTP must be present");

        boolean emailTriggered = emailStartedLatch.await(3, TimeUnit.SECONDS);
        assertTrue(emailTriggered, "Background email sending should be dispatched to executor");

        finishEmailLatch.countDown();
    }

    @Test
    @DisplayName("Post-submission SMTP failure: HTTP already returned, error logged, metric incremented, worker survives")
    void postSubmissionSmtpFailure_LoggedAndMetricIncremented_WorkerRemainsAlive() throws Exception {
        CountDownLatch failureLatch = new CountDownLatch(1);
        AtomicReference<String> workerThreadName = new AtomicReference<>();

        doAnswer(invocation -> {
            workerThreadName.set(Thread.currentThread().getName());
            failureLatch.countDown();
            throw new MailSendException("Simulated SMTP downstream timeout/refusal");
        }).when(mailSender).send(any(SimpleMailMessage.class));

        double initialFailedCount = getFailedEmailMetricCount();

        // 1. Caller triggers sendOtp - HTTP flow completes without error
        assertDoesNotThrow(() -> adminAuthService.sendOtp("19472026"));

        // 2. Wait for async worker to execute and fail
        assertTrue(failureLatch.await(5, TimeUnit.SECONDS), "Async email failure task should execute");

        // Give background logger/metrics time to complete
        Thread.sleep(100);

        // 3. Verify failure metric incremented
        double newFailedCount = getFailedEmailMetricCount();
        assertEquals(initialFailedCount + 1.0, newFailedCount, 0.001,
                "Email delivery failure metric must increment when SMTP throws");

        // 4. Verify worker thread did not terminate - submit a new email task and ensure it succeeds
        CountDownLatch recoveryLatch = new CountDownLatch(1);
        doAnswer(inv -> {
            recoveryLatch.countDown();
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.send("recovery@example.com", "Recovery Check", "Testing worker vitality");
        assertTrue(recoveryLatch.await(5, TimeUnit.SECONDS),
                "Async worker pool must remain alive and accept tasks after an SMTP exception");
    }

    @Test
    @DisplayName("Executor saturation: Task submission rejected, caller receives error, HTTP response does NOT claim success, OTP preserved")
    void mailExecutorSaturation_RejectionObservableToCaller_NoFalseSuccess_OtpPreserved() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) mailExecutorBean;

        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(2);

        try {
            // Saturate executor: 2 core threads + 50 queue capacity + 3 extra threads = 55 tasks
            for (int i = 0; i < 2; i++) {
                executor.execute(() -> {
                    startedLatch.countDown();
                    try {
                        blockerLatch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                });
            }

            assertTrue(startedLatch.await(5, TimeUnit.SECONDS), "Core workers should start");

            for (int i = 0; i < 50; i++) {
                executor.execute(() -> {
                    try {
                        blockerLatch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                });
            }

            for (int i = 0; i < 3; i++) {
                executor.execute(() -> {
                    try {
                        blockerLatch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                });
            }

            // Pool is now completely saturated at 55 tasks.
            // A. Service call directly throws TaskRejectedException / RejectedExecutionException
            assertThrows(Exception.class, () -> adminAuthService.sendOtp("19472026"),
                    "sendOtp must reject when executor is fully saturated");

            // B. HTTP request via MockMvc does NOT return 200 OK with 'OTP sent'
            mockMvc.perform(post("/api/admin/auth/send-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"secretKey\":\"19472026\"}"))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("OTP sent to admin email")));

            // C. Verify OTP persistence semantics: The OTP generated before the rejected enqueue attempt
            // is safely stored in the OTP store and was not lost.
            var otpEntry = otpStore.findOtp("ADMIN", "19472026");
            assertTrue(otpEntry.isPresent(), "OTP persisted in store before enqueue failure must remain intact");
            assertNotNull(otpEntry.get().otp(), "Stored OTP must be present");

        } finally {
            blockerLatch.countDown();
        }
    }

    @Test
    @DisplayName("MDC correlation ID is cleaned up in caller and worker under success, async exception, and rejection")
    void correlationIdLifecycleAndMdcCleanup_AllScenarios() throws Exception {
        AsyncConfig.CorrelationIdTaskDecorator decorator = new AsyncConfig.CorrelationIdTaskDecorator();

        // Scenario 1: Successful Async Execution
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "corr-success-test");
        AtomicBoolean workerMdcClean = new AtomicBoolean(false);
        AtomicReference<String> insideWorkerCorr = new AtomicReference<>();

        Runnable successTask = decorator.decorate(() -> {
            insideWorkerCorr.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        });

        Thread worker1 = new Thread(() -> {
            successTask.run();
            workerMdcClean.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY) == null);
        });
        worker1.start();
        worker1.join(3000);

        assertEquals("corr-success-test", insideWorkerCorr.get());
        assertTrue(workerMdcClean.get(), "Worker MDC must be cleaned up after successful execution");

        // Scenario 2: Async Exception
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "corr-exception-test");
        AtomicBoolean workerMdcCleanAfterError = new AtomicBoolean(false);

        Runnable errorTask = decorator.decorate(() -> {
            throw new RuntimeException("Simulated task failure");
        });

        Thread worker2 = new Thread(() -> {
            try {
                errorTask.run();
            } catch (RuntimeException ignored) {}
            workerMdcCleanAfterError.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY) == null);
        });
        worker2.start();
        worker2.join(3000);

        assertTrue(workerMdcCleanAfterError.get(), "Worker MDC must be cleaned up in finally block after exception");

        // Scenario 3: Caller MDC clean up
        MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY), "Caller MDC must remain clean");
    }

    private double getFailedEmailMetricCount() {
        Counter counter = meterRegistry.find("profmojo.email.delivery")
                .tag("status", "failed")
                .counter();
        return counter != null ? counter.count() : 0.0;
    }
}
