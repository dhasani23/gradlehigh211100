package com.gradlehigh211100.userservice.exception;

/**
 * Exception thrown for user preference-related errors
 */
public class UserPreferenceException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public UserPreferenceException(String message) {
        super(message);
    }
    
    public UserPreferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}