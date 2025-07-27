package com.gradlehigh211100.productcatalog.exception;

/**
 * Exception thrown when a category cannot be found in the system
 */
public class CategoryNotFoundException extends RuntimeException {
    
    public CategoryNotFoundException(String message) {
        super(message);
    }
    
    public CategoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}