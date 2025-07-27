package com.ecommerce.root.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A standardized API response object that provides a consistent format
 * for all API responses in the application.
 */
public class ApiResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Object data;
    private int status;
    private LocalDateTime timestamp;

    /**
     * Default constructor
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor for creating a basic API response
     *
     * @param success whether the operation was successful
     * @param message the response message
     * @param data the response payload data
     * @param status the HTTP status code
     */
    public ApiResponse(boolean success, String message, Object data, int status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Creates a successful response with data
     *
     * @param message success message
     * @param data response payload
     * @param status HTTP status code
     * @return a new ApiResponse instance
     */
    public static ApiResponse success(String message, Object data, int status) {
        return new ApiResponse(true, message, data, status);
    }

    /**
     * Creates an error response
     *
     * @param message error message
     * @param status HTTP status code
     * @return a new ApiResponse instance
     */
    public static ApiResponse error(String message, int status) {
        return new ApiResponse(false, message, null, status);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}