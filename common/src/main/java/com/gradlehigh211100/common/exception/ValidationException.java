package com.gradlehigh211100.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Specialized exception for validation errors with detailed field-level error information.
 * This exception can track both field-specific validation errors and global errors that
 * are not tied to specific fields.
 */
public class ValidationException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Map of field names to their validation error messages
     */
    private final Map<String, String> fieldErrors;
    
    /**
     * List of global validation errors not tied to specific fields
     */
    private final List<String> globalErrors;
    
    /**
     * Constructor with validation message.
     * 
     * @param message the validation error message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
        this.globalErrors = new ArrayList<>();
        
        // If a message was provided, add it as a global error
        if (message != null && !message.trim().isEmpty()) {
            this.globalErrors.add(message);
        }
    }
    
    /**
     * Constructor with field-specific errors.
     * 
     * @param fieldErrors map of field names to their validation error messages
     */
    public ValidationException(Map<String, String> fieldErrors) {
        super(buildMessageFromFieldErrors(fieldErrors));
        
        // Create a defensive copy of the provided field errors
        this.fieldErrors = new HashMap<>();
        if (fieldErrors != null) {
            this.fieldErrors.putAll(fieldErrors);
        }
        
        this.globalErrors = new ArrayList<>();
    }
    
    /**
     * Private constructor for building the exception through chain methods.
     * 
     * @param message the validation error message
     * @param fieldErrors map of field errors
     * @param globalErrors list of global errors
     */
    private ValidationException(String message, Map<String, String> fieldErrors, List<String> globalErrors) {
        super(message);
        this.fieldErrors = new HashMap<>(fieldErrors);
        this.globalErrors = new ArrayList<>(globalErrors);
    }
    
    /**
     * Build a meaningful error message from field errors.
     * 
     * @param fieldErrors map of field names to their validation error messages
     * @return a consolidated error message
     */
    private static String buildMessageFromFieldErrors(Map<String, String> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "Validation failed";
        }
        
        StringBuilder sb = new StringBuilder("Validation failed: ");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
            if (!first) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
    }
    
    /**
     * Adds a field-specific validation error.
     * 
     * @param field the field name
     * @param error the validation error message
     * @return this ValidationException instance for method chaining
     * @throws NullPointerException if field or error is null
     */
    public ValidationException addFieldError(String field, String error) {
        Objects.requireNonNull(field, "Field name cannot be null");
        Objects.requireNonNull(error, "Error message cannot be null");
        
        Map<String, String> newFieldErrors = new HashMap<>(this.fieldErrors);
        newFieldErrors.put(field, error);
        
        return new ValidationException(getMessage(), newFieldErrors, this.globalErrors);
    }
    
    /**
     * Adds a global validation error.
     * 
     * @param error the global validation error message
     * @return this ValidationException instance for method chaining
     * @throws NullPointerException if error is null
     */
    public ValidationException addGlobalError(String error) {
        Objects.requireNonNull(error, "Error message cannot be null");
        
        List<String> newGlobalErrors = new ArrayList<>(this.globalErrors);
        newGlobalErrors.add(error);
        
        return new ValidationException(getMessage(), this.fieldErrors, newGlobalErrors);
    }
    
    /**
     * Checks if there are field-specific errors.
     * 
     * @return true if there are field-specific errors, false otherwise
     */
    public boolean hasFieldErrors() {
        return !this.fieldErrors.isEmpty();
    }
    
    /**
     * Checks if there are global errors.
     * 
     * @return true if there are global errors, false otherwise
     */
    public boolean hasGlobalErrors() {
        return !this.globalErrors.isEmpty();
    }
    
    /**
     * Returns all field errors.
     * 
     * @return an unmodifiable map of field names to their validation error messages
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(this.fieldErrors);
    }
    
    /**
     * Returns all global errors.
     * 
     * @return an unmodifiable list of global validation errors
     */
    public List<String> getGlobalErrors() {
        return Collections.unmodifiableList(this.globalErrors);
    }
    
    /**
     * Returns a field error for a specific field.
     * 
     * @param field the field name
     * @return the validation error message for the specified field, or null if not found
     */
    public String getFieldError(String field) {
        return this.fieldErrors.get(field);
    }
    
    /**
     * Checks if the exception has any validation errors (field or global).
     * 
     * @return true if there are any validation errors, false otherwise
     */
    public boolean hasErrors() {
        return hasFieldErrors() || hasGlobalErrors();
    }
    
    /**
     * Returns the total count of errors (field errors plus global errors).
     * 
     * @return the total number of validation errors
     */
    public int getErrorCount() {
        return this.fieldErrors.size() + this.globalErrors.size();
    }
    
    /**
     * Creates a new ValidationException combining this exception with another one.
     * 
     * @param other the other ValidationException to merge with
     * @return a new ValidationException containing errors from both exceptions
     * @throws NullPointerException if other is null
     */
    public ValidationException mergeWith(ValidationException other) {
        Objects.requireNonNull(other, "Cannot merge with a null exception");
        
        Map<String, String> mergedFieldErrors = new HashMap<>(this.fieldErrors);
        mergedFieldErrors.putAll(other.fieldErrors);
        
        List<String> mergedGlobalErrors = new ArrayList<>(this.globalErrors);
        mergedGlobalErrors.addAll(other.globalErrors);
        
        return new ValidationException(getMessage(), mergedFieldErrors, mergedGlobalErrors);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ").append(getMessage()).append("\n");
        
        if (hasFieldErrors()) {
            sb.append("Field Errors:\n");
            for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        if (hasGlobalErrors()) {
            sb.append("Global Errors:\n");
            for (String error : globalErrors) {
                sb.append("  - ").append(error).append("\n");
            }
        }
        
        return sb.toString();
    }
}