package com.transitassist.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated.
 * Default status is 400 (Bad Request); use the second constructor for 409 (Conflict).
 */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessRuleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
