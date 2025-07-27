package com.gradlehigh211100.common.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Standardized API response wrapper providing consistent response format across all endpoints.
 * This class encapsulates the common response pattern used throughout the application,
 * ensuring uniform error handling and successful response construction.
 *
 * @param <T> The type of data payload for successful responses
 */
public class ApiResponse<T> {
    
    private final boolean success;
    private final String message;
    private final T data;
    private final Map<String, String> errors;
    private final LocalDateTime timestamp;
    private final int statusCode;
    
    /**
     * Private constructor to enforce using factory methods for object creation.
     * This ensures all ApiResponse objects are created with a consistent state.
     *
     * @param success    Whether the operation was successful
     * @param message    Human-readable message about the operation result
     * @param data       Generic data payload for successful responses
     * @param errors     Field-specific error messages for validation failures
     * @param statusCode HTTP status code
     */
    private ApiResponse(boolean success, String message, T data, Map<String, String> errors, int statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        // Defensive copy to prevent modification of the errors map
        this.errors = errors != null ? new HashMap<>(errors) : new HashMap<>();
        this.timestamp = LocalDateTime.now();
        this.statusCode = statusCode;
    }
    
    /**
     * Creates a successful response with data.
     *
     * @param data The data payload
     * @param <T>  The type of the data payload
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }
    
    /**
     * Creates a successful response with data and a custom message.
     *
     * @param data    The data payload
     * @param message Custom success message
     * @param <T>     The type of the data payload
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null, 200);
    }
    
    /**
     * Creates an error response with a message.
     *
     * @param message Error message
     * @param <T>     The type of the data payload (will be null)
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, 400);
    }
    
    /**
     * Creates an error response with field-specific errors.
     *
     * @param errors Field-specific error messages
     * @param <T>    The type of the data payload (will be null)
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> error(Map<String, String> errors) {
        return new ApiResponse<>(false, "Validation failed", null, errors, 400);
    }
    
    /**
     * Advanced factory method to create a custom error response with specified status code.
     * This allows for more granular control over the response for different error scenarios.
     *
     * @param message    Error message
     * @param statusCode HTTP status code
     * @param <T>        The type of the data payload (will be null)
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return new ApiResponse<>(false, message, null, null, statusCode);
    }
    
    /**
     * Advanced factory method to create a custom error response with errors map and specified status code.
     *
     * @param message    Error message
     * @param errors     Field-specific error messages
     * @param statusCode HTTP status code
     * @param <T>        The type of the data payload (will be null)
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message, Map<String, String> errors, int statusCode) {
        return new ApiResponse<>(false, message, null, errors, statusCode);
    }
    
    /**
     * Checks if response indicates success.
     *
     * @return true if the response is successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Checks if response has validation errors.
     *
     * @return true if the response has validation errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Returns response data.
     *
     * @return The data payload
     */
    public T getData() {
        return data;
    }
    
    /**
     * Returns response message.
     *
     * @return Human-readable message about the operation result
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Returns validation errors.
     *
     * @return Field-specific error messages
     */
    public Map<String, String> getErrors() {
        // Return immutable map to prevent modification
        return Collections.unmodifiableMap(errors);
    }
    
    /**
     * Returns response timestamp.
     *
     * @return The timestamp when this response was created
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Returns HTTP status code.
     *
     * @return The HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Add an error to the errors map.
     * This is useful for incrementally building error responses.
     *
     * @param field       The field name that has an error
     * @param errorMessage The error message for the field
     * @return This ApiResponse instance for method chaining
     */
    public ApiResponse<T> addError(String field, String errorMessage) {
        if (field != null && errorMessage != null) {
            this.errors.put(field, errorMessage);
        }
        return this;
    }
    
    /**
     * Merges errors from another error response.
     * Useful when aggregating validation errors from multiple sources.
     *
     * @param other Another ApiResponse containing errors
     * @return This ApiResponse instance for method chaining
     */
    public ApiResponse<T> mergeErrors(ApiResponse<?> other) {
        if (other != null && other.hasErrors()) {
            this.errors.putAll(other.getErrors());
        }
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiResponse<?> that = (ApiResponse<?>) o;
        return success == that.success &&
               statusCode == that.statusCode &&
               Objects.equals(message, that.message) &&
               Objects.equals(data, that.data) &&
               Objects.equals(errors, that.errors);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, message, data, errors, statusCode);
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
               "success=" + success +
               ", message='" + message + '\'' +
               ", data=" + data +
               ", errors=" + errors +
               ", timestamp=" + timestamp +
               ", statusCode=" + statusCode +
               '}';
    }
    
    /**
     * TODO: Add support for internationalized messages
     */
    
    /**
     * FIXME: Handle nested validation errors for complex object hierarchies
     */
}