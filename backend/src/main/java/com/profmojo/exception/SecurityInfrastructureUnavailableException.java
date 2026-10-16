package com.profmojo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class SecurityInfrastructureUnavailableException extends RuntimeException {

    public SecurityInfrastructureUnavailableException(String message) {
        super(message);
    }

    public SecurityInfrastructureUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
