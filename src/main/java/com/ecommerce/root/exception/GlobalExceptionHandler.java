package com.ecommerce.root.exception;

/**
 * Global exception handler
 */
public class GlobalExceptionHandler {

    /**
     * Error response class
     */
    public static class ApiResponse {
        private String message;
        
        public ApiResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Handles validation exceptions
     *
     * @param ex The exception
     * @return The API response
     */
    public ApiResponse handleValidationException(Exception ex) {
        return new ApiResponse(ex.getMessage());
    }
}