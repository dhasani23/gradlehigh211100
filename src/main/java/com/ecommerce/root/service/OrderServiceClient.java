package com.ecommerce.root.service;

import java.util.List;
import java.util.Map;

/**
 * OrderServiceClient - Client interface for communication with the Order Service.
 * This interface defines methods for interacting with order-related functionality.
 */
public interface OrderServiceClient {
    
    /**
     * Sends a request to the Order Service.
     * 
     * @param serviceUrl the base URL of the service
     * @param endpoint the specific endpoint to call
     * @param payload the request payload
     * @return the response from the service
     * @throws Exception if the request fails
     */
    Object sendRequest(String serviceUrl, String endpoint, Object payload) throws Exception;
    
    /**
     * Gets order details by order ID.
     * 
     * @param orderId the ID of the order
     * @return the order details
     * @throws Exception if the request fails
     */
    Object getOrderById(Long orderId) throws Exception;
    
    /**
     * Gets orders for a user.
     * 
     * @param userId the ID of the user
     * @return the user's orders
     * @throws Exception if the request fails
     */
    Object[] getOrdersForUser(Long userId) throws Exception;
    
    /**
     * Creates a new order.
     * 
     * @param userId the ID of the user
     * @param orderItems the order items
     * @return the created order
     * @throws Exception if the request fails
     */
    Object createOrder(Long userId, List<Map<String, Object>> orderItems) throws Exception;
    
    /**
     * Updates the status of an order.
     * 
     * @param orderId the ID of the order
     * @param status the new status
     * @return the updated order
     * @throws Exception if the request fails
     */
    Object updateOrderStatus(Long orderId, String status) throws Exception;
}