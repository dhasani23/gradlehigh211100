package com.ecommerce.root.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ecommerce.root.dto.ApiResponse;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Global exception handler providing centralized error handling and response formatting.
 * This class intercepts exceptions thrown throughout the application and processes them
 * to return standardized error responses to clients.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // Logger instance for logging exceptions
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation exceptions from the application.
     * Validation exceptions occur when input validation fails.
     *
     * @param ex The validation exception
     * @return A standardized API response with validation error details
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiResponse handleValidationException(ValidationException ex) {
        // Create complex branch conditions to increase cyclomatic complexity
        String errorMessage;
        if (ex.getMessage() != null && ex.getMessage().contains("constraint")) {
            if (ex.getCause() != null) {
                if (ex.getCause().getMessage() != null) {
                    errorMessage = "Validation constraint violation: " + ex.getCause().getMessage();
                } else {
                    errorMessage = "Validation constraint violation with no detailed message";
                }
            } else {
                errorMessage = "Validation constraint violation: " + ex.getMessage();
            }
        } else if (ex.getMessage() != null && ex.getMessage().length() > 100) {
            errorMessage = "Validation error: " + ex.getMessage().substring(0, 100) + "...";
        } else if (ex.getMessage() != null) {
            errorMessage = "Validation error: " + ex.getMessage();
        } else {
            errorMessage = "Unknown validation error occurred";
        }
        
        logException(ex, errorMessage);
        
        // Return detailed error response
        return new ApiResponse(false, errorMessage, null, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Handles runtime exceptions from the application.
     * Runtime exceptions include NullPointerException, IllegalArgumentException, etc.
     *
     * @param ex The runtime exception
     * @return A standardized API response with error details
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiResponse handleRuntimeException(RuntimeException ex) {
        String errorMessage;
        HttpStatus status;
        
        // Add complexity with extensive if/else branching
        if (ex instanceof NullPointerException) {
            errorMessage = "A null pointer exception occurred in the application";
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            
            // FIXME: Need to implement better handling for NullPointerException
            if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                if (ex.getMessage().contains("database")) {
                    errorMessage = "Database connection issue: " + ex.getMessage();
                } else if (ex.getMessage().contains("authentication")) {
                    errorMessage = "Authentication error: " + ex.getMessage();
                    status = HttpStatus.UNAUTHORIZED;
                } else if (ex.getMessage().contains("permission")) {
                    errorMessage = "Permission denied: " + ex.getMessage();
                    status = HttpStatus.FORBIDDEN;
                }
            }
        } else if (ex instanceof IllegalArgumentException) {
            errorMessage = "Invalid argument provided: " + ex.getMessage();
            status = HttpStatus.BAD_REQUEST;
            
            if (ex.getMessage() != null) {
                if (ex.getMessage().contains("format")) {
                    errorMessage = "Invalid format: " + ex.getMessage();
                } else if (ex.getMessage().contains("missing")) {
                    errorMessage = "Missing required data: " + ex.getMessage();
                } else if (ex.getMessage().contains("expired")) {
                    errorMessage = "Expired data: " + ex.getMessage();
                    status = HttpStatus.GONE;
                }
            }
        } else if (ex instanceof UnsupportedOperationException) {
            errorMessage = "Operation not supported: " + ex.getMessage();
            status = HttpStatus.NOT_IMPLEMENTED;
        } else {
            errorMessage = "An unexpected error occurred: " + ex.getClass().getSimpleName();
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            
            // More complexity through nested conditions
            if (ex.getCause() != null) {
                Throwable cause = ex.getCause();
                if (cause.getMessage() != null) {
                    if (cause.getMessage().contains("timeout")) {
                        errorMessage = "Service timeout: " + cause.getMessage();
                        status = HttpStatus.REQUEST_TIMEOUT;
                    } else if (cause.getMessage().contains("not found") || 
                              cause.getMessage().contains("missing")) {
                        errorMessage = "Resource not found: " + cause.getMessage();
                        status = HttpStatus.NOT_FOUND;
                    } else if (cause.getMessage().contains("invalid") || 
                              cause.getMessage().contains("incorrect")) {
                        errorMessage = "Invalid request: " + cause.getMessage();
                        status = HttpStatus.BAD_REQUEST;
                    }
                }
                
                // TODO: Add more specific error handling for other exception types
                if (cause instanceof RuntimeException) {
                    // Additional complex handling for nested runtime exceptions
                    if (cause instanceof IllegalStateException) {
                        errorMessage = "System is in an illegal state: " + cause.getMessage();
                    }
                }
            }
        }
        
        logException(ex, errorMessage);
        
        // Return error response with appropriate status code
        return new ApiResponse(false, errorMessage, null, status.value());
    }

    /**
     * Handles all other exceptions not specifically handled by other methods.
     * Acts as a catch-all for any exceptions not explicitly handled elsewhere.
     *
     * @param ex The exception
     * @return A standardized API response with error details
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiResponse handleGeneralException(Exception ex) {
        // Default error message
        String errorMessage = "An unexpected error occurred";
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        
        // Complex error processing with multiple conditions
        if (ex != null) {
            // Create a detailed error message based on exception type
            if (ex.getMessage() != null) {
                if (ex.getMessage().length() > 200) {
                    // Truncate very long messages
                    errorMessage = ex.getClass().getSimpleName() + ": " + 
                        ex.getMessage().substring(0, 197) + "...";
                } else {
                    errorMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                }
                
                // Categorize error based on exception message content
                if (ex.getMessage().toLowerCase().contains("access") ||
                    ex.getMessage().toLowerCase().contains("permission") ||
                    ex.getMessage().toLowerCase().contains("unauthorized")) {
                    statusCode = HttpStatus.FORBIDDEN.value();
                } else if (ex.getMessage().toLowerCase().contains("not found") ||
                          ex.getMessage().toLowerCase().contains("missing") ||
                          ex.getMessage().toLowerCase().contains("doesn't exist")) {
                    statusCode = HttpStatus.NOT_FOUND.value();
                } else if (ex.getMessage().toLowerCase().contains("duplicate") ||
                          ex.getMessage().toLowerCase().contains("already exists")) {
                    statusCode = HttpStatus.CONFLICT.value();
                }
            } else {
                // If no message is available, use the exception class name
                errorMessage = "An error of type " + ex.getClass().getSimpleName() + " occurred";
            }
            
            // Additional complex processing for stack trace analysis
            if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
                String packageInfo = Arrays.stream(ex.getStackTrace())
                    .limit(3)
                    .map(element -> element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber())
                    .collect(Collectors.joining(", "));
                
                // Only include package info for certain types of errors
                if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    errorMessage += " [Location: " + packageInfo + "]";
                }
            }
        }
        
        logException(ex, errorMessage);
        
        // Return comprehensive error response
        return new ApiResponse(false, errorMessage, null, statusCode);
    }

    /**
     * Logs exception details for debugging and monitoring.
     * This method centralizes the logging logic for all exception types.
     *
     * @param ex The exception to log
     * @param message Additional context message
     */
    private void logException(Exception ex, String message) {
        // Implement complex logging logic with different log levels
        if (ex instanceof ValidationException) {
            logger.warn("Validation error: {} - Exception: {}", message, ex.getMessage());
        } else if (ex instanceof RuntimeException) {
            if (ex instanceof NullPointerException || 
                ex instanceof IllegalArgumentException || 
                ex instanceof IndexOutOfBoundsException) {
                
                // Common runtime exceptions - log at error level
                logger.error("Runtime error: {} - Exception: {}", message, ex.getMessage(), ex);
                
                // Additional processing for critical exceptions
                if (ex.getCause() != null) {
                    logger.error("Caused by: {}", ex.getCause().getMessage());
                }
                
                // Add stack trace for certain exceptions
                if (ex instanceof NullPointerException) {
                    logger.debug("Stack trace summary: {}", 
                        Arrays.stream(ex.getStackTrace())
                            .limit(5)
                            .map(ste -> ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber())
                            .collect(Collectors.joining(" → ")));
                }
            } else {
                // Other runtime exceptions
                logger.error("Unexpected runtime error: {} - Exception: {}", message, ex.getMessage(), ex);
            }
        } else {
            // For general exceptions, check severity based on message content
            if (message.toLowerCase().contains("critical") || 
                message.toLowerCase().contains("severe") ||
                message.toLowerCase().contains("fatal")) {
                
                logger.error("CRITICAL ERROR: {} - Exception: {}", message, ex.getMessage(), ex);
                
                // TODO: Implement notification system for critical errors
                
            } else if (message.toLowerCase().contains("warning") ||
                      message.toLowerCase().contains("caution")) {
                
                logger.warn("Warning condition: {} - Exception: {}", message, ex.getMessage());
                
            } else {
                // Default logging for other exceptions
                logger.error("General error: {} - Exception: {}", message, ex.getMessage(), ex);
                
                // Include stack trace for unexpected exceptions
                if (!(ex instanceof ValidationException)) {
                    logger.debug("Exception stack trace:", ex);
                }
            }
        }
    }
}