package com.ecommerce.root.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private RetryTemplate retryTemplate;

    @InjectMocks
    private ProductServiceClientImpl productServiceClient;

    private final String baseUrl = "http://test-product-service:8080";

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(productServiceClient, "productServiceUrl", baseUrl);
        
        // Setup circuit breaker to directly execute the supplier
        when(circuitBreaker.executeSupplier(any())).thenAnswer(invocation -> {
            return ((java.util.function.Supplier<?>) invocation.getArgument(0)).get();
        });
        
        // Setup retry template to directly execute the callback
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return ((org.springframework.retry.RetryCallback<Object, Throwable>) invocation.getArgument(0)).doWithRetry(null);
        });
    }

    @Test
    public void testGetProductById_Success() {
        // Arrange
        ProductServiceClient.ProductDto expectedProduct = new ProductServiceClient.ProductDto();
        expectedProduct.setId(1L);
        expectedProduct.setName("Test Product");
        expectedProduct.setPrice(99.99);
        
        when(restTemplate.getForEntity(eq(baseUrl + "/api/products/1"), eq(ProductServiceClient.ProductDto.class)))
                .thenReturn(new ResponseEntity<>(expectedProduct, HttpStatus.OK));
        
        // Act
        ProductServiceClient.ProductDto result = productServiceClient.getProductById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Product", result.getName());
        assertEquals(99.99, result.getPrice());
    }

    @Test
    public void testGetProductById_NotFound() {
        // Arrange
        when(restTemplate.getForEntity(eq(baseUrl + "/api/products/999"), eq(ProductServiceClient.ProductDto.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        
        // Act
        ProductServiceClient.ProductDto result = productServiceClient.getProductById(999L);
        
        // Assert
        assertNull(result);
    }

    @Test
    public void testSearchProducts_Success() {
        // Arrange
        ProductServiceClient.ProductDto product1 = new ProductServiceClient.ProductDto();
        product1.setId(1L);
        product1.setName("Product 1");
        
        ProductServiceClient.ProductDto product2 = new ProductServiceClient.ProductDto();
        product2.setId(2L);
        product2.setName("Product 2");
        
        List<ProductServiceClient.ProductDto> expectedProducts = Arrays.asList(product1, product2);
        
        when(restTemplate.exchange(
                contains(baseUrl + "/api/products/search"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(expectedProducts, HttpStatus.OK));
        
        // Act
        ProductServiceClient.ProductSearchRequest request = new ProductServiceClient.ProductSearchRequest()
                .withBrand("TestBrand")
                .withMinPrice(10.0)
                .withMaxPrice(100.0);
        
        List<ProductServiceClient.ProductDto> result = productServiceClient.searchProducts(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Product 1", result.get(0).getName());
        assertEquals("Product 2", result.get(1).getName());
    }

    @Test
    public void testGetProductsByCategory_Success() {
        // Arrange
        ProductServiceClient.ProductDto product1 = new ProductServiceClient.ProductDto();
        product1.setId(1L);
        product1.setCategoryId(5L);
        
        List<ProductServiceClient.ProductDto> expectedProducts = Collections.singletonList(product1);
        
        when(restTemplate.exchange(
                eq(baseUrl + "/api/products/category/5"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(expectedProducts, HttpStatus.OK));
        
        // Act
        List<ProductServiceClient.ProductDto> result = productServiceClient.getProductsByCategory(5L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(5L, result.get(0).getCategoryId());
    }

    @Test
    public void testCheckServiceHealth_Success() {
        // Arrange
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");
        
        when(restTemplate.getForEntity(eq(baseUrl + "/actuator/health"), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(healthResponse, HttpStatus.OK));
        
        // Act
        boolean result = productServiceClient.checkServiceHealth();
        
        // Assert
        assertTrue(result);
    }

    @Test
    public void testCheckServiceHealth_Down() {
        // Arrange
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "DOWN");
        
        when(restTemplate.getForEntity(eq(baseUrl + "/actuator/health"), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(healthResponse, HttpStatus.OK));
        
        // Act
        boolean result = productServiceClient.checkServiceHealth();
        
        // Assert
        assertFalse(result);
    }
}