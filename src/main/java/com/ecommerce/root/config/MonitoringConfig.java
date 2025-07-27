package com.ecommerce.root.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Configuration class for monitoring and metrics using Micrometer and Prometheus.
 * This class provides comprehensive configuration for metrics collection, health checks,
 * and custom business metrics to monitor system performance and health.
 *
 * @author Monitoring Team
 * @version 1.0
 */
@Configuration
public class MonitoringConfig {

    /**
     * Application name for metrics identification
     */
    private final String applicationName;
    
    /**
     * Whether metrics collection is enabled
     */
    private final boolean metricsEnabled;
    
    @Autowired
    private Environment environment;
    
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final Map<String, AtomicInteger> endpointHitCounts = new HashMap<>();

    /**
     * Constructor for MonitoringConfig.
     * 
     * @param applicationName the name of the application for metrics identification
     * @param metricsEnabled whether metrics collection is enabled
     */
    public MonitoringConfig(
            @Value("${spring.application.name:e-commerce-application}") String applicationName,
            @Value("${metrics.enabled:true}") boolean metricsEnabled) {
        this.applicationName = applicationName;
        this.metricsEnabled = metricsEnabled;
    }

    /**
     * Configures the Prometheus meter registry for metrics collection.
     * This registry will collect and expose metrics in a format that Prometheus can scrape.
     * 
     * @return A configured MeterRegistry instance
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        if (!metricsEnabled) {
            return new SimpleMeterRegistry();
        }
        
        // Create a Prometheus registry
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Add common tags for metrics
        registry.config()
            .commonTags("application", applicationName)
            .commonTags("environment", getEnvironmentName());
        
        // Configure filters for metrics
        configureMetricsFilters(registry);
        
        // Register JVM metrics
        registerJvmMetrics(registry);
        
        // Handle different deployment environments
        String deployEnv = environment.getProperty("app.deployment.environment", "dev");
        switch (deployEnv) {
            case "prod":
                // Production-specific configurations
                registry.config().meterFilter(MeterFilter.deny(id -> 
                    id.getName().contains("debugging") || 
                    id.getName().startsWith("dev.")));
                break;
                
            case "staging":
                // Staging-specific configurations
                registry.config().meterFilter(MeterFilter.acceptNameStartsWith("business."));
                registry.config().meterFilter(MeterFilter.acceptNameStartsWith("system."));
                break;
                
            case "dev":
            default:
                // Development-specific configurations - capture everything
                // Add extra debug metrics
                Timer.builder("dev.debugTimer")
                    .description("Debug timer for development")
                    .register(registry);
                break;
        }
        
        // Additional dynamic configuration based on application profile
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles != null && activeProfiles.length > 0) {
            if (Arrays.asList(activeProfiles).contains("monitoring")) {
                // Enhanced monitoring profile
                registry.config().meterFilter(new PrometheusRenameFilter());
                // Add more detailed metrics for monitoring profile
                Counter.builder("monitoring.profile.active")
                    .description("Indicates that enhanced monitoring is active")
                    .register(registry)
                    .increment();
            }
            
            if (Arrays.asList(activeProfiles).contains("performance")) {
                // Performance testing profile
                Timer perfTestTimer = Timer.builder("performance.test.timer")
                    .description("Timer for performance tests")
                    .publishPercentileHistogram()
                    .register(registry);
                
                // Simulate some measurements for the performance timer
                perfTestTimer.record(100, TimeUnit.MILLISECONDS);
            }
        }
        
        return registry;
    }

    /**
     * Configures timer metrics for performance monitoring.
     * These timers will track how long certain operations take.
     * 
     * @return A configured TimerConfig instance
     */
    @Bean
    public TimerConfig timerConfig() {
        TimerConfig config = new TimerConfig();
        
        if (!metricsEnabled) {
            config.setEnabled(false);
            return config;
        }
        
        config.setEnabled(true);
        
        // Configure based on runtime conditions
        String performanceProfile = environment.getProperty("app.performance.profile", "balanced");
        
        switch (performanceProfile) {
            case "high-precision":
                config.setPercentilePrecision(0.001);
                config.setPublishHistogram(true);
                config.setPercentiles(new double[]{0.5, 0.75, 0.9, 0.95, 0.99, 0.999, 0.9999});
                break;
                
            case "memory-optimized":
                config.setPercentilePrecision(0.05);
                config.setPublishHistogram(false);
                config.setPercentiles(new double[]{0.5, 0.95});
                break;
                
            case "balanced":
            default:
                config.setPercentilePrecision(0.01);
                config.setPublishHistogram(true);
                config.setPercentiles(new double[]{0.5, 0.9, 0.95, 0.99});
                break;
        }
        
        // Configure SLA thresholds
        Map<String, Long> slaThresholds = new HashMap<>();
        slaThresholds.put("api.request", 200L);
        slaThresholds.put("db.query", 50L);
        slaThresholds.put("cache.access", 5L);
        config.setSlaThresholds(slaThresholds);
        
        // Apply additional customizations based on feature flags
        boolean detailedMetrics = Boolean.parseBoolean(environment.getProperty("metrics.detailed", "false"));
        if (detailedMetrics) {
            config.setDetailedMetrics(true);
            config.setMaximumExpectedValue(30000L);
            config.setMinimumExpectedValue(1L);
            
            // If we're using detailed metrics, add even more percentile calculations
            double[] existingPercentiles = config.getPercentiles();
            double[] enhancedPercentiles = new double[existingPercentiles.length + 3];
            System.arraycopy(existingPercentiles, 0, enhancedPercentiles, 0, existingPercentiles.length);
            enhancedPercentiles[existingPercentiles.length] = 0.9999;
            enhancedPercentiles[existingPercentiles.length + 1] = 0.99999;
            enhancedPercentiles[existingPercentiles.length + 2] = 0.999999;
            config.setPercentiles(enhancedPercentiles);
        }
        
        return config;
    }

    /**
     * Registers custom metrics for business logic monitoring.
     * These metrics provide insights into business processes and application performance.
     * 
     * @param registry The meter registry to use for custom metrics
     */
    @Bean
    public void customMetrics(MeterRegistry registry) {
        if (!metricsEnabled) {
            return;
        }
        
        // Active requests gauge
        Gauge.builder("http.active.requests", activeRequests, AtomicInteger::get)
            .description("Number of active HTTP requests")
            .tag("application", applicationName)
            .register(registry);
        
        // Error count
        Counter errorCounter = Counter.builder("application.errors")
            .description("Count of application errors")
            .tag("application", applicationName)
            .register(registry);
            
        // Success count
        Counter successCounter = Counter.builder("application.success")
            .description("Count of successful operations")
            .tag("application", applicationName)
            .register(registry);
            
        // System metrics
        Gauge.builder("system.load.average", this, value -> getSystemLoadAverage())
            .description("System load average")
            .tag("application", applicationName)
            .register(registry);
            
        // Custom business metrics - depends on the active features
        String[] features = environment.getProperty("app.enabled.features", String[].class, new String[0]);
        if (features != null) {
            for (String feature : features) {
                switch (feature) {
                    case "orders":
                        Counter.builder("business.orders.created")
                            .description("Number of orders created")
                            .tag("feature", "orders")
                            .tag("type", "creation")
                            .register(registry);
                        
                        Counter.builder("business.orders.fulfilled")
                            .description("Number of orders fulfilled")
                            .tag("feature", "orders")
                            .tag("type", "fulfillment")
                            .register(registry);
                        
                        Counter.builder("business.orders.canceled")
                            .description("Number of orders canceled")
                            .tag("feature", "orders")
                            .tag("type", "cancellation")
                            .register(registry);
                        break;
                        
                    case "payments":
                        Counter.builder("business.payments.succeeded")
                            .description("Number of successful payments")
                            .tag("feature", "payments")
                            .tag("status", "success")
                            .register(registry);
                        
                        Counter.builder("business.payments.failed")
                            .description("Number of failed payments")
                            .tag("feature", "payments")
                            .tag("status", "failure")
                            .register(registry);
                            
                        // Add gauges for payment amounts
                        AtomicInteger paymentTotal = new AtomicInteger(0);
                        Gauge.builder("business.payments.total", paymentTotal, AtomicInteger::get)
                            .description("Total payment amount processed")
                            .tag("feature", "payments")
                            .register(registry);
                        break;
                        
                    case "inventory":
                        // Inventory-specific metrics
                        AtomicInteger stockLevel = new AtomicInteger(100);
                        Gauge.builder("business.inventory.stock", stockLevel, AtomicInteger::get)
                            .description("Current stock level")
                            .tag("feature", "inventory")
                            .register(registry);
                        
                        Counter.builder("business.inventory.restocked")
                            .description("Number of restocked items")
                            .tag("feature", "inventory")
                            .tag("type", "restock")
                            .register(registry);
                        break;
                        
                    case "users":
                        // User-specific metrics
                        Counter.builder("business.users.registered")
                            .description("Number of registered users")
                            .tag("feature", "users")
                            .tag("type", "registration")
                            .register(registry);
                            
                        Counter.builder("business.users.active")
                            .description("Number of active users")
                            .tag("feature", "users")
                            .tag("status", "active")
                            .register(registry);
                        break;
                        
                    default:
                        // Generic feature metrics
                        Counter.builder("business.feature." + feature + ".usage")
                            .description("Usage of feature: " + feature)
                            .tag("feature", feature)
                            .register(registry);
                        break;
                }
            }
        }
        
        // Register endpoint-specific counters dynamically
        String[] monitoredEndpoints = environment.getProperty("metrics.monitored.endpoints", String[].class, new String[0]);
        if (monitoredEndpoints != null) {
            for (String endpoint : monitoredEndpoints) {
                // Create counter for this endpoint
                AtomicInteger hitCount = new AtomicInteger(0);
                endpointHitCounts.put(endpoint, hitCount);
                
                Gauge.builder("http.endpoint.hits", hitCount, AtomicInteger::get)
                    .description("Hit count for endpoint")
                    .tag("endpoint", endpoint)
                    .tag("application", applicationName)
                    .register(registry);
            }
        }
    }

    /**
     * Configures a custom health indicator for system monitoring.
     * This health indicator will report the overall health of the application.
     * 
     * @return A configured HealthIndicator instance
     */
    @Bean
    public HealthIndicator healthIndicator() {
        return () -> {
            // Build a complex health status determination based on multiple factors
            boolean systemHealthy = true;
            Map<String, Object> details = new HashMap<>();
            
            // Check JVM memory status
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100.0;
            
            details.put("memory.used.bytes", usedMemory);
            details.put("memory.max.bytes", maxMemory);
            details.put("memory.usage.percent", memoryUsagePercent);
            
            if (memoryUsagePercent > 90) {
                systemHealthy = false;
                details.put("memory.status", "CRITICAL");
            } else if (memoryUsagePercent > 80) {
                details.put("memory.status", "WARNING");
            } else {
                details.put("memory.status", "OK");
            }
            
            // Check disk space
            String dataDir = environment.getProperty("app.data.directory", "/tmp");
            long diskFreeSpace = new java.io.File(dataDir).getFreeSpace();
            details.put("disk.free.bytes", diskFreeSpace);
            
            if (diskFreeSpace < 100 * 1024 * 1024) { // Less than 100MB
                systemHealthy = false;
                details.put("disk.status", "CRITICAL");
            } else if (diskFreeSpace < 500 * 1024 * 1024) { // Less than 500MB
                details.put("disk.status", "WARNING");
            } else {
                details.put("disk.status", "OK");
            }
            
            // Check error rate
            int totalRequests = successCount.get() + errorCount.get();
            double errorRate = totalRequests > 0 ? 
                (double) errorCount.get() / totalRequests : 0.0;
            
            details.put("errors.count", errorCount.get());
            details.put("success.count", successCount.get());
            details.put("error.rate", errorRate);
            
            if (errorRate > 0.1) { // More than 10% error rate
                systemHealthy = false;
                details.put("error.status", "CRITICAL");
            } else if (errorRate > 0.01) { // More than 1% error rate
                details.put("error.status", "WARNING");
            } else {
                details.put("error.status", "OK");
            }
            
            // Check active connections
            int activeConnections = activeRequests.get();
            details.put("connections.active", activeConnections);
            
            int maxConnections = Integer.parseInt(
                environment.getProperty("server.max.connections", "100"));
            
            if (activeConnections > maxConnections * 0.9) { // Over 90% capacity
                systemHealthy = false;
                details.put("connections.status", "CRITICAL");
            } else if (activeConnections > maxConnections * 0.8) { // Over 80% capacity
                details.put("connections.status", "WARNING");
            } else {
                details.put("connections.status", "OK");
            }
            
            // Add additional checks based on environment
            String deployEnv = environment.getProperty("app.deployment.environment", "dev");
            if ("prod".equals(deployEnv)) {
                // Production has stricter health requirements
                double systemLoad = getSystemLoadAverage();
                details.put("system.load", systemLoad);
                
                if (systemLoad > 0.8) { // High system load
                    systemHealthy = false;
                    details.put("system.load.status", "CRITICAL");
                } else if (systemLoad > 0.6) {
                    details.put("system.load.status", "WARNING");
                } else {
                    details.put("system.load.status", "OK");
                }
            }
            
            // Final health determination
            if (systemHealthy) {
                return Health.up()
                    .withDetails(details)
                    .build();
            } else {
                return Health.down()
                    .withDetails(details)
                    .build();
            }
        };
    }
    
    /**
     * Helper method to determine the environment name.
     * 
     * @return The name of the environment
     */
    private String getEnvironmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        
        if (activeProfiles != null && activeProfiles.length > 0) {
            for (String profile : activeProfiles) {
                if ("prod".equals(profile) || "production".equals(profile)) {
                    return "production";
                } else if ("staging".equals(profile)) {
                    return "staging";
                } else if ("qa".equals(profile)) {
                    return "qa";
                } else if ("dev".equals(profile) || "development".equals(profile)) {
                    return "development";
                }
            }
        }
        
        return "development"; // Default environment
    }
    
    /**
     * Helper method to configure filters for metrics.
     * 
     * @param registry The meter registry to configure
     */
    private void configureMetricsFilters(MeterRegistry registry) {
        // Filter out metrics that are too verbose
        registry.config().meterFilter(MeterFilter.deny(id -> {
            String name = id.getName();
            
            // Filter out specific metrics that generate too much data
            if (name.startsWith("jvm.gc.pause") && !name.contains("count")) {
                return true;
            }
            
            // Filter out metrics with high cardinality
            Map<String, String> tags = id.getTags();
            if (tags.containsKey("uri") && tags.get("uri").contains("favicon")) {
                return true;
            }
            
            // Complex conditional filtering
            if (name.startsWith("http.server.requests")) {
                String exception = tags.get("exception");
                String outcome = tags.get("outcome");
                
                // Only keep specific exceptions and outcomes
                if (exception != null && !exception.equals("None") && 
                    !exception.contains("NotFoundException") &&
                    !exception.contains("SecurityException")) {
                    return true;
                }
                
                if ("REDIRECTION".equals(outcome) && name.contains("actuator")) {
                    return true;
                }
            }
            
            return false;
        }));
        
        // Add custom filters based on application configuration
        boolean enableVerboseMetrics = Boolean.parseBoolean(
            environment.getProperty("metrics.verbose", "false"));
            
        if (!enableVerboseMetrics) {
            registry.config().meterFilter(
                MeterFilter.deny(id -> id.getName().startsWith("tomcat.") ||
                                      id.getName().startsWith("process.") ||
                                      id.getName().startsWith("logback.")));
        }
    }
    
    /**
     * Helper method to register JVM metrics with the registry.
     * 
     * @param registry The meter registry for registering metrics
     */
    private void registerJvmMetrics(MeterRegistry registry) {
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ClassLoaderMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
    }
    
    /**
     * Helper method to get the system load average.
     * Returns a value between 0.0 and 1.0 representing system load.
     * 
     * @return The system load as a double between 0.0 and 1.0
     */
    private double getSystemLoadAverage() {
        // This is a simplified implementation for demonstration purposes
        // In a real system, this would use OperatingSystemMXBean to get actual system load
        double systemLoad = 0.0;
        try {
            java.lang.management.OperatingSystemMXBean osBean = 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                double loadAvg = sunOsBean.getSystemLoadAverage();
                int processors = osBean.getAvailableProcessors();
                
                if (loadAvg >= 0 && processors > 0) {
                    systemLoad = loadAvg / processors; // Normalize by processor count
                }
            }
        } catch (Exception e) {
            // Fallback to a reasonable default if we can't get the actual load
            systemLoad = 0.5;
        }
        
        return systemLoad;
    }
    
    /**
     * Inner class for timer configuration settings.
     * This class holds configuration settings for timer metrics.
     */
    public static class TimerConfig {
        private boolean enabled;
        private double percentilePrecision;
        private boolean publishHistogram;
        private double[] percentiles;
        private boolean detailedMetrics;
        private long minimumExpectedValue;
        private long maximumExpectedValue;
        private Map<String, Long> slaThresholds;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getPercentilePrecision() {
            return percentilePrecision;
        }
        
        public void setPercentilePrecision(double percentilePrecision) {
            this.percentilePrecision = percentilePrecision;
        }
        
        public boolean isPublishHistogram() {
            return publishHistogram;
        }
        
        public void setPublishHistogram(boolean publishHistogram) {
            this.publishHistogram = publishHistogram;
        }
        
        public double[] getPercentiles() {
            return percentiles;
        }
        
        public void setPercentiles(double[] percentiles) {
            this.percentiles = percentiles;
        }
        
        public boolean isDetailedMetrics() {
            return detailedMetrics;
        }
        
        public void setDetailedMetrics(boolean detailedMetrics) {
            this.detailedMetrics = detailedMetrics;
        }
        
        public long getMinimumExpectedValue() {
            return minimumExpectedValue;
        }
        
        public void setMinimumExpectedValue(long minimumExpectedValue) {
            this.minimumExpectedValue = minimumExpectedValue;
        }
        
        public long getMaximumExpectedValue() {
            return maximumExpectedValue;
        }
        
        public void setMaximumExpectedValue(long maximumExpectedValue) {
            this.maximumExpectedValue = maximumExpectedValue;
        }
        
        public Map<String, Long> getSlaThresholds() {
            return slaThresholds;
        }
        
        public void setSlaThresholds(Map<String, Long> slaThresholds) {
            this.slaThresholds = slaThresholds;
        }
    }
}