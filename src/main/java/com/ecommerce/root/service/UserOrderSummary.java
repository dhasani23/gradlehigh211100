package com.ecommerce.root.service;

/**
 * UserOrderSummary - Aggregated data model containing user information and order history.
 * This model is used for combined data responses from multiple microservices.
 */
public class UserOrderSummary {

    private Object userData;
    private Object[] orderData;
    
    /**
     * Constructs a new UserOrderSummary with user data and order array.
     * 
     * @param userData the user profile data
     * @param orderData array of user orders
     */
    public UserOrderSummary(Object userData, Object[] orderData) {
        this.userData = userData;
        this.orderData = orderData;
    }

    /**
     * Gets the user data.
     * 
     * @return the user data
     */
    public Object getUserData() {
        return userData;
    }

    /**
     * Sets the user data.
     * 
     * @param userData the user data to set
     */
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /**
     * Gets the order data array.
     * 
     * @return the order data array
     */
    public Object[] getOrderData() {
        return orderData;
    }

    /**
     * Sets the order data array.
     * 
     * @param orderData the order data array to set
     */
    public void setOrderData(Object[] orderData) {
        this.orderData = orderData;
    }
}