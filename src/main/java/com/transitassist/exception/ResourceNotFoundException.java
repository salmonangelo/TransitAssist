package com.transitassist.exception;

/** Thrown when a User / Helper / AssistanceRequest id does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
