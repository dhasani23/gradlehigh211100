package com.gradlehigh211100.common.exception;

/**
 * Base exception class for the application.
 * Serves as a parent class for all application-specific exceptions.
 */
public class BaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new BaseException with null as its detail message.
     */
    public BaseException() {
        super();
    }

    /**
     * Constructs a new BaseException with the specified detail message.
     *
     * @param message the detail message
     */
    public BaseException(String message) {
        super(message);
    }

    /**
     * Constructs a new BaseException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public BaseException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new BaseException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}