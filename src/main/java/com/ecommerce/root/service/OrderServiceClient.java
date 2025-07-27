package com.ecommerce.root.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;

/**
 * Client service for communicating with Order Processing module with circuit breaker and retry logic.
 * This service provides high-level methods for interacting with the Order Service API, with built-in
 * resilience patterns to handle failures and prevent cascading failures.
 * 
 * @author ecommerce-team
 */
@Service
public class OrderServiceClient {
    
    private static final Logger LOGGER = Logger.getLogger(OrderServiceClient.class.getName());
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int CIRCUIT_BREAKER_TIMEOUT = 5000; // 5 seconds
    private static final int CIRCUIT_BREAKER_RESET_TIMEOUT = 30000; // 30 seconds
    private static final float FAILURE_RATE_THRESHOLD = 50.0f;
    
    private final String orderServiceUrl;
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param orderServiceUrl Base URL for Order Service
     * @param restTemplate REST template for HTTP communication
     */
    @Autowired
    public OrderServiceClient(
            @Value("${order.service.url}") String orderServiceUrl,
            RestTemplate restTemplate) {
        
        this.orderServiceUrl = orderServiceUrl;
        this.restTemplate = restTemplate;
        
        // Configure and initialize Circuit Breaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .waitDurationInOpenState(java.time.Duration.ofMillis(CIRCUIT_BREAKER_RESET_TIMEOUT))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderServiceCircuitBreaker");
        
        // Configure retry mechanism with exponential backoff
        this.retryTemplate = new RetryTemplate();
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500); // 500ms initial interval
        backOffPolicy.setMultiplier(2.0); // exponential multiplier
        backOffPolicy.setMaxInterval(5000); // 5s maximum interval
        
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.registerListener(new OrderServiceRetryListener());
    }
    
    /**
     * Get order information by order ID from Order Service.
     * 
     * @param orderId ID of the order to retrieve
     * @return OrderDto containing order information
     * @throws OrderServiceException if the order cannot be retrieved after multiple attempts
     */
    public OrderDto getOrderById(Long orderId) {
        LOGGER.info("Retrieving order with ID: " + orderId);
        
        String endpoint = orderServiceUrl + "/orders/" + orderId;
        
        CheckedFunction0<OrderDto> orderSupplier = () -> retryTemplate.execute(context -> {
            try {
                ResponseEntity<OrderDto> response = restTemplate.getForEntity(endpoint, OrderDto.class);
                return response.getBody();
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    LOGGER.warning("Order not found with ID: " + orderId);
                    return null;
                }
                throw new OrderServiceException("Client error retrieving order " + orderId, ex);
            } catch (Exception ex) {
                LOGGER.warning("Failed to retrieve order " + orderId + ": " + ex.getMessage());
                throw new OrderServiceException("Error retrieving order " + orderId, ex);
            }
        });
        
        try {
            return Try.of(CircuitBreaker.decorateCheckedSupplier(circuitBreaker, orderSupplier))
                    .getOrElseThrow(ex -> new OrderServiceException("Circuit breaker prevented order retrieval", ex));
        } catch (OrderServiceException ex) {
            LOGGER.severe("Failed to get order after multiple attempts: " + ex.getMessage());
            throw ex;
        } catch (Throwable t) {
            LOGGER.severe("Unexpected error retrieving order: " + t.getMessage());
            throw new OrderServiceException("Unexpected error retrieving order", t);
        }
    }
    
    /**
     * Create new order via Order Service.
     * 
     * @param orderRequest Request object containing order details
     * @return OrderDto containing created order information
     * @throws OrderServiceException if the order cannot be created after multiple attempts
     */
    public OrderDto createOrder(CreateOrderRequest orderRequest) {
        LOGGER.info("Creating new order for user: " + orderRequest.getUserId());
        
        if (orderRequest == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }
        
        validateCreateOrderRequest(orderRequest);
        
        String endpoint = orderServiceUrl + "/orders";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<CreateOrderRequest> requestEntity = new HttpEntity<>(orderRequest, headers);
        
        CheckedFunction0<OrderDto> orderSupplier = () -> retryTemplate.execute(context -> {
            try {
                ResponseEntity<OrderDto> response = restTemplate.postForEntity(
                        endpoint,
                        requestEntity,
                        OrderDto.class);
                
                LOGGER.info("Order created successfully with ID: " + 
                        (response.getBody() != null ? response.getBody().getOrderId() : "unknown"));
                        
                return response.getBody();
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    LOGGER.warning("Invalid order request: " + ex.getResponseBodyAsString());
                    throw new OrderServiceException("Invalid order request", ex, false); // Don't retry on validation errors
                }
                LOGGER.warning("Client error creating order: " + ex.getStatusCode());
                throw new OrderServiceException("Client error creating order", ex);
            } catch (HttpServerErrorException ex) {
                LOGGER.warning("Server error creating order: " + ex.getStatusCode());
                throw new OrderServiceException("Server error creating order", ex);
            } catch (Exception ex) {
                LOGGER.warning("Error creating order: " + ex.getMessage());
                throw new OrderServiceException("Error creating order", ex);
            }
        });
        
        try {
            return Try.of(CircuitBreaker.decorateCheckedSupplier(circuitBreaker, orderSupplier))
                    .getOrElseThrow(ex -> new OrderServiceException("Circuit breaker prevented order creation", ex));
        } catch (OrderServiceException ex) {
            if (!ex.isRetryable()) {
                throw ex; // Don't wrap non-retryable exceptions
            }
            LOGGER.severe("Failed to create order after multiple attempts: " + ex.getMessage());
            throw new OrderServiceException("Failed to create order after multiple attempts", ex);
        } catch (Throwable t) {
            LOGGER.severe("Unexpected error creating order: " + t.getMessage());
            throw new OrderServiceException("Unexpected error creating order", t);
        }
    }
    
    /**
     * Get orders for a specific user from Order Service.
     * 
     * @param userId ID of the user whose orders to retrieve
     * @return List of OrderDto objects
     * @throws OrderServiceException if the orders cannot be retrieved after multiple attempts
     */
    public List<OrderDto> getOrdersByUser(Long userId) {
        LOGGER.info("Retrieving orders for user: " + userId);
        
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        
        String endpoint = orderServiceUrl + "/orders";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("userId", userId);
                
        CheckedFunction0<List<OrderDto>> ordersSupplier = () -> retryTemplate.execute(context -> {
            try {
                ResponseEntity<OrderDto[]> response = restTemplate.getForEntity(
                        builder.build().toUri(),
                        OrderDto[].class);
                
                if (response.getBody() == null) {
                    return Collections.emptyList();
                }
                
                return Arrays.asList(response.getBody());
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    LOGGER.warning("No orders found for user: " + userId);
                    return Collections.emptyList();
                }
                throw new OrderServiceException("Client error retrieving orders for user " + userId, ex);
            } catch (Exception ex) {
                LOGGER.warning("Error retrieving orders for user " + userId + ": " + ex.getMessage());
                throw new OrderServiceException("Error retrieving orders for user " + userId, ex);
            }
        });
        
        try {
            return Try.of(CircuitBreaker.decorateCheckedSupplier(circuitBreaker, ordersSupplier))
                    .getOrElseThrow(ex -> new OrderServiceException("Circuit breaker prevented order retrieval", ex));
        } catch (OrderServiceException ex) {
            LOGGER.severe("Failed to get orders after multiple attempts: " + ex.getMessage());
            throw ex;
        } catch (Throwable t) {
            LOGGER.severe("Unexpected error retrieving orders: " + t.getMessage());
            throw new OrderServiceException("Unexpected error retrieving orders", t);
        }
    }
    
    /**
     * Check if Order Service is healthy and responsive.
     * 
     * @return true if service is responsive, false otherwise
     */
    public boolean checkServiceHealth() {
        LOGGER.info("Checking Order Service health");
        
        String endpoint = orderServiceUrl + "/actuator/health";
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(endpoint, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                LOGGER.info("Order Service health status: " + status);
                return "UP".equals(status);
            } else {
                LOGGER.warning("Order Service health check returned unexpected status: " + response.getStatusCode());
                return false;
            }
        } catch (Exception ex) {
            LOGGER.warning("Order Service health check failed: " + ex.getMessage());
            return false;
        }
    }
    
    /**
     * Validate the create order request parameters.
     * 
     * @param request The order request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCreateOrderRequest(CreateOrderRequest request) {
        List<String> errors = new ArrayList<>();
        
        if (request.getUserId() == null || request.getUserId() <= 0) {
            errors.add("Invalid user ID");
        }
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            errors.add("Order must contain at least one item");
        } else {
            // Validate individual items
            for (int i = 0; i < request.getItems().size(); i++) {
                OrderItemDto item = request.getItems().get(i);
                
                if (item.getProductId() == null || item.getProductId() <= 0) {
                    errors.add("Item #" + (i+1) + ": Invalid product ID");
                }
                
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    errors.add("Item #" + (i+1) + ": Quantity must be greater than 0");
                }
            }
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid order request: " + String.join(", ", errors));
        }
    }
    
    /**
     * Custom listener for retry events.
     */
    private static class OrderServiceRetryListener extends RetryListenerSupport {
        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            int retryCount = context.getRetryCount();
            LOGGER.warning("Retry attempt " + retryCount + " failed with error: " + throwable.getMessage());
            
            // FIXME: Add metrics tracking for retry attempts and failures
            
            if (retryCount >= MAX_RETRY_ATTEMPTS - 1) {
                LOGGER.severe("Maximum retry attempts reached (" + MAX_RETRY_ATTEMPTS + ")");
            }
        }
    }
    
    /**
     * Custom exception for Order Service errors.
     */
    public static class OrderServiceException extends RuntimeException {
        private final boolean retryable;
        
        public OrderServiceException(String message, Throwable cause) {
            this(message, cause, true);
        }
        
        public OrderServiceException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
    
    // TODO: Create additional methods for order management (cancel, update, etc.)
    // TODO: Add metrics collection for circuit breaker and retry performance
    // TODO: Implement bulkhead pattern for API rate limiting
}