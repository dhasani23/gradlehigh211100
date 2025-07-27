package com.ecommerce.root.service;

import java.util.Map;

/**
 * ProductServiceClient - Client interface for communication with the Product Service.
 * This interface defines methods for interacting with product-related functionality.
 */
public interface ProductServiceClient {
    
    /**
     * Sends a request to the Product Service.
     * 
     * @param serviceUrl the base URL of the service
     * @param endpoint the specific endpoint to call
     * @param payload the request payload
     * @return the response from the service
     * @throws Exception if the request fails
     */
    Object sendRequest(String serviceUrl, String endpoint, Object payload) throws Exception;
    
    /**
     * Gets product details by product ID.
     * 
     * @param productId the ID of the product
     * @return the product details
     * @throws Exception if the request fails
     */
    Object getProductById(Long productId) throws Exception;
    
    /**
     * Gets product inventory by product ID.
     * 
     * @param productId the ID of the product
     * @return the product inventory
     * @throws Exception if the request fails
     */
    Object getInventoryForProduct(Long productId) throws Exception;
    
    /**
     * Searches for products based on search criteria.
     * 
     * @param criteria the search criteria
     * @return the search results
     * @throws Exception if the request fails
     */
    Object searchProducts(Map<String, Object> criteria) throws Exception;
    
    /**
     * Gets product recommendations based on user ID or product ID.
     * 
     * @param parameters the recommendation parameters
     * @return the product recommendations
     * @throws Exception if the request fails
     */
    Object getRecommendations(Map<String, Object> parameters) throws Exception;
}