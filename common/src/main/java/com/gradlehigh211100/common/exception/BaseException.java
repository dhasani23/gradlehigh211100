package com.gradlehigh211100.common.exception;

/**
 * Base exception class for the application.
 * Provides common functionality for all custom exceptions.
 */
public class BaseException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new BaseException with the specified detail message.
     *
     * @param message the detail message
     */
    public BaseException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new BaseException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new BaseException with the specified cause.
     *
     * @param cause the cause
     */
    public BaseException(Throwable cause) {
        super(cause);
    }
}