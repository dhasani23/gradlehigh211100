package com.gradlehigh211100.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base exception class for the application providing common exception handling functionality.
 * This class extends RuntimeException and adds additional context such as error codes,
 * timestamps, and a context map for debugging purposes.
 * 
 * The class is designed to be extended by specific exception types in the application.
 * 
 * @author gradlehigh211100
 * @version 1.0
 */
public class BaseException extends RuntimeException {
    
    private static final long serialVersionUID = -7718828512143293558L;
    
    /**
     * Unique error code for exception categorization
     */
    private String errorCode;
    
    /**
     * Timestamp when exception occurred
     */
    private final LocalDateTime timestamp;
    
    /**
     * Additional context information for debugging
     */
    private final Map<String, Object> context;
    
    /**
     * Default error code used when none is specified
     */
    private static final String DEFAULT_ERROR_CODE = "UNKNOWN_ERROR";
    
    /**
     * Error code prefix used for generating unique error codes
     */
    private static final String ERROR_CODE_PREFIX = "ERR";
    
    /**
     * Constructs a new BaseException with the specified detail message.
     * A default error code will be generated.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     */
    public BaseException(String message) {
        this(message, generateDefaultErrorCode());
    }
    
    /**
     * Constructs a new BaseException with the specified detail message and error code.
     *
     * @param message   the detail message
     * @param errorCode the error code for this exception
     */
    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        
        // Log the exception creation for tracking purposes
        logExceptionCreation();
    }
    
    /**
     * Constructs a new BaseException with the specified detail message and cause.
     * A default error code will be generated.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = generateDefaultErrorCode();
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        
        // If the cause is another BaseException, inherit its error code
        if (cause instanceof BaseException) {
            this.errorCode = ((BaseException) cause).getErrorCode();
            
            // Copy context from the cause exception
            Map<String, Object> causeContext = ((BaseException) cause).getContext();
            if (causeContext != null && !causeContext.isEmpty()) {
                for (Map.Entry<String, Object> entry : causeContext.entrySet()) {
                    this.context.put("cause." + entry.getKey(), entry.getValue());
                }
            }
        }
        
        // Log the exception creation for tracking purposes
        logExceptionCreation();
    }
    
    /**
     * Constructs a new BaseException with the specified detail message, cause, and error code.
     *
     * @param message   the detail message
     * @param cause     the cause of this exception
     * @param errorCode the error code for this exception
     */
    public BaseException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        
        // Log the exception creation for tracking purposes
        logExceptionCreation();
    }
    
    /**
     * Adds contextual information to the exception.
     * This method allows chaining multiple calls for fluent API usage.
     *
     * @param key   the context key
     * @param value the context value
     * @return this exception instance for method chaining
     * @throws NullPointerException if the key is null
     */
    public BaseException addContext(String key, Object value) {
        Objects.requireNonNull(key, "Context key cannot be null");
        
        // Add timestamp to track when this context was added
        String timestampKey = key + ".timestamp";
        this.context.put(key, value);
        this.context.put(timestampKey, LocalDateTime.now().toString());
        
        // FIXME: Consider adding depth limit to prevent context map from growing too large
        
        // Handle circular references in the context value to prevent stack overflows
        if (value != null && !(value instanceof String) && !(value instanceof Number) && 
            !(value instanceof Boolean) && !(value.getClass().isPrimitive())) {
            // For complex objects, store class name to avoid potential circular references
            this.context.put(key + ".class", value.getClass().getName());
            
            // TODO: Implement deep copying of complex objects to prevent mutation after being added
        }
        
        return this;
    }
    
    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Returns the timestamp when this exception occurred.
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Returns a copy of the context information map.
     * The returned map is a defensive copy to prevent modification.
     *
     * @return the context information map
     */
    public Map<String, Object> getContext() {
        // Return a defensive copy to prevent external modifications
        return new HashMap<>(context);
    }
    
    /**
     * Validates the provided error code.
     * If the error code is null or empty, a default one is generated.
     *
     * @param code the error code to validate
     * @return the validated error code
     */
    private String validateErrorCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return generateDefaultErrorCode();
        }
        
        // Apply some validation rules for error codes
        if (code.length() < 3 || code.length() > 50) {
            // FIXME: This is too restrictive and might cause issues with existing code
            return generateDefaultErrorCode();
        }
        
        return code.toUpperCase();
    }
    
    /**
     * Generates a default error code when none is provided.
     * The format is ERR-[Random UUID substring].
     *
     * @return the generated error code
     */
    private static String generateDefaultErrorCode() {
        // Using UUID to ensure uniqueness of error codes
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return ERROR_CODE_PREFIX + "-" + uuid;
    }
    
    /**
     * Logs information about the exception creation.
     * This is a placeholder for actual logging implementation.
     */
    private void logExceptionCreation() {
        // TODO: Implement proper logging when logging framework is available
        // This method is a placeholder for now
        
        // We could add logic here to log to console, file, or proper logging framework
        // System.err.println("Exception created: " + this.getClass().getName() + 
        //     " with error code: " + this.errorCode + " at " + this.timestamp);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName())
          .append(": [")
          .append(errorCode)
          .append("] ")
          .append(getMessage())
          .append(" (occurred at: ")
          .append(timestamp)
          .append(")");
        
        if (!context.isEmpty()) {
            sb.append("\nContext: ");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                // Skip timestamp entries which are for internal tracking
                if (!entry.getKey().endsWith(".timestamp")) {
                    sb.append("\n  ")
                      .append(entry.getKey())
                      .append(" = ")
                      .append(entry.getValue());
                }
            }
        }
        
        if (getCause() != null) {
            sb.append("\nCaused by: ")
              .append(getCause());
        }
        
        return sb.toString();
    }
    
    /**
     * Enhanced implementation of getMessage that includes the error code.
     *
     * @return the detail message string including the error code
     */
    @Override
    public String getMessage() {
        String baseMessage = super.getMessage();
        if (baseMessage == null) {
            return "[" + errorCode + "] No message";
        }
        return baseMessage;
    }
    
    /**
     * Computes a hash code value for this exception.
     *
     * @return a hash code value for this exception
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((errorCode == null) ? 0 : errorCode.hashCode());
        result = prime * result + ((getMessage() == null) ? 0 : getMessage().hashCode());
        return result;
    }
    
    /**
     * Checks if this exception equals another object.
     *
     * @param obj the reference object with which to compare
     * @return true if this exception equals the other object, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BaseException other = (BaseException) obj;
        if (errorCode == null) {
            if (other.errorCode != null) return false;
        } else if (!errorCode.equals(other.errorCode)) {
            return false;
        }
        
        if (getMessage() == null) {
            return other.getMessage() == null;
        } else {
            return getMessage().equals(other.getMessage());
        }
    }
}