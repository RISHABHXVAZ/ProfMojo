package com.profmojo.exception;

import lombok.Getter;

@Getter
public class DeliveryLockoutException extends RuntimeException {

    private final long retryAfterSeconds;

    public DeliveryLockoutException(String message) {
        this(message, 900L);
    }

    public DeliveryLockoutException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(retryAfterSeconds, 1L);
    }
}
