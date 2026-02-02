package com.profmojo.services;

public interface EmailService {
    void send(String to, String subject, String body);
}
