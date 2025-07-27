package com.gradlehigh211100.productcatalog.exception;

/**
 * Exception thrown when a category operation fails
 */
public class CategoryOperationException extends RuntimeException {
    
    public CategoryOperationException(String message) {
        super(message);
    }
    
    public CategoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}