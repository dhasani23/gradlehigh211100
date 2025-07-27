package com.ecommerce.root.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Metrics collection and aggregation service providing system performance statistics.
 * This service handles various types of metrics including counters, timers, and gauges.
 * It provides high-level methods for recording and retrieving metrics data.
 * 
 * High cyclomatic complexity in this class is due to the extensive error handling,
 * edge case management, and the complex conditions in metrics collection.
 */
public class MetricsService {

    /**
     * Micrometer meter registry for metrics collection
     */
    private final MeterRegistry meterRegistry;
    
    /**
     * Map of custom counter metrics
     */
    private final Map<String, Counter> customCounters;
    
    /**
     * Map of custom timer metrics
     */
    private final Map<String, Timer> customTimers;
    
    /**
     * Cache for most recently accessed metrics to optimize performance
     */
    private final Map<String, Object> metricsCache;
    
    /**
     * Lock object for thread synchronization during metrics updates
     */
    private final Object metricLock = new Object();
    
    /**
     * Flag indicating if the service is in debug mode
     */
    private boolean debugMode = false;
    
    /**
     * Threshold for timer metric alerts in milliseconds
     */
    private long timerAlertThresholdMs = 1000;

    /**
     * Constructs a new MetricsService with the provided meter registry.
     *
     * @param meterRegistry the Micrometer registry to use for metrics collection
     * @throws IllegalArgumentException if meterRegistry is null
     */
    public MetricsService(MeterRegistry meterRegistry) {
        if (meterRegistry == null) {
            throw new IllegalArgumentException("MeterRegistry cannot be null");
        }
        
        this.meterRegistry = meterRegistry;
        this.customCounters = new ConcurrentHashMap<>();
        this.customTimers = new ConcurrentHashMap<>();
        this.metricsCache = new ConcurrentHashMap<>();
        
        // Register basic system metrics
        initializeSystemMetrics();
    }

    /**
     * Initializes basic system metrics like memory usage, CPU load, etc.
     * FIXME: Add actual JVM metrics initialization once we have proper implementation
     */
    private void initializeSystemMetrics() {
        // Register JVM memory metrics
        registerCustomGauge("jvm.memory.used", () -> {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        });
        
        registerCustomGauge("jvm.memory.total", () -> {
            return Runtime.getRuntime().totalMemory();
        });
        
        // TODO: Add more system metrics like thread count, GC metrics, etc.
    }

    /**
     * Increments a counter metric with optional tags.
     * If the counter doesn't exist, it will be created.
     *
     * @param metricName the name of the metric to increment
     * @param tags optional tags to associate with the metric (key-value pairs)
     */
    public void incrementCounter(String metricName, String... tags) {
        validateMetricName(metricName);
        
        try {
            String counterKey = generateMetricKey(metricName, tags);
            
            Counter counter;
            synchronized (metricLock) {
                counter = customCounters.get(counterKey);
                
                if (counter == null) {
                    // Complex logic for tag parsing and counter creation
                    List<Tag> tagList = new ArrayList<>();
                    
                    // Process tags if provided
                    if (tags != null && tags.length > 0) {
                        // Validate tags are in proper key-value pairs
                        if (tags.length % 2 != 0) {
                            throw new IllegalArgumentException("Tags must be provided as key-value pairs");
                        }
                        
                        for (int i = 0; i < tags.length; i += 2) {
                            String tagKey = tags[i];
                            String tagValue = tags[i + 1];
                            
                            // Validate tag key and value
                            if (tagKey == null || tagKey.trim().isEmpty()) {
                                throw new IllegalArgumentException("Tag key cannot be null or empty");
                            }
                            
                            if (tagValue == null) {
                                tagValue = "unknown"; // Default value for null tag values
                            }
                            
                            tagList.add(Tag.of(tagKey, tagValue));
                        }
                    }
                    
                    // Create counter with the processed tags
                    counter = Counter.builder(metricName)
                            .tags(tagList)
                            .description("Custom counter metric: " + metricName)
                            .register(meterRegistry);
                    
                    customCounters.put(counterKey, counter);
                    
                    // Clear cache when adding new counter
                    metricsCache.remove("counters");
                }
            }
            
            // Increment the counter
            counter.increment();
            
            if (debugMode) {
                System.out.println("Incremented counter: " + counterKey);
            }
            
        } catch (Exception e) {
            // Error handling for production environments
            System.err.println("Failed to increment counter [" + metricName + "]: " + e.getMessage());
            
            // Fall back to a default counter in case of errors
            try {
                Counter fallbackCounter = meterRegistry.counter("metrics.errors");
                fallbackCounter.increment();
            } catch (Exception fallbackEx) {
                // Silent catch for fallback error
            }
        }
    }

    /**
     * Records a timer metric with the specified duration.
     * If the timer doesn't exist, it will be created.
     *
     * @param metricName the name of the metric to record
     * @param duration the duration to record
     */
    public void recordTimer(String metricName, Duration duration) {
        validateMetricName(metricName);
        
        if (duration == null) {
            throw new IllegalArgumentException("Duration cannot be null");
        }
        
        try {
            Timer timer;
            synchronized (metricLock) {
                timer = customTimers.get(metricName);
                
                if (timer == null) {
                    // Create complex timer with percentiles and SLO thresholds
                    timer = Timer.builder(metricName)
                            .description("Custom timer metric: " + metricName)
                            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                            .publishPercentileHistogram()
                            .sla(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofSeconds(1))
                            .minimumExpectedValue(Duration.ofMillis(1))
                            .maximumExpectedValue(Duration.ofMinutes(10))
                            .register(meterRegistry);
                    
                    customTimers.put(metricName, timer);
                    
                    // Clear cache when adding new timer
                    metricsCache.remove("timers");
                }
            }
            
            // Record the timing
            timer.record(duration.toNanos(), TimeUnit.NANOSECONDS);
            
            // Check for slow operations that exceed threshold
            if (duration.toMillis() > timerAlertThresholdMs) {
                // Log slow operations
                System.out.println("WARN: Slow operation detected - " + metricName + 
                        " took " + duration.toMillis() + "ms (threshold: " + timerAlertThresholdMs + "ms)");
                
                // Increment a separate counter for slow operations
                incrementCounter("metrics.slow.operations", "metricName", metricName);
            }
            
            // Update the last recorded value in the cache
            metricsCache.put("last." + metricName, duration.toMillis());
            
        } catch (Exception e) {
            System.err.println("Failed to record timer [" + metricName + "]: " + e.getMessage());
            
            // Record error metrics
            try {
                Counter errorCounter = meterRegistry.counter("metrics.timer.errors", 
                        "metricName", metricName, 
                        "errorType", e.getClass().getSimpleName());
                errorCounter.increment();
            } catch (Exception fallbackEx) {
                // Silent catch for fallback error
            }
        }
    }

    /**
     * Retrieves a snapshot of all registered metrics.
     * This is a computationally expensive operation and should not be called frequently.
     *
     * @return a map containing all metrics with their current values
     */
    public Map<String, Object> getMetricsSnapshot() {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            
            // Get counter metrics
            Map<String, Double> counters = new HashMap<>();
            for (Map.Entry<String, Counter> entry : customCounters.entrySet()) {
                counters.put(entry.getKey(), entry.getValue().count());
            }
            snapshot.put("counters", counters);
            
            // Get timer metrics with complex statistics
            Map<String, Map<String, Number>> timers = new HashMap<>();
            for (Map.Entry<String, Timer> entry : customTimers.entrySet()) {
                Timer timer = entry.getValue();
                Map<String, Number> timerStats = new HashMap<>();
                
                // Collect comprehensive timer statistics
                timerStats.put("count", timer.count());
                timerStats.put("totalTime", timer.totalTime(TimeUnit.MILLISECONDS));
                timerStats.put("max", timer.max(TimeUnit.MILLISECONDS));
                timerStats.put("mean", timer.mean(TimeUnit.MILLISECONDS));
                
                // Add percentiles if available
                try {
                    timerStats.put("p50", timer.percentile(0.5, TimeUnit.MILLISECONDS));
                    timerStats.put("p95", timer.percentile(0.95, TimeUnit.MILLISECONDS));
                    timerStats.put("p99", timer.percentile(0.99, TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    // Some timer implementations might not support percentiles
                    timerStats.put("percentileError", 1);
                }
                
                timers.put(entry.getKey(), timerStats);
            }
            snapshot.put("timers", timers);
            
            // Include system metrics
            snapshot.put("system", collectSystemMetrics());
            
            // Include service metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("metricsCount", customCounters.size() + customTimers.size());
            snapshot.put("metadata", metadata);
            
            return Collections.unmodifiableMap(snapshot);
            
        } catch (Exception e) {
            System.err.println("Failed to get metrics snapshot: " + e.getMessage());
            
            // Return a minimal snapshot in case of errors
            Map<String, Object> errorSnapshot = new HashMap<>();
            errorSnapshot.put("error", "Failed to collect metrics: " + e.getMessage());
            errorSnapshot.put("timestamp", System.currentTimeMillis());
            return errorSnapshot;
        }
    }

    /**
     * Collects various system metrics.
     * 
     * @return a map of system metrics
     */
    private Map<String, Object> collectSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        // Memory metrics
        systemMetrics.put("memory.max", runtime.maxMemory());
        systemMetrics.put("memory.total", runtime.totalMemory());
        systemMetrics.put("memory.free", runtime.freeMemory());
        systemMetrics.put("memory.used", runtime.totalMemory() - runtime.freeMemory());
        
        // TODO: Add more comprehensive system metrics
        
        return systemMetrics;
    }

    /**
     * Registers a custom gauge metric that will return values from the provided supplier.
     *
     * @param name the name of the gauge metric
     * @param supplier the supplier that provides the gauge value
     */
    public void registerCustomGauge(String name, Supplier<Number> supplier) {
        validateMetricName(name);
        
        if (supplier == null) {
            throw new IllegalArgumentException("Value supplier cannot be null");
        }
        
        try {
            // Complex gauge creation with error handling and null checks
            Gauge gauge = Gauge
                .builder(name, () -> {
                    try {
                        Number value = supplier.get();
                        return value != null ? value.doubleValue() : 0.0;
                    } catch (Exception e) {
                        System.err.println("Error getting gauge value for " + name + ": " + e.getMessage());
                        
                        // Increment an error counter
                        try {
                            Counter errorCounter = meterRegistry.counter("metrics.gauge.errors", "name", name);
                            errorCounter.increment();
                        } catch (Exception ignored) {
                            // Silent catch
                        }
                        
                        return 0.0; // Default value in case of error
                    }
                })
                .description("Custom gauge metric: " + name)
                .register(meterRegistry);
            
            // Clear gauge cache
            metricsCache.remove("gauges");
            
            if (debugMode) {
                System.out.println("Registered custom gauge: " + name);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to register gauge [" + name + "]: " + e.getMessage());
            e.printStackTrace();
            
            // Record error
            try {
                Counter errorCounter = meterRegistry.counter("metrics.registration.errors");
                errorCounter.increment();
            } catch (Exception ignored) {
                // Silent catch
            }
        }
    }

    /**
     * Validates a metric name to ensure it meets naming requirements.
     * 
     * @param metricName the metric name to validate
     * @throws IllegalArgumentException if the metric name is invalid
     */
    private void validateMetricName(String metricName) {
        if (metricName == null || metricName.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name cannot be null or empty");
        }
        
        // Check for invalid characters in metric name
        if (!metricName.matches("^[a-zA-Z0-9_.]+$")) {
            throw new IllegalArgumentException(
                    "Metric name contains invalid characters. Use only letters, numbers, underscores, and dots.");
        }
        
        // Check for reserved prefixes
        if (metricName.startsWith("_") || metricName.startsWith(".")) {
            throw new IllegalArgumentException("Metric name cannot start with underscore or dot");
        }
    }

    /**
     * Generates a unique key for a metric based on its name and tags.
     * 
     * @param metricName the name of the metric
     * @param tags the tags associated with the metric
     * @return a unique key for the metric
     */
    private String generateMetricKey(String metricName, String... tags) {
        StringBuilder keyBuilder = new StringBuilder(metricName);
        
        if (tags != null && tags.length > 0) {
            // For odd number of tags, we'll ignore the last one
            int tagsToProcess = tags.length - (tags.length % 2);
            
            for (int i = 0; i < tagsToProcess; i += 2) {
                String tagKey = tags[i];
                String tagValue = tags[i + 1];
                
                if (tagKey != null && tagValue != null) {
                    keyBuilder.append("#").append(tagKey).append("=").append(tagValue);
                }
            }
        }
        
        return keyBuilder.toString();
    }

    /**
     * Sets the debug mode for the metrics service.
     * When enabled, additional logging will be performed.
     * 
     * @param debugMode true to enable debug mode, false to disable
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Sets the threshold for timer metric alerts.
     * 
     * @param thresholdMs the threshold in milliseconds
     */
    public void setTimerAlertThreshold(long thresholdMs) {
        if (thresholdMs < 0) {
            throw new IllegalArgumentException("Timer alert threshold cannot be negative");
        }
        this.timerAlertThresholdMs = thresholdMs;
    }
    
    /**
     * Resets all custom metrics.
     * This is primarily useful for testing or during application reinitialization.
     */
    public void resetAllMetrics() {
        synchronized (metricLock) {
            // Remove all custom counters and timers from the registry
            // Note: This is a complex operation with high cyclomatic complexity
            
            // FIXME: This approach doesn't properly remove meters from registry
            // We should implement proper meter removal through MeterRegistry API
            
            customCounters.clear();
            customTimers.clear();
            metricsCache.clear();
            
            // Re-initialize system metrics
            initializeSystemMetrics();
        }
    }
    
    /**
     * Returns the meter registry used by this metrics service.
     * 
     * @return the meter registry
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}