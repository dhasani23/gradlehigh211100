package com.ecommerce.root.service;

import java.util.List;

/**
 * Service interface for checking system health and dependencies
 */
public interface HealthService {

    /**
     * Check health of a specified service
     * 
     * @param serviceName the service to check
     * @return the health status of the service
     */
    com.ecommerce.root.controller.HealthController.HealthStatus checkServiceHealth(String serviceName);
    
    /**
     * Check if a service is supported for health checks
     * 
     * @param serviceName the service to check
     * @return true if the service is supported
     */
    boolean isServiceSupported(String serviceName);
    
    /**
     * Check if the auth service fallback is available
     * 
     * @return true if fallback is available
     */
    boolean isAuthServiceFallbackAvailable();
    
    /**
     * Check if database connection is established
     * 
     * @return true if database is connected
     */
    boolean isDatabaseConnected();
    
    /**
     * Get number of active database connections
     * 
     * @return the number of active connections
     */
    int getActiveDatabaseConnections();
    
    /**
     * Get maximum number of database connections
     * 
     * @return the maximum connections allowed
     */
    int getMaxDatabaseConnections();
    
    /**
     * Get database query response time
     * 
     * @return response time in milliseconds
     */
    long getDatabaseResponseTime();
    
    /**
     * Check if cache connection is established
     * 
     * @return true if cache is connected
     */
    boolean isCacheConnected();
    
    /**
     * Get cache response time
     * 
     * @return response time in milliseconds
     */
    long getCacheResponseTime();
    
    /**
     * Get cache hit rate percentage
     * 
     * @return hit rate as percentage (0-100)
     */
    int getCacheHitRate();
    
    /**
     * Check if message broker connection is established
     * 
     * @return true if message broker is connected
     */
    boolean isMessageBrokerConnected();
    
    /**
     * Get message queue backlog count
     * 
     * @return number of messages in backlog
     */
    int getMessageQueueBacklog();
    
    /**
     * Check if auth service is available
     * 
     * @return true if auth service is available
     */
    boolean isAuthServiceAvailable();
    
    /**
     * Get auth service response time
     * 
     * @return response time in milliseconds
     */
    long getAuthServiceResponseTime();
    
    /**
     * Check if auth service certificate is valid
     * 
     * @return true if certificate is valid
     */
    boolean isAuthServiceCertificateValid();
    
    /**
     * Get list of registered external APIs
     * 
     * @return list of API names
     */
    List<String> getRegisteredExternalApis();
    
    /**
     * Check if external API is available
     * 
     * @param apiName the API to check
     * @return true if API is available
     */
    boolean isExternalApiAvailable(String apiName);
    
    /**
     * Get external API response time
     * 
     * @param apiName the API to check
     * @return response time in milliseconds
     */
    long getExternalApiResponseTime(String apiName);
    
    /**
     * Get number of active threads
     * 
     * @return number of active threads
     */
    int getActiveThreadCount();
    
    /**
     * Get maximum thread count
     * 
     * @return maximum number of threads
     */
    int getMaxThreadCount();
    
    /**
     * Check if database query can be executed
     * 
     * @return true if query can be executed
     */
    boolean canExecuteDatabaseQuery();
    
    /**
     * Check if fallback cache mode is enabled
     * 
     * @return true if fallback mode is enabled
     */
    boolean isFallbackCacheModeEnabled();
}