package com.ecommerce.root.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Product Service related beans.
 * Sets up RestTemplate, CircuitBreaker, and RetryTemplate with appropriate configurations.
 */
@Configuration
public class ProductServiceConfig {

    /**
     * Creates a configured RestTemplate for HTTP communication
     * 
     * @param builder the RestTemplateBuilder
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate productServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Creates a CircuitBreaker instance for the product service
     * 
     * @param circuitBreakerRegistry the registry containing circuit breaker configurations
     * @return CircuitBreaker instance
     */
    @Bean
    public CircuitBreaker productServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("productService");
    }

    /**
     * Creates a RetryTemplate with exponential backoff policy for product service calls
     * 
     * @return configured RetryTemplate
     */
    @Bean
    public RetryTemplate productServiceRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMaxInterval(10000);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Configure retry policy
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(Exception.class, true);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}