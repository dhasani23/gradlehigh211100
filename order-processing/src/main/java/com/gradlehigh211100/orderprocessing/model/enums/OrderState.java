package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enum representing the various states an order can be in throughout its lifecycle.
 * These states track the order from creation through processing, delivery and completion.
 */
public enum OrderState {
    /**
     * Initial state when an order is first created
     */
    NEW,
    
    /**
     * Order has been submitted but waiting for processing to begin
     */
    PENDING,
    
    /**
     * Order is currently being processed
     */
    PROCESSING,
    
    /**
     * Order requires payment before it can proceed
     */
    PAYMENT_REQUIRED,
    
    /**
     * Order has been shipped to the customer
     */
    SHIPPED,
    
    /**
     * Order processing is temporarily paused
     */
    ON_HOLD,
    
    /**
     * Order has been delivered to the customer
     */
    DELIVERED,
    
    /**
     * Order has been returned by the customer
     */
    RETURNED,
    
    /**
     * Customer has been refunded for the order
     */
    REFUNDED,
    
    /**
     * Order has been successfully completed
     */
    COMPLETED,
    
    /**
     * Order has been cancelled and will not be processed
     */
    CANCELLED;
    
    /**
     * Determines if this is a terminal state (no further state changes expected)
     * 
     * @return true if this is a terminal state
     */
    public boolean isTerminalState() {
        return this == COMPLETED || this == CANCELLED;
    }
    
    /**
     * Determines if this state represents an active order
     * 
     * @return true if the order is active in this state
     */
    public boolean isActiveState() {
        return this == NEW || this == PENDING || this == PROCESSING || 
               this == PAYMENT_REQUIRED || this == ON_HOLD || this == SHIPPED;
    }
    
    /**
     * Determines if the state requires customer action
     * 
     * @return true if customer action is required
     */
    public boolean requiresCustomerAction() {
        return this == PAYMENT_REQUIRED;
    }
}