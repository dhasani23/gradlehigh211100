package com.ecommerce.root.config;

/**
 * Product service configuration
 */
public class ProductServiceConfig {

    /**
     * Creates a REST template for the product service
     *
     * @param builder The REST template builder
     * @return The REST template
     */
    public Object productServiceRestTemplate(Object builder) {
        return new Object();
    }

    /**
     * Creates a circuit breaker for the product service
     *
     * @param circuitBreakerRegistry The circuit breaker registry
     * @return The circuit breaker
     */
    public Object productServiceCircuitBreaker(Object circuitBreakerRegistry) {
        return new Object();
    }

    /**
     * Creates a retry template for the product service
     *
     * @return The retry template
     */
    public Object productServiceRetryTemplate() {
        return new Object();
    }
}