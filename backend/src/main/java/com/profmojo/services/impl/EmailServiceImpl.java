package com.profmojo.services.impl;

import com.profmojo.config.AsyncConfig;
import com.profmojo.metrics.AppMetricsService;
import com.profmojo.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final AppMetricsService appMetricsService;

    @Override
    @Async(AsyncConfig.MAIL_EXECUTOR_BEAN)
    public void send(String to, String subject, String body) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Starting asynchronous email delivery on thread [{}] for subject=[{}]", threadName, subject);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Asynchronous email delivery succeeded in {}ms on thread [{}]", duration, threadName);
            appMetricsService.incrementEmailDelivery("success");
        } catch (MailException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Asynchronous email delivery failed after {}ms on thread [{}]: {}", duration, threadName, e.getMessage());
            appMetricsService.incrementEmailDelivery("failed");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during asynchronous email delivery after {}ms on thread [{}]: {}", duration, threadName, e.getMessage());
            appMetricsService.incrementEmailDelivery("failed");
        }
    }
}
