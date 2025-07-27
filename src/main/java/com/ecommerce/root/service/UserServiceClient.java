package com.ecommerce.root.service;

/**
 * UserServiceClient - Client interface for communication with the User Service.
 * This interface defines methods for interacting with user-related functionality.
 */
public interface UserServiceClient {
    
    /**
     * Sends a request to the User Service.
     * 
     * @param serviceUrl the base URL of the service
     * @param endpoint the specific endpoint to call
     * @param payload the request payload
     * @return the response from the service
     * @throws Exception if the request fails
     */
    Object sendRequest(String serviceUrl, String endpoint, Object payload) throws Exception;
    
    /**
     * Gets user details by user ID.
     * 
     * @param userId the ID of the user
     * @return the user details
     * @throws Exception if the request fails
     */
    Object getUserById(Long userId) throws Exception;
    
    /**
     * Gets user preferences by user ID.
     * 
     * @param userId the ID of the user
     * @return the user preferences
     * @throws Exception if the request fails
     */
    Object getUserPreferences(Long userId) throws Exception;
    
    /**
     * Updates user information.
     * 
     * @param userId the ID of the user
     * @param userData the updated user data
     * @return the updated user
     * @throws Exception if the request fails
     */
    Object updateUser(Long userId, Object userData) throws Exception;
}