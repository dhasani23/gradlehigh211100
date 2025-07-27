package com.ecommerce.root.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard API response wrapper for consistent response format across all endpoints.
 * This class provides a uniform structure for all API responses in the application.
 * 
 * @author gradlehigh211100
 * @version 1.0
 */
public class ApiResponse {
    
    // Response status indicator
    private boolean success;
    
    // Response message providing additional information
    private String message;
    
    // Response payload containing the actual data
    private Object data;
    
    // Timestamp when the response was generated
    private long timestamp;
    
    // List of error messages if any validation or processing errors occurred
    private List<String> errors;
    
    /**
     * Default constructor.
     * Initializes an empty API response with current timestamp and empty error list.
     */
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
        this.errors = new ArrayList<>();
    }
    
    /**
     * Parameterized constructor.
     * Initializes API response with provided details and current timestamp.
     * 
     * @param success indicates if the request was successful
     * @param message response message
     * @param data response payload
     */
    public ApiResponse(boolean success, String message, Object data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    /**
     * Creates a successful response with provided data.
     * 
     * @param data response payload
     * @return successful API response
     */
    public static ApiResponse success(Object data) {
        return new ApiResponse(true, "Request processed successfully", data);
    }
    
    /**
     * Creates a successful response with provided data and custom message.
     * Added for additional flexibility in response creation.
     * 
     * @param message custom success message
     * @param data response payload
     * @return successful API response
     */
    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(true, message, data);
    }
    
    /**
     * Creates an error response with provided error message.
     * 
     * @param message error message
     * @return error API response
     */
    public static ApiResponse error(String message) {
        ApiResponse response = new ApiResponse(false, message, null);
        response.addError(message);
        return response;
    }
    
    /**
     * Creates an error response with provided error messages list.
     * Added for complex validation scenarios where multiple errors need to be reported.
     * 
     * @param message general error message
     * @param errors detailed error messages
     * @return error API response
     */
    public static ApiResponse error(String message, List<String> errors) {
        ApiResponse response = new ApiResponse(false, message, null);
        if (errors != null) {
            response.setErrors(errors);
        }
        return response;
    }
    
    /**
     * Adds an error message to the errors list.
     * Utility method to add individual error messages.
     * 
     * @param error error message to add
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
    
    /**
     * Adds multiple error messages to the errors list.
     * Utility method to add multiple error messages at once.
     * 
     * @param errors list of error messages to add
     */
    public void addErrors(List<String> errors) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }
    
    /**
     * Checks if the response contains any errors.
     * 
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return this.errors != null && !this.errors.isEmpty();
    }
    
    /**
     * Get the success status.
     * 
     * @return success status
     */
    public boolean getSuccess() {
        return success;
    }
    
    /**
     * Set the success status.
     * 
     * @param success the success status to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Get the response message.
     * 
     * @return response message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Set the response message.
     * 
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Get the response data.
     * 
     * @return response data
     */
    public Object getData() {
        return data;
    }
    
    /**
     * Set the response data.
     * 
     * @param data the data to set
     */
    public void setData(Object data) {
        this.data = data;
    }
    
    /**
     * Get the timestamp.
     * 
     * @return timestamp when the response was generated
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the timestamp.
     * 
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get the errors list.
     * 
     * @return list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Set the errors list.
     * 
     * @param errors the list of errors to set
     */
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    /**
     * Utility method to handle complex processing with multiple potential error conditions.
     * This method demonstrates high cyclomatic complexity by handling various conditional paths.
     * 
     * @param data input data to process
     * @param validationRules validation rules to apply
     * @return processed API response
     */
    public static ApiResponse processComplexRequest(Object data, List<String> validationRules) {
        ApiResponse response = new ApiResponse();
        
        // Complex error handling and validation logic
        if (data == null) {
            response.setSuccess(false);
            response.setMessage("Data cannot be null");
            response.addError("NULL_DATA_ERROR");
            return response;
        }
        
        // Simulated complex validation with multiple branches
        List<String> validationErrors = new ArrayList<>();
        
        if (validationRules != null && !validationRules.isEmpty()) {
            for (String rule : validationRules) {
                if (rule == null || rule.trim().isEmpty()) {
                    validationErrors.add("Invalid validation rule: empty rule");
                    continue;
                }
                
                // Complex rule parsing and handling
                String[] ruleParts = rule.split(":");
                if (ruleParts.length < 2) {
                    validationErrors.add("Invalid rule format: " + rule);
                    continue;
                }
                
                String ruleType = ruleParts[0].trim();
                String ruleValue = ruleParts[1].trim();
                
                // Different branches based on rule type
                switch (ruleType) {
                    case "minLength":
                        try {
                            int minLength = Integer.parseInt(ruleValue);
                            if (data instanceof String && ((String)data).length() < minLength) {
                                validationErrors.add("String length less than required minimum: " + minLength);
                            }
                        } catch (NumberFormatException e) {
                            validationErrors.add("Invalid minLength value: " + ruleValue);
                        }
                        break;
                    case "maxLength":
                        try {
                            int maxLength = Integer.parseInt(ruleValue);
                            if (data instanceof String && ((String)data).length() > maxLength) {
                                validationErrors.add("String length greater than allowed maximum: " + maxLength);
                            }
                        } catch (NumberFormatException e) {
                            validationErrors.add("Invalid maxLength value: " + ruleValue);
                        }
                        break;
                    case "required":
                        boolean isRequired = Boolean.parseBoolean(ruleValue);
                        if (isRequired) {
                            if (data instanceof String && ((String)data).trim().isEmpty()) {
                                validationErrors.add("Required field is empty");
                            }
                        }
                        break;
                    case "range":
                        if (data instanceof Number) {
                            String[] rangeParts = ruleValue.split("-");
                            if (rangeParts.length != 2) {
                                validationErrors.add("Invalid range format: " + ruleValue);
                                continue;
                            }
                            
                            try {
                                double min = Double.parseDouble(rangeParts[0]);
                                double max = Double.parseDouble(rangeParts[1]);
                                double value = ((Number)data).doubleValue();
                                
                                if (value < min || value > max) {
                                    validationErrors.add("Value outside allowed range (" + min + "-" + max + "): " + value);
                                }
                            } catch (NumberFormatException e) {
                                validationErrors.add("Invalid range values: " + ruleValue);
                            }
                        } else {
                            validationErrors.add("Range validation only applicable to numeric values");
                        }
                        break;
                    case "pattern":
                        if (data instanceof String) {
                            try {
                                if (!((String)data).matches(ruleValue)) {
                                    validationErrors.add("Value doesn't match required pattern: " + ruleValue);
                                }
                            } catch (Exception e) {
                                validationErrors.add("Invalid regex pattern: " + ruleValue);
                            }
                        } else {
                            validationErrors.add("Pattern validation only applicable to string values");
                        }
                        break;
                    default:
                        validationErrors.add("Unknown validation rule type: " + ruleType);
                }
            }
        }
        
        // FIXME: Handle more complex data type validations beyond String and Number
        
        // TODO: Implement custom domain-specific validation rules
        
        if (!validationErrors.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Validation failed");
            response.addErrors(validationErrors);
            return response;
        }
        
        // Process the data if validation passes
        try {
            // Simulate data processing
            response.setSuccess(true);
            response.setMessage("Data processed successfully");
            response.setData(data); // In real implementation, would transform the data
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error processing data: " + e.getMessage());
            response.addError("PROCESSING_ERROR: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", errors=" + errors +
                '}';
    }
}