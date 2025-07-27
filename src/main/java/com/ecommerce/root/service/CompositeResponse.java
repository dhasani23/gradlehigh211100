package com.ecommerce.root.service;

import java.util.HashMap;
import java.util.Map;

/**
 * CompositeResponse - Response object for composite requests that aggregates data from multiple services.
 */
public class CompositeResponse {

    private String requestId;
    private String status;
    private String error;
    private Map<String, Object> results;

    /**
     * Default constructor initializes the results map.
     */
    public CompositeResponse() {
        this.results = new HashMap<>();
    }

    /**
     * Gets the request ID.
     * 
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the request ID.
     * 
     * @param requestId the request ID to set
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Gets the status.
     * 
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     * 
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the error message.
     * 
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     * 
     * @param error the error message to set
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the results map.
     * 
     * @return the results map
     */
    public Map<String, Object> getResults() {
        return results;
    }

    /**
     * Sets the results map.
     * 
     * @param results the results map to set
     */
    public void setResults(Map<String, Object> results) {
        this.results = results;
    }
    
    /**
     * Adds a result to the results map.
     * 
     * @param key the result key
     * @param value the result value
     */
    public void addResult(String key, Object value) {
        this.results.put(key, value);
    }
}