package com.ecommerce.root.service;

import java.util.Map;
import java.util.HashMap;

/**
 * Service for metrics
 */
public class MetricsService {

    private final Map<String, Object> customTimers;

    /**
     * Constructor
     */
    public MetricsService() {
        this.customTimers = new HashMap<>();
    }

    /**
     * Gets the meter registry
     *
     * @return The meter registry
     */
    public Object getMeterRegistry() {
        return new Object();
    }
}