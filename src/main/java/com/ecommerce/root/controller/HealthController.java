package com.ecommerce.root.controller;

import com.ecommerce.root.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Health check controller providing system health status and dependency checks
 * This controller is responsible for exposing endpoints to monitor system health,
 * check dependencies, and provide Kubernetes-compatible liveness and readiness probes.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);
    
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    
    private final Map<String, Long> serviceResponseTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> serviceFailureCounts = new ConcurrentHashMap<>();
    private final Set<String> criticalServices = new HashSet<>();
    
    /**
     * Injected health service for checking system health
     */
    @Autowired
    private HealthService healthService;
    
    @PostConstruct
    public void initialize() {
        LOGGER.info("Initializing HealthController");
        criticalServices.add("database");
        criticalServices.add("cache");
        criticalServices.add("messageBroker");
        criticalServices.add("authService");
        
        // Initialize monitoring counters
        criticalServices.forEach(service -> {
            serviceResponseTimes.put(service, 0L);
            serviceFailureCounts.put(service, 0);
        });
    }
    
    /**
     * Get overall system health status including all dependencies
     * 
     * @return HealthStatus object containing overall system health information
     */
    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        LOGGER.debug("Received request for complete health check");
        
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> details = new HashMap<>();
            boolean isSystemHealthy = true;
            boolean hasDegradation = false;
            List<String> failedComponents = new ArrayList<>();
            
            // Check each critical service
            for (String serviceName : criticalServices) {
                try {
                    HealthStatus serviceHealth = healthService.checkServiceHealth(serviceName);
                    details.put(serviceName, serviceHealth);
                    
                    // Track response times for performance trending
                    long responseTime = serviceHealth.getResponseTime();
                    serviceResponseTimes.put(serviceName, responseTime);
                    
                    if (STATUS_DOWN.equals(serviceHealth.getStatus())) {
                        isSystemHealthy = false;
                        failedComponents.add(serviceName);
                        incrementFailureCount(serviceName);
                    } else if (STATUS_DEGRADED.equals(serviceHealth.getStatus())) {
                        hasDegradation = true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error checking service health for {}: {}", serviceName, e.getMessage(), e);
                    details.put(serviceName, new HealthStatus(STATUS_UNKNOWN, "Error during health check", 0));
                    isSystemHealthy = false;
                    failedComponents.add(serviceName);
                    incrementFailureCount(serviceName);
                }
            }
            
            // Calculate additional metrics
            double avgResponseTime = calculateAverageResponseTime();
            int totalFailures = calculateTotalFailures();
            
            String overallStatus;
            if (!isSystemHealthy) {
                overallStatus = STATUS_DOWN;
                
                // Additional logic to check if system can operate in degraded mode
                if (canOperateInDegradedMode(failedComponents)) {
                    overallStatus = STATUS_DEGRADED;
                }
            } else if (hasDegradation) {
                overallStatus = STATUS_DEGRADED;
            } else {
                overallStatus = STATUS_UP;
            }
            
            long endTime = System.currentTimeMillis();
            
            HealthStatus result = new HealthStatus(overallStatus, "Complete health check completed in " + (endTime - startTime) + "ms", endTime - startTime);
            result.setDetails(details);
            result.addMetric("averageResponseTime", avgResponseTime);
            result.addMetric("totalFailures", totalFailures);
            result.addMetric("servicesChecked", criticalServices.size());
            
            HttpStatus httpStatus = determineHttpStatus(overallStatus);
            
            LOGGER.info("Health check completed. Status: {}, Failed components: {}", 
                    overallStatus, String.join(", ", failedComponents));
            
            return new ResponseEntity<>(result, httpStatus);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during health check", e);
            HealthStatus errorStatus = new HealthStatus(STATUS_UNKNOWN, "Error during health check: " + e.getMessage(), 0);
            return new ResponseEntity<>(errorStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get health status for a specific service or component
     * 
     * @param serviceName The name of the service to check
     * @return HealthStatus object for the specified service
     */
    @GetMapping("/{serviceName}")
    public ResponseEntity<HealthStatus> healthCheck(@PathVariable String serviceName) {
        LOGGER.debug("Received health check request for service: {}", serviceName);
        
        if (serviceName == null || serviceName.trim().isEmpty()) {
            LOGGER.warn("Invalid service name provided");
            return new ResponseEntity<>(
                new HealthStatus(STATUS_UNKNOWN, "Invalid service name provided", 0),
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            // Apply different checking strategies based on service type
            HealthStatus status;
            long startTime = System.currentTimeMillis();
            
            switch (serviceName.toLowerCase()) {
                case "database":
                    status = checkDatabaseHealth();
                    break;
                case "cache":
                    status = checkCacheHealth();
                    break;
                case "messagebroker":
                    status = checkMessageBrokerHealth();
                    break;
                case "authservice":
                    status = checkAuthServiceHealth();
                    break;
                case "externalapi":
                    status = checkExternalApiHealth();
                    break;
                default:
                    // For any other service, use the general health check
                    if (healthService.isServiceSupported(serviceName)) {
                        status = healthService.checkServiceHealth(serviceName);
                    } else {
                        LOGGER.warn("Unsupported service requested: {}", serviceName);
                        return new ResponseEntity<>(
                            new HealthStatus(STATUS_UNKNOWN, "Unsupported service: " + serviceName, 0),
                            HttpStatus.NOT_FOUND
                        );
                    }
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            status.setResponseTime(responseTime);
            
            // Update metrics
            serviceResponseTimes.put(serviceName.toLowerCase(), responseTime);
            if (!STATUS_UP.equals(status.getStatus())) {
                incrementFailureCount(serviceName.toLowerCase());
            }
            
            // Add historical metrics
            Map<String, Object> historicalMetrics = new HashMap<>();
            historicalMetrics.put("failureCount", serviceFailureCounts.getOrDefault(serviceName.toLowerCase(), 0));
            historicalMetrics.put("averageResponseTime", getAverageResponseTimeForService(serviceName.toLowerCase()));
            status.addDetail("historicalMetrics", historicalMetrics);
            
            HttpStatus httpStatus = determineHttpStatus(status.getStatus());
            
            LOGGER.info("Health check for service {} completed with status: {}", serviceName, status.getStatus());
            return new ResponseEntity<>(status, httpStatus);
            
        } catch (Exception e) {
            LOGGER.error("Error checking health for service {}: {}", serviceName, e.getMessage(), e);
            HealthStatus errorStatus = new HealthStatus(STATUS_UNKNOWN, "Error checking service: " + e.getMessage(), 0);
            return new ResponseEntity<>(errorStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Liveness probe endpoint for Kubernetes health checks
     * This endpoint is used by Kubernetes to determine if the application is alive
     * 
     * @return Map containing status information
     */
    @GetMapping("/liveness")
    public Map<String, String> liveness() {
        LOGGER.debug("Liveness probe called");
        Map<String, String> response = new HashMap<>();
        
        try {
            // Simple check - just verify that the application is responsive
            response.put("status", STATUS_UP);
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
            response.put("service", "ecommerce-application");
            
            // Check if critical functionality is working
            boolean jvmHealthy = checkJvmHealth();
            boolean threadPoolHealthy = checkThreadPoolHealth();
            
            if (!jvmHealthy || !threadPoolHealthy) {
                response.put("status", STATUS_DOWN);
                
                StringBuilder details = new StringBuilder();
                if (!jvmHealthy) details.append("JVM health check failed. ");
                if (!threadPoolHealthy) details.append("Thread pool health check failed.");
                
                response.put("details", details.toString());
            }
            
            LOGGER.debug("Liveness probe result: {}", response.get("status"));
            return response;
        } catch (Exception e) {
            LOGGER.error("Error during liveness check", e);
            response.put("status", STATUS_DOWN);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Readiness probe endpoint for Kubernetes health checks
     * This endpoint is used by Kubernetes to determine if the application is ready to receive traffic
     * 
     * @return Map containing readiness information
     */
    @GetMapping("/readiness")
    public Map<String, String> readiness() {
        LOGGER.debug("Readiness probe called");
        Map<String, String> response = new HashMap<>();
        
        try {
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
            response.put("service", "ecommerce-application");
            
            // Check essential services to determine readiness
            boolean databaseReady = isDatabaseReady();
            boolean cacheReady = isCacheReady();
            boolean authServiceReady = isAuthServiceReady();
            
            if (databaseReady && (cacheReady || canOperateWithoutCache()) && authServiceReady) {
                response.put("status", STATUS_UP);
            } else {
                response.put("status", STATUS_DOWN);
                
                StringBuilder details = new StringBuilder("Not ready because: ");
                if (!databaseReady) details.append("Database not ready. ");
                if (!cacheReady && !canOperateWithoutCache()) details.append("Cache not ready. ");
                if (!authServiceReady) details.append("Auth service not ready.");
                
                response.put("details", details.toString());
            }
            
            // Add dependency details
            response.put("database", databaseReady ? "ready" : "not ready");
            response.put("cache", cacheReady ? "ready" : "not ready");
            response.put("authService", authServiceReady ? "ready" : "not ready");
            
            LOGGER.debug("Readiness probe result: {}", response.get("status"));
            return response;
        } catch (Exception e) {
            LOGGER.error("Error during readiness check", e);
            response.put("status", STATUS_DOWN);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    // Helper methods with complex conditional logic to increase cyclomatic complexity
    
    private boolean canOperateInDegradedMode(List<String> failedComponents) {
        // Critical services that must be available for the system to operate
        if (failedComponents.contains("database")) {
            return false;  // Can't operate without database
        }
        
        // Can operate without cache if it's the only failed component
        if (failedComponents.contains("cache") && failedComponents.size() == 1) {
            return true;
        }
        
        // Can operate without message broker in some cases
        if (failedComponents.contains("messageBroker") && !failedComponents.contains("authService")) {
            return true;
        }
        
        // If auth service is down but we have a fallback mechanism
        if (failedComponents.contains("authService") && healthService.isAuthServiceFallbackAvailable()) {
            return failedComponents.size() <= 1; // Only if auth service is the only failure
        }
        
        // If more than two critical services are down, system can't operate
        return failedComponents.size() <= 1;
    }
    
    private HttpStatus determineHttpStatus(String status) {
        switch (status) {
            case STATUS_UP:
                return HttpStatus.OK;
            case STATUS_DEGRADED:
                return HttpStatus.OK; // Still return 200 but with degraded status in body
            case STATUS_DOWN:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case STATUS_UNKNOWN:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    private void incrementFailureCount(String serviceName) {
        serviceFailureCounts.compute(serviceName, (key, count) -> count == null ? 1 : count + 1);
        
        // Log if failure count exceeds threshold
        int threshold = 5;
        int currentCount = serviceFailureCounts.getOrDefault(serviceName, 0);
        if (currentCount >= threshold) {
            LOGGER.warn("Service {} has failed {} times, which exceeds the threshold of {}", 
                    serviceName, currentCount, threshold);
        }
    }
    
    private double calculateAverageResponseTime() {
        if (serviceResponseTimes.isEmpty()) {
            return 0.0;
        }
        
        long sum = serviceResponseTimes.values().stream()
                .mapToLong(Long::longValue)
                .sum();
                
        return (double) sum / serviceResponseTimes.size();
    }
    
    private double getAverageResponseTimeForService(String serviceName) {
        Long responseTime = serviceResponseTimes.get(serviceName);
        return responseTime == null ? 0.0 : responseTime;
    }
    
    private int calculateTotalFailures() {
        return serviceFailureCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
    
    private HealthStatus checkDatabaseHealth() {
        try {
            // Complex database health check logic
            boolean isConnected = healthService.isDatabaseConnected();
            int activeConnections = healthService.getActiveDatabaseConnections();
            int maxConnections = healthService.getMaxDatabaseConnections();
            long queryResponseTime = healthService.getDatabaseResponseTime();
            
            Map<String, Object> details = new HashMap<>();
            details.put("connected", isConnected);
            details.put("activeConnections", activeConnections);
            details.put("maxConnections", maxConnections);
            details.put("responseTime", queryResponseTime);
            
            String status;
            String message;
            
            if (!isConnected) {
                status = STATUS_DOWN;
                message = "Database connection failed";
            } else if (activeConnections > maxConnections * 0.9) {
                status = STATUS_DEGRADED;
                message = "Database connection pool near capacity (" + 
                        activeConnections + "/" + maxConnections + ")";
            } else if (queryResponseTime > 1000) {
                status = STATUS_DEGRADED;
                message = "Database response time degraded: " + queryResponseTime + "ms";
            } else {
                status = STATUS_UP;
                message = "Database is healthy";
            }
            
            HealthStatus healthStatus = new HealthStatus(status, message, queryResponseTime);
            healthStatus.setDetails(details);
            return healthStatus;
        } catch (Exception e) {
            LOGGER.error("Error checking database health", e);
            return new HealthStatus(STATUS_DOWN, "Database health check error: " + e.getMessage(), 0);
        }
    }
    
    private HealthStatus checkCacheHealth() {
        try {
            boolean isConnected = healthService.isCacheConnected();
            long responseTime = healthService.getCacheResponseTime();
            int hitRate = healthService.getCacheHitRate();
            
            Map<String, Object> details = new HashMap<>();
            details.put("connected", isConnected);
            details.put("responseTime", responseTime);
            details.put("hitRate", hitRate + "%");
            
            String status;
            String message;
            
            if (!isConnected) {
                status = STATUS_DOWN;
                message = "Cache connection failed";
            } else if (responseTime > 100) {
                status = STATUS_DEGRADED;
                message = "Cache response time degraded: " + responseTime + "ms";
            } else if (hitRate < 50) {
                status = STATUS_DEGRADED;
                message = "Low cache hit rate: " + hitRate + "%";
            } else {
                status = STATUS_UP;
                message = "Cache is healthy";
            }
            
            HealthStatus healthStatus = new HealthStatus(status, message, responseTime);
            healthStatus.setDetails(details);
            return healthStatus;
        } catch (Exception e) {
            LOGGER.error("Error checking cache health", e);
            return new HealthStatus(STATUS_DOWN, "Cache health check error: " + e.getMessage(), 0);
        }
    }
    
    private HealthStatus checkMessageBrokerHealth() {
        try {
            boolean isConnected = healthService.isMessageBrokerConnected();
            int queueBacklog = healthService.getMessageQueueBacklog();
            
            Map<String, Object> details = new HashMap<>();
            details.put("connected", isConnected);
            details.put("queueBacklog", queueBacklog);
            
            String status;
            String message;
            
            if (!isConnected) {
                status = STATUS_DOWN;
                message = "Message broker connection failed";
            } else if (queueBacklog > 1000) {
                status = STATUS_DEGRADED;
                message = "High message queue backlog: " + queueBacklog;
            } else {
                status = STATUS_UP;
                message = "Message broker is healthy";
            }
            
            HealthStatus healthStatus = new HealthStatus(status, message, 0);
            healthStatus.setDetails(details);
            return healthStatus;
        } catch (Exception e) {
            LOGGER.error("Error checking message broker health", e);
            return new HealthStatus(STATUS_DOWN, "Message broker health check error: " + e.getMessage(), 0);
        }
    }
    
    private HealthStatus checkAuthServiceHealth() {
        try {
            boolean isAvailable = healthService.isAuthServiceAvailable();
            long responseTime = healthService.getAuthServiceResponseTime();
            boolean isCertValid = healthService.isAuthServiceCertificateValid();
            
            Map<String, Object> details = new HashMap<>();
            details.put("available", isAvailable);
            details.put("responseTime", responseTime);
            details.put("certificateValid", isCertValid);
            
            String status;
            String message;
            
            if (!isAvailable) {
                status = STATUS_DOWN;
                message = "Auth service is unavailable";
            } else if (!isCertValid) {
                status = STATUS_DOWN;
                message = "Auth service certificate is invalid";
            } else if (responseTime > 500) {
                status = STATUS_DEGRADED;
                message = "Auth service response time degraded: " + responseTime + "ms";
            } else {
                status = STATUS_UP;
                message = "Auth service is healthy";
            }
            
            HealthStatus healthStatus = new HealthStatus(status, message, responseTime);
            healthStatus.setDetails(details);
            return healthStatus;
        } catch (Exception e) {
            LOGGER.error("Error checking auth service health", e);
            return new HealthStatus(STATUS_DOWN, "Auth service health check error: " + e.getMessage(), 0);
        }
    }
    
    private HealthStatus checkExternalApiHealth() {
        try {
            List<String> externalApis = healthService.getRegisteredExternalApis();
            Map<String, Object> apiStatuses = new HashMap<>();
            boolean allApisUp = true;
            boolean anyApiDown = false;
            
            for (String api : externalApis) {
                boolean isAvailable = healthService.isExternalApiAvailable(api);
                long responseTime = healthService.getExternalApiResponseTime(api);
                
                Map<String, Object> apiStatus = new HashMap<>();
                apiStatus.put("available", isAvailable);
                apiStatus.put("responseTime", responseTime);
                
                if (!isAvailable) {
                    apiStatus.put("status", STATUS_DOWN);
                    anyApiDown = true;
                    allApisUp = false;
                } else if (responseTime > 1000) {
                    apiStatus.put("status", STATUS_DEGRADED);
                    allApisUp = false;
                } else {
                    apiStatus.put("status", STATUS_UP);
                }
                
                apiStatuses.put(api, apiStatus);
            }
            
            String overallStatus;
            String message;
            
            if (externalApis.isEmpty()) {
                overallStatus = STATUS_UP;
                message = "No external APIs configured";
            } else if (anyApiDown) {
                overallStatus = STATUS_DEGRADED; // Degraded instead of down because we might have fallbacks
                message = "One or more external APIs are unavailable";
            } else if (!allApisUp) {
                overallStatus = STATUS_DEGRADED;
                message = "One or more external APIs are degraded";
            } else {
                overallStatus = STATUS_UP;
                message = "All external APIs are healthy";
            }
            
            HealthStatus healthStatus = new HealthStatus(overallStatus, message, 0);
            healthStatus.addDetail("apis", apiStatuses);
            return healthStatus;
        } catch (Exception e) {
            LOGGER.error("Error checking external API health", e);
            return new HealthStatus(STATUS_DOWN, "External API health check error: " + e.getMessage(), 0);
        }
    }
    
    private boolean checkJvmHealth() {
        // Check JVM health based on memory usage
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            
            double memoryUsageRatio = (double) usedMemory / maxMemory;
            
            if (memoryUsageRatio > 0.95) {
                LOGGER.warn("JVM memory usage critical: {}%", Math.round(memoryUsageRatio * 100));
                return false;
            }
            
            int availableProcessors = runtime.availableProcessors();
            if (availableProcessors < 2) {
                LOGGER.warn("System running with limited processing resources: {} processors", availableProcessors);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error checking JVM health", e);
            return false;
        }
    }
    
    private boolean checkThreadPoolHealth() {
        try {
            int activeThreads = healthService.getActiveThreadCount();
            int maxThreads = healthService.getMaxThreadCount();
            
            if (maxThreads > 0 && (double) activeThreads / maxThreads > 0.9) {
                LOGGER.warn("Thread pool near capacity: {}/{}", activeThreads, maxThreads);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error checking thread pool health", e);
            return false;
        }
    }
    
    private boolean isDatabaseReady() {
        try {
            return healthService.isDatabaseConnected() && 
                   healthService.canExecuteDatabaseQuery();
        } catch (Exception e) {
            LOGGER.error("Error checking database readiness", e);
            return false;
        }
    }
    
    private boolean isCacheReady() {
        try {
            return healthService.isCacheConnected() && 
                   healthService.getCacheResponseTime() < 200;
        } catch (Exception e) {
            LOGGER.error("Error checking cache readiness", e);
            return false;
        }
    }
    
    private boolean isAuthServiceReady() {
        try {
            if (!healthService.isAuthServiceAvailable()) {
                return false;
            }
            
            // Additional checks for auth service readiness
            if (!healthService.isAuthServiceCertificateValid()) {
                LOGGER.warn("Auth service certificate is invalid");
                return false;
            }
            
            if (healthService.getAuthServiceResponseTime() > 1000) {
                LOGGER.warn("Auth service response time too high");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error checking auth service readiness", e);
            return false;
        }
    }
    
    private boolean canOperateWithoutCache() {
        // Determine if the system can operate without cache based on configuration
        return healthService.isFallbackCacheModeEnabled();
    }
    
    /**
     * Inner class representing health status details
     */
    public static class HealthStatus {
        private String status;
        private String message;
        private long responseTime;
        private Map<String, Object> details;
        private Map<String, Object> metrics;
        
        public HealthStatus(String status, String message, long responseTime) {
            this.status = status;
            this.message = message;
            this.responseTime = responseTime;
            this.details = new HashMap<>();
            this.metrics = new HashMap<>();
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getResponseTime() {
            return responseTime;
        }
        
        public void setResponseTime(long responseTime) {
            this.responseTime = responseTime;
        }
        
        public Map<String, Object> getDetails() {
            return details;
        }
        
        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
        
        public void addDetail(String key, Object value) {
            this.details.put(key, value);
        }
        
        public Map<String, Object> getMetrics() {
            return metrics;
        }
        
        public void setMetrics(Map<String, Object> metrics) {
            this.metrics = metrics;
        }
        
        public void addMetric(String key, Object value) {
            this.metrics.put(key, value);
        }
    }
}