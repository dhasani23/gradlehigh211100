package com.ecommerce.root.service;

import com.ecommerce.root.exception.UserServiceException;
import com.ecommerce.root.exception.UserServiceException.ErrorCode;
import com.ecommerce.root.model.AuthenticationResponse;
import com.ecommerce.root.model.CreateUserRequest;
import com.ecommerce.root.model.LoginRequest;
import com.ecommerce.root.model.UserDto;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Client service for communicating with User Service module with circuit breaker and retry logic.
 * This service handles communication with the User Service API, including:
 * - User authentication
 * - User retrieval
 * - User creation
 * - Service health checks
 *
 * It implements fault tolerance patterns including:
 * - Circuit breaker to prevent cascading failures
 * - Retry logic with exponential backoff
 * - Timeout handling
 */
@Service
public class UserServiceClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceClient.class);
    
    private static final String USER_API_PATH = "/api/users";
    private static final String AUTH_API_PATH = "/api/auth";
    private static final String HEALTH_CHECK_PATH = "/actuator/health";
    
    // Error messages
    private static final String ERR_RETRIEVE_USER = "Failed to retrieve user with ID: ";
    private static final String ERR_AUTH_USER = "Failed to authenticate user: ";
    private static final String ERR_CREATE_USER = "Failed to create user: ";
    private static final String ERR_SERVICE_UNAVAILABLE = "User service is unavailable";
    
    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private CircuitBreaker circuitBreaker;
    private RetryTemplate retryTemplate;
    
    /**
     * Initialize circuit breaker and retry template with configuration
     */
    @PostConstruct
    public void init() {
        initCircuitBreaker();
        initRetryTemplate();
        LOGGER.info("UserServiceClient initialized with userServiceUrl: {}", userServiceUrl);
    }
    
    /**
     * Initialize circuit breaker with custom configuration
     */
    private void initCircuitBreaker() {
        // Configure circuit breaker with high complexity parameters
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100)
                .minimumNumberOfCalls(20)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        HttpServerErrorException.class,
                        ResourceAccessException.class,
                        TimeoutException.class)
                .ignoreExceptions(HttpClientErrorException.class)
                .build();
        
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("userServiceCircuitBreaker");
        
        // Add event listeners for monitoring circuit breaker state
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> LOGGER.warn("Circuit breaker state changed: {} -> {}", 
                        event.getStateTransition().getFromState(), 
                        event.getStateTransition().getToState()))
                .onError(event -> LOGGER.error("Circuit breaker call failed: {}", event.getThrowable().getMessage()))
                .onSuccess(event -> LOGGER.debug("Circuit breaker call succeeded"));
    }
    
    /**
     * Initialize retry template with exponential backoff policy
     */
    private void initRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        
        // Configure retry policy with complex conditions
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(ResourceAccessException.class, true);
        retryableExceptions.put(HttpServerErrorException.class, true);
        retryableExceptions.put(TimeoutException.class, true);
        retryableExceptions.put(HttpClientErrorException.class, false);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);
        template.setRetryPolicy(retryPolicy);
        
        // Set backoff policy with increasing delays
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);
        template.setBackOffPolicy(backOffPolicy);
        
        // Set listeners to log retry attempts
        template.registerListener(new RetryLoggingListener());
        
        this.retryTemplate = template;
    }
    
    /**
     * Get user information by user ID from User Service
     * 
     * @param userId User ID to retrieve
     * @return User DTO containing user information
     * @throws UserServiceException if user retrieval fails
     */
    public UserDto getUserById(Long userId) {
        LOGGER.debug("Getting user with ID: {}", userId);
        
        if (userId == null || userId <= 0) {
            LOGGER.error("Invalid user ID provided: {}", userId);
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        String url = userServiceUrl + USER_API_PATH + "/" + userId;
        
        try {
            // Apply circuit breaker pattern with retry logic
            return executeWithCircuitBreaker(() -> {
                try {
                    return retryTemplate.execute((RetryContext context) -> {
                        HttpHeaders headers = createStandardHeaders();
                        HttpEntity<?> entity = new HttpEntity<>(headers);
                        
                        LOGGER.debug("Sending request to: {}", url);
                        ResponseEntity<UserDto> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                UserDto.class);
                        
                        if (response.getStatusCode().is2xxSuccessful()) {
                            UserDto user = response.getBody();
                            if (user != null) {
                                LOGGER.debug("Successfully retrieved user with ID: {}", userId);
                                return user;
                            } else {
                                LOGGER.warn("User service returned empty response for ID: {}", userId);
                                throw new UserServiceException("User not found with ID: " + userId, 
                                        ErrorCode.USER_NOT_FOUND);
                            }
                        } else {
                            LOGGER.warn("Unexpected response status: {} for user ID: {}", 
                                    response.getStatusCodeValue(), userId);
                            throw new UserServiceException("Unexpected response from user service: " + 
                                    response.getStatusCodeValue(), ErrorCode.GENERAL_ERROR);
                        }
                    });
                } catch (UserServiceException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOGGER.error("Error retrieving user with ID: {}", userId, ex);
                    throw translateException(ex, ERR_RETRIEVE_USER + userId);
                }
            });
        } catch (Exception ex) {
            handleServiceException(ex, ERR_RETRIEVE_USER + userId);
            throw translateException(ex, ERR_RETRIEVE_USER + userId);
        }
    }
    
    /**
     * Authenticate user credentials via User Service
     * 
     * @param credentials Login credentials including username and password
     * @return Authentication response containing token and user information
     * @throws UserServiceException if authentication fails
     */
    public AuthenticationResponse authenticateUser(LoginRequest credentials) {
        LOGGER.debug("Authenticating user: {}", credentials.getUsername());
        
        // Validate input
        if (credentials == null || credentials.getUsername() == null || credentials.getPassword() == null) {
            LOGGER.error("Invalid login credentials provided");
            throw new IllegalArgumentException("Username and password are required");
        }
        
        String url = userServiceUrl + AUTH_API_PATH + "/login";
        
        try {
            // Apply circuit breaker pattern with retry logic
            return executeWithCircuitBreaker(() -> {
                try {
                    return retryTemplate.execute((RetryContext context) -> {
                        HttpHeaders headers = createStandardHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<LoginRequest> entity = new HttpEntity<>(credentials, headers);
                        
                        LOGGER.debug("Sending authentication request for user: {}", credentials.getUsername());
                        ResponseEntity<AuthenticationResponse> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                AuthenticationResponse.class);
                        
                        if (response.getStatusCode().is2xxSuccessful()) {
                            AuthenticationResponse authResponse = response.getBody();
                            if (authResponse != null && authResponse.getToken() != null) {
                                LOGGER.info("Authentication successful for user: {}", credentials.getUsername());
                                return authResponse;
                            } else {
                                LOGGER.warn("Authentication response invalid for user: {}", credentials.getUsername());
                                throw new UserServiceException("Authentication failed - invalid response", 
                                        ErrorCode.AUTHENTICATION_ERROR);
                            }
                        } else {
                            LOGGER.warn("Authentication failed with status: {} for user: {}", 
                                    response.getStatusCodeValue(), credentials.getUsername());
                            throw new UserServiceException("Authentication failed with status: " + 
                                    response.getStatusCodeValue(), ErrorCode.AUTHENTICATION_ERROR);
                        }
                    });
                } catch (UserServiceException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOGGER.error("Error during authentication for user: {}", credentials.getUsername(), ex);
                    throw translateException(ex, ERR_AUTH_USER + credentials.getUsername());
                }
            });
        } catch (Exception ex) {
            handleServiceException(ex, ERR_AUTH_USER + credentials.getUsername());
            throw translateException(ex, ERR_AUTH_USER + credentials.getUsername());
        }
    }
    
    /**
     * Create new user via User Service
     * 
     * @param userRequest User creation details
     * @return UserDto containing created user information
     * @throws UserServiceException if user creation fails
     */
    public UserDto createUser(CreateUserRequest userRequest) {
        LOGGER.debug("Creating new user: {}", userRequest.getUsername());
        
        // Validate input
        if (userRequest == null || userRequest.getUsername() == null || userRequest.getEmail() == null) {
            LOGGER.error("Invalid user creation request");
            throw new IllegalArgumentException("Username and email are required");
        }
        
        if (!userRequest.passwordsMatch()) {
            LOGGER.error("Password mismatch in user creation request");
            throw new UserServiceException("Password and confirm password do not match", 
                    ErrorCode.VALIDATION_ERROR);
        }
        
        String url = userServiceUrl + USER_API_PATH;
        
        try {
            // Apply circuit breaker pattern with retry logic
            return executeWithCircuitBreaker(() -> {
                try {
                    return retryTemplate.execute((RetryContext context) -> {
                        HttpHeaders headers = createStandardHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(userRequest, headers);
                        
                        LOGGER.debug("Sending user creation request for: {}", userRequest.getUsername());
                        ResponseEntity<UserDto> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                UserDto.class);
                        
                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            UserDto createdUser = response.getBody();
                            if (createdUser != null) {
                                LOGGER.info("Successfully created user: {}", userRequest.getUsername());
                                return createdUser;
                            } else {
                                LOGGER.warn("User service returned empty response for created user: {}", 
                                        userRequest.getUsername());
                                throw new UserServiceException("Empty response after user creation", 
                                        ErrorCode.GENERAL_ERROR);
                            }
                        } else {
                            LOGGER.warn("User creation failed with status: {} for user: {}", 
                                    response.getStatusCodeValue(), userRequest.getUsername());
                            throw new UserServiceException("User creation failed with status: " + 
                                    response.getStatusCodeValue(), ErrorCode.GENERAL_ERROR);
                        }
                    });
                } catch (UserServiceException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOGGER.error("Error creating user: {}", userRequest.getUsername(), ex);
                    throw translateException(ex, ERR_CREATE_USER + userRequest.getUsername());
                }
            });
        } catch (Exception ex) {
            handleServiceException(ex, ERR_CREATE_USER + userRequest.getUsername());
            throw translateException(ex, ERR_CREATE_USER + userRequest.getUsername());
        }
    }
    
    /**
     * Check if User Service is healthy and responsive
     * 
     * @return true if service is healthy, false otherwise
     */
    public boolean checkServiceHealth() {
        LOGGER.debug("Checking User Service health");
        
        String url = userServiceUrl + HEALTH_CHECK_PATH;
        
        try {
            // Don't use circuit breaker for health check to prevent false positives
            return retryTemplate.execute((RetryContext context) -> {
                try {
                    HttpHeaders headers = createStandardHeaders();
                    HttpEntity<?> entity = new HttpEntity<>(headers);
                    
                    LOGGER.debug("Sending health check request to: {}", url);
                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> health = response.getBody();
                        String status = Objects.toString(health.get("status"), "");
                        
                        boolean isUp = "UP".equalsIgnoreCase(status);
                        LOGGER.info("User service health status: {}", status);
                        return isUp;
                    } else {
                        LOGGER.warn("Health check failed with status: {}", 
                                response.getStatusCodeValue());
                        return false;
                    }
                } catch (Exception ex) {
                    LOGGER.error("Health check failed", ex);
                    return false;
                }
            });
        } catch (Exception ex) {
            LOGGER.error("Health check failed with exception", ex);
            return false;
        }
    }
    
    /**
     * Execute a function with circuit breaker protection
     * 
     * @param <T> Return type
     * @param supplier Function to execute
     * @return Result of the function
     */
    private <T> T executeWithCircuitBreaker(Supplier<T> supplier) {
        // Use circuit breaker pattern to prevent cascading failures
        return circuitBreaker.executeSupplier(supplier);
    }
    
    /**
     * Create standard HTTP headers for API requests
     * 
     * @return HttpHeaders with standard headers
     */
    private HttpHeaders createStandardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "UserServiceClient/1.0");
        // FIXME: Add authorization token if needed
        return headers;
    }
    
    /**
     * Translate exceptions to UserServiceException
     * 
     * @param ex Original exception
     * @param defaultMessage Default message for the exception
     * @return Translated UserServiceException
     */
    private UserServiceException translateException(Exception ex, String defaultMessage) {
        if (ex instanceof UserServiceException) {
            return (UserServiceException) ex;
        }
        
        if (ex instanceof HttpClientErrorException.NotFound) {
            return new UserServiceException("Resource not found: " + ex.getMessage(), 
                    ErrorCode.USER_NOT_FOUND);
        }
        
        if (ex instanceof HttpClientErrorException.Unauthorized || 
            ex instanceof HttpClientErrorException.Forbidden) {
            return new UserServiceException("Authentication error: " + ex.getMessage(), 
                    ErrorCode.AUTHENTICATION_ERROR);
        }
        
        if (ex instanceof ResourceAccessException) {
            return new UserServiceException("Connection error: " + ex.getMessage(), 
                    ErrorCode.CONNECTION_ERROR);
        }
        
        if (ex instanceof HttpServerErrorException) {
            return new UserServiceException("Server error: " + ex.getMessage(), 
                    ErrorCode.SERVICE_UNAVAILABLE);
        }
        
        if (ex instanceof TimeoutException) {
            return new UserServiceException("Request timed out: " + ex.getMessage(), 
                    ErrorCode.TIMEOUT_ERROR);
        }
        
        // Handle circuit breaker specific exceptions
        if (ex.getMessage() != null && ex.getMessage().contains("CircuitBreaker")) {
            return new UserServiceException("Service unavailable (circuit open): " + ex.getMessage(), 
                    ErrorCode.CIRCUIT_OPEN);
        }
        
        return new UserServiceException(defaultMessage, ex, ErrorCode.GENERAL_ERROR);
    }
    
    /**
     * Handle service exception with appropriate logging
     * 
     * @param ex Exception to handle
     * @param message Error message
     */
    private void handleServiceException(Exception ex, String message) {
        if (ex instanceof UserServiceException) {
            UserServiceException userEx = (UserServiceException) ex;
            if (userEx.getErrorCode() == ErrorCode.CIRCUIT_OPEN) {
                LOGGER.error("Circuit breaker is open. User service is unavailable.");
            } else if (userEx.getErrorCode() == ErrorCode.CONNECTION_ERROR) {
                LOGGER.error("Cannot connect to User Service at: {}", userServiceUrl);
            } else {
                LOGGER.error(message, ex);
            }
        } else {
            LOGGER.error(message, ex);
        }
    }
    
    /**
     * Listener class to log retry attempts
     */
    private static class RetryLoggingListener implements org.springframework.retry.RetryListener {
        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryPolicy retryPolicy) {
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryPolicy retryPolicy, Throwable throwable) {
            // No action needed on close
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryPolicy retryPolicy, Throwable throwable) {
            LOGGER.warn("Retry attempt {} failed: {}", context.getRetryCount(), throwable.getMessage());
        }
    }
    
    // Getters and setters for testing and configuration
    
    public void setUserServiceUrl(String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
    }
    
    public String getUserServiceUrl() {
        return userServiceUrl;
    }
    
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    public void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }
}