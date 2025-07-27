package com.ecommerce.root.service;

import com.ecommerce.root.config.TestConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@Import(TestConfig.class)
public class ProductServiceClientTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Autowired
    private RetryTemplate retryTemplate;
    
    private ProductServiceClientImpl productServiceClient;

    private final String baseUrl = "http://test-product-service:8080";

    @Before
    public void setup() {
        productServiceClient = new ProductServiceClientImpl();
        ReflectionTestUtils.setField(productServiceClient, "productServiceUrl", baseUrl);
        ReflectionTestUtils.setField(productServiceClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(productServiceClient, "circuitBreaker", circuitBreaker);
        ReflectionTestUtils.setField(productServiceClient, "retryTemplate", retryTemplate);
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
        assertEquals(1L, result.getId().longValue());
        assertEquals("Test Product", result.getName());
        assertEquals(99.99, result.getPrice().doubleValue(), 0.001);
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
        
        ResponseEntity<List<ProductServiceClient.ProductDto>> response = new ResponseEntity<>(expectedProducts, HttpStatus.OK);
        
        when(restTemplate.exchange(
                contains(baseUrl + "/api/products/search"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        
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
        
        ResponseEntity<List<ProductServiceClient.ProductDto>> response = new ResponseEntity<>(expectedProducts, HttpStatus.OK);
        
        when(restTemplate.exchange(
                eq(baseUrl + "/api/products/category/5"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        
        // Act
        List<ProductServiceClient.ProductDto> result = productServiceClient.getProductsByCategory(5L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId().longValue());
        assertEquals(5L, result.get(0).getCategoryId().longValue());
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