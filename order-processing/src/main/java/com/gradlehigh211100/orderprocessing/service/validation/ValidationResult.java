package com.gradlehigh211100.orderprocessing.service.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of validation operations.
 * Contains information about validation status and any validation errors.
 */
public class ValidationResult {
    
    private boolean valid;
    private List<String> errors;
    
    /**
     * Creates a new validation result.
     * 
     * @param valid whether the validation was successful
     */
    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
    }
    
    /**
     * Creates a new validation result.
     * 
     * @param valid whether the validation was successful
     * @param errors list of error messages
     */
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
    }
    
    /**
     * @return true if validation was successful, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Sets the validation status.
     * 
     * @param valid new validation status
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    /**
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Adds an error message to the validation result.
     * 
     * @param error the error message to add
     */
    public void addError(String error) {
        this.errors.add(error);
        if (this.valid) {
            this.valid = false;
        }
    }
    
    /**
     * Adds multiple error messages to the validation result.
     * 
     * @param errors the error messages to add
     */
    public void addErrors(List<String> errors) {
        this.errors.addAll(errors);
        if (this.valid && !errors.isEmpty()) {
            this.valid = false;
        }
    }
    
    /**
     * Creates a valid validation result with no errors.
     * 
     * @return a valid validation result
     */
    public static ValidationResult valid() {
        return new ValidationResult(true);
    }
    
    /**
     * Creates an invalid validation result with the specified error.
     * 
     * @param error the error message
     * @return an invalid validation result
     */
    public static ValidationResult invalid(String error) {
        ValidationResult result = new ValidationResult(false);
        result.addError(error);
        return result;
    }
    
    /**
     * Creates an invalid validation result with the specified errors.
     * 
     * @param errors the error messages
     * @return an invalid validation result
     */
    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}