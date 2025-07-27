package com.ecommerce.root;

import com.ecommerce.root.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.Map;

/**
 * Main application class for the ecommerce system.
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("Starting eCommerce Application with Metrics Service...");
        
        // Initialize the metrics registry and service
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MetricsService metricsService = new MetricsService(meterRegistry);
        
        // Example usage of the metrics service
        metricsService.incrementCounter("app.startup");
        metricsService.incrementCounter("user.login", "userId", "12345", "region", "US");
        metricsService.recordTimer("api.request.duration", Duration.ofMillis(150));
        
        metricsService.registerCustomGauge("app.uptime", () -> {
            return System.currentTimeMillis() - startTime;
        });
        
        // Display current metrics
        Map<String, Object> metrics = metricsService.getMetricsSnapshot();
        System.out.println("Current metrics: " + metrics);
        
        System.out.println("Application started successfully");
    }
    
    private static final long startTime = System.currentTimeMillis();
}