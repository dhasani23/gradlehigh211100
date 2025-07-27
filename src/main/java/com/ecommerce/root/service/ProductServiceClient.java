package com.ecommerce.root.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Client service for communicating with Product Catalog module with circuit breaker and retry logic.
 * This service encapsulates all communication with the Product Service API and provides fault tolerance
 * mechanisms through circuit breaker pattern and retry policies.
 */
@Service
public class ProductServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);
    
    /**
     * Base URL for Product Service API calls
     */
    @Value("${product.service.url}")
    private String productServiceUrl;
    
    /**
     * REST template for HTTP communication
     */
    private final RestTemplate restTemplate;
    
    /**
     * Circuit breaker for fault tolerance
     */
    private final CircuitBreaker circuitBreaker;
    
    /**
     * Retry template for failed requests
     */
    private final RetryTemplate retryTemplate;
    
    /**
     * Constructor with dependency injection
     * 
     * @param restTemplate REST template for making HTTP requests
     * @param circuitBreaker Circuit breaker instance for fault tolerance
     * @param retryTemplate Retry template for failed requests
     */
    @Autowired
    public ProductServiceClient(
            RestTemplate restTemplate,
            CircuitBreaker circuitBreaker,
            RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.retryTemplate = retryTemplate;
    }
    
    /**
     * Get product information by product ID from Product Service
     * 
     * @param productId the ID of the product to retrieve
     * @return ProductDto containing product details
     * @throws ProductServiceException if the product cannot be retrieved
     */
    public ProductDto getProductById(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive number");
        }
        
        logger.debug("Fetching product with ID: {}", productId);
        
        String endpoint = productServiceUrl + "/api/products/" + productId;
        
        try {
            // Execute request with circuit breaker and retry logic
            return executeWithFaultTolerance(() -> {
                ResponseEntity<ProductDto> response = restTemplate.getForEntity(endpoint, ProductDto.class);
                return response.getBody();
            }, "getProductById");
        } catch (Exception ex) {
            logger.error("Failed to retrieve product with ID: {}", productId, ex);
            
            // Handle different types of exceptions
            if (ex instanceof HttpClientErrorException.NotFound) {
                logger.warn("Product with ID {} not found", productId);
                return null;
            } else if (ex instanceof HttpServerErrorException) {
                throw new ProductServiceException("Product service server error", ex);
            } else if (ex instanceof ResourceAccessException) {
                throw new ProductServiceException("Cannot connect to product service", ex);
            } else {
                throw new ProductServiceException("Error retrieving product: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Search products based on criteria via Product Service
     * 
     * @param searchCriteria the search request containing filtering parameters
     * @return List of ProductDto matching the search criteria
     * @throws ProductServiceException if the search operation fails
     */
    public List<ProductDto> searchProducts(ProductSearchRequest searchCriteria) {
        if (searchCriteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }
        
        logger.debug("Searching products with criteria: {}", searchCriteria);
        
        String endpoint = productServiceUrl + "/api/products/search";
        
        try {
            // Build query parameters based on search criteria
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
            
            // Apply all available search parameters
            if (searchCriteria.getName() != null) {
                builder.queryParam("name", searchCriteria.getName());
            }
            
            if (searchCriteria.getMinPrice() != null) {
                builder.queryParam("minPrice", searchCriteria.getMinPrice());
            }
            
            if (searchCriteria.getMaxPrice() != null) {
                builder.queryParam("maxPrice", searchCriteria.getMaxPrice());
            }
            
            if (searchCriteria.getBrand() != null) {
                builder.queryParam("brand", searchCriteria.getBrand());
            }
            
            if (searchCriteria.getSortBy() != null) {
                builder.queryParam("sortBy", searchCriteria.getSortBy());
            }
            
            if (searchCriteria.getSortDirection() != null) {
                builder.queryParam("sortDirection", searchCriteria.getSortDirection());
            }
            
            if (searchCriteria.getPage() != null) {
                builder.queryParam("page", searchCriteria.getPage());
            }
            
            if (searchCriteria.getPageSize() != null) {
                builder.queryParam("pageSize", searchCriteria.getPageSize());
            }
            
            // Additional custom parameters
            if (searchCriteria.getCustomParameters() != null) {
                for (Map.Entry<String, String> entry : searchCriteria.getCustomParameters().entrySet()) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
            
            URI uri = builder.build().toUri();
            
            // Execute with fault tolerance
            return executeWithFaultTolerance(() -> {
                ResponseEntity<List<ProductDto>> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ProductDto>>() {});
                
                return response.getBody() != null ? response.getBody() : Collections.emptyList();
            }, "searchProducts");
            
        } catch (Exception ex) {
            logger.error("Failed to search products with criteria: {}", searchCriteria, ex);
            
            // Handle specific exceptions
            if (ex instanceof HttpServerErrorException) {
                throw new ProductServiceException("Product service server error during search", ex);
            } else if (ex instanceof ResourceAccessException) {
                throw new ProductServiceException("Cannot connect to product service during search", ex);
            } else {
                throw new ProductServiceException("Error searching products: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Get products by category from Product Service
     * 
     * @param categoryId the category ID to filter products
     * @return List of ProductDto in the specified category
     * @throws ProductServiceException if the operation fails
     */
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be a positive number");
        }
        
        logger.debug("Fetching products for category ID: {}", categoryId);
        
        String endpoint = productServiceUrl + "/api/products/category/" + categoryId;
        
        try {
            // Execute with fault tolerance
            return executeWithFaultTolerance(() -> {
                ResponseEntity<List<ProductDto>> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ProductDto>>() {});
                
                return response.getBody() != null ? response.getBody() : Collections.emptyList();
            }, "getProductsByCategory");
            
        } catch (Exception ex) {
            logger.error("Failed to retrieve products for category ID: {}", categoryId, ex);
            
            // Handle specific exceptions
            if (ex instanceof HttpClientErrorException.NotFound) {
                logger.warn("No products found for category ID: {}", categoryId);
                return Collections.emptyList();
            } else if (ex instanceof HttpServerErrorException) {
                throw new ProductServiceException("Product service server error for category request", ex);
            } else if (ex instanceof ResourceAccessException) {
                throw new ProductServiceException("Cannot connect to product service for category request", ex);
            } else {
                throw new ProductServiceException("Error retrieving products by category: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Check if Product Service is healthy and responsive
     * 
     * @return true if service is healthy, false otherwise
     */
    public boolean checkServiceHealth() {
        logger.debug("Checking product service health status");
        
        String endpoint = productServiceUrl + "/actuator/health";
        
        try {
            // Simple health check without circuit breaker to avoid recursive health checks
            ResponseEntity<Map> response = restTemplate.getForEntity(endpoint, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                boolean isUp = "UP".equals(status);
                
                if (isUp) {
                    logger.info("Product service health check passed: {}", status);
                } else {
                    logger.warn("Product service health check returned non-UP status: {}", status);
                }
                
                return isUp;
            } else {
                logger.warn("Product service health check failed: Invalid response");
                return false;
            }
        } catch (Exception ex) {
            logger.warn("Product service health check failed with exception", ex);
            return false;
        }
    }
    
    /**
     * Execute a callable with circuit breaker and retry logic applied
     * 
     * @param supplier the callable to execute
     * @param operationName name of the operation for logging purposes
     * @param <T> the return type of the callable
     * @return the result of the callable execution
     * @throws Exception if the execution fails even after retries
     */
    private <T> T executeWithFaultTolerance(Callable<T> supplier, String operationName) throws Exception {
        // Convert callable to supplier for circuit breaker
        Supplier<T> circuitBreakerSupplier = () -> {
            try {
                // Use RetryTemplate to add retry logic
                return retryTemplate.execute(context -> {
                    try {
                        if (context.getRetryCount() > 0) {
                            logger.warn("Retry attempt {} for operation: {}", 
                                    context.getRetryCount(), operationName);
                        }
                        return supplier.call();
                    } catch (Exception e) {
                        logger.warn("Operation '{}' failed (attempt {}): {}", 
                                operationName, context.getRetryCount() + 1, e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
                throw e;
            }
        };
        
        // Apply circuit breaker pattern
        return circuitBreaker.executeSupplier(circuitBreakerSupplier);
    }
    
    /**
     * Exception class for Product Service errors
     */
    public static class ProductServiceException extends RuntimeException {
        public ProductServiceException(String message) {
            super(message);
        }
        
        public ProductServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Data class representing a product search request
     */
    public static class ProductSearchRequest {
        private String name;
        private Double minPrice;
        private Double maxPrice;
        private String brand;
        private String sortBy;
        private String sortDirection;
        private Integer page;
        private Integer pageSize;
        private Map<String, String> customParameters;
        
        public ProductSearchRequest() {
            this.customParameters = new HashMap<>();
        }
        
        // Builder methods for fluent API
        public ProductSearchRequest withName(String name) {
            this.name = name;
            return this;
        }
        
        public ProductSearchRequest withMinPrice(Double minPrice) {
            this.minPrice = minPrice;
            return this;
        }
        
        public ProductSearchRequest withMaxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }
        
        public ProductSearchRequest withBrand(String brand) {
            this.brand = brand;
            return this;
        }
        
        public ProductSearchRequest withSortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }
        
        public ProductSearchRequest withSortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }
        
        public ProductSearchRequest withPage(Integer page) {
            this.page = page;
            return this;
        }
        
        public ProductSearchRequest withPageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }
        
        public ProductSearchRequest withCustomParameter(String key, String value) {
            this.customParameters.put(key, value);
            return this;
        }
        
        // Getters and setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Double getMinPrice() {
            return minPrice;
        }
        
        public void setMinPrice(Double minPrice) {
            this.minPrice = minPrice;
        }
        
        public Double getMaxPrice() {
            return maxPrice;
        }
        
        public void setMaxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
        }
        
        public String getBrand() {
            return brand;
        }
        
        public void setBrand(String brand) {
            this.brand = brand;
        }
        
        public String getSortBy() {
            return sortBy;
        }
        
        public void setSortBy(String sortBy) {
            this.sortBy = sortBy;
        }
        
        public String getSortDirection() {
            return sortDirection;
        }
        
        public void setSortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
        }
        
        public Integer getPage() {
            return page;
        }
        
        public void setPage(Integer page) {
            this.page = page;
        }
        
        public Integer getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }
        
        public Map<String, String> getCustomParameters() {
            return customParameters;
        }
        
        public void setCustomParameters(Map<String, String> customParameters) {
            this.customParameters = customParameters;
        }
        
        @Override
        public String toString() {
            return "ProductSearchRequest{" +
                   "name='" + name + '\'' +
                   ", minPrice=" + minPrice +
                   ", maxPrice=" + maxPrice +
                   ", brand='" + brand + '\'' +
                   ", sortBy='" + sortBy + '\'' +
                   ", sortDirection='" + sortDirection + '\'' +
                   ", page=" + page +
                   ", pageSize=" + pageSize +
                   ", customParameters=" + customParameters +
                   '}';
        }
    }
    
    /**
     * Data Transfer Object for Product information
     */
    public static class ProductDto {
        private Long id;
        private String name;
        private String description;
        private Double price;
        private Long categoryId;
        private String brand;
        private String sku;
        private Integer stockLevel;
        private List<String> images;
        private Map<String, Object> attributes;
        
        // Default constructor
        public ProductDto() {
            this.images = new ArrayList<>();
            this.attributes = new HashMap<>();
        }
        
        // Getters and setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Double getPrice() {
            return price;
        }
        
        public void setPrice(Double price) {
            this.price = price;
        }
        
        public Long getCategoryId() {
            return categoryId;
        }
        
        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }
        
        public String getBrand() {
            return brand;
        }
        
        public void setBrand(String brand) {
            this.brand = brand;
        }
        
        public String getSku() {
            return sku;
        }
        
        public void setSku(String sku) {
            this.sku = sku;
        }
        
        public Integer getStockLevel() {
            return stockLevel;
        }
        
        public void setStockLevel(Integer stockLevel) {
            this.stockLevel = stockLevel;
        }
        
        public List<String> getImages() {
            return images;
        }
        
        public void setImages(List<String> images) {
            this.images = images;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
        
        @Override
        public String toString() {
            return "ProductDto{" +
                   "id=" + id +
                   ", name='" + name + '\'' +
                   ", price=" + price +
                   ", categoryId=" + categoryId +
                   ", brand='" + brand + '\'' +
                   ", sku='" + sku + '\'' +
                   ", stockLevel=" + stockLevel +
                   '}';
        }
    }
}