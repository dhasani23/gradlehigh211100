package com.ecommerce.root.service;

/**
 * Enumeration representing the possible health statuses for services
 * in the e-commerce platform.
 */
public enum HealthStatus {
    /**
     * Service is functioning properly with no issues detected
     */
    HEALTHY,
    
    /**
     * Service is available but experiencing performance issues or partial functionality
     */
    DEGRADED,
    
    /**
     * Service is completely unavailable or non-functional
     */
    DOWN,
    
    /**
     * Service health status could not be determined
     */
    UNKNOWN,
    
    /**
     * Service has critical issues that affect system functionality
     */
    CRITICAL
}