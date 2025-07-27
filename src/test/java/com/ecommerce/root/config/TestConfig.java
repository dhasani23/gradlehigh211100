package com.ecommerce.root.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Test configuration that provides test versions of the required beans.
 */
@Configuration
public class TestConfig {
    
    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    @Primary
    public CircuitBreaker testCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(10))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(5)
                .build();
                
        return CircuitBreaker.of("testCircuitBreaker", config);
    }
    
    @Bean
    @Primary
    public RetryTemplate testRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1);
        backOffPolicy.setMaxInterval(10);
        backOffPolicy.setMultiplier(1.5);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(2);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        retryTemplate.setListeners(new RetryListener[]{
            new RetryListener() {
                @Override
                public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                    return true;
                }

                @Override
                public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                    // No-op
                }

                @Override
                public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                    System.err.println("Retry error: " + throwable.getMessage());
                }
            }
        });
        
        return retryTemplate;
    }
}