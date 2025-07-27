package com.ecommerce.root.exception;

/**
 * Exception thrown for user service communication errors
 */
public class UserServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final ErrorCode errorCode;

    public UserServiceException(String message) {
        super(message);
        this.errorCode = ErrorCode.GENERAL_ERROR;
    }

    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.GENERAL_ERROR;
    }
    
    public UserServiceException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public UserServiceException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * Error codes for categorizing exceptions
     */
    public enum ErrorCode {
        GENERAL_ERROR(1000),
        CONNECTION_ERROR(1001),
        AUTHENTICATION_ERROR(1002),
        USER_NOT_FOUND(1003),
        VALIDATION_ERROR(1004),
        TIMEOUT_ERROR(1005),
        CIRCUIT_OPEN(1006),
        SERVICE_UNAVAILABLE(1007);
        
        private final int code;
        
        ErrorCode(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
    }
}