package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Represents the possible states of an order during its lifecycle.
 */
public enum OrderState {
    /**
     * Order has been created but not yet processed
     */
    CREATED,
    
    /**
     * Order is being processed (payment verification, inventory check, etc.)
     */
    PROCESSING,
    
    /**
     * Order is on hold (payment issues, inventory issues, etc.)
     */
    ON_HOLD,
    
    /**
     * Order has been shipped to the customer
     */
    SHIPPED,
    
    /**
     * Order has been delivered to the customer
     */
    DELIVERED,
    
    /**
     * Order has been completed (delivered and confirmed)
     */
    COMPLETED,
    
    /**
     * Order has been cancelled before delivery
     */
    CANCELLED,
    
    /**
     * Order has been returned by the customer
     */
    RETURNED,
    
    /**
     * Order has been refunded
     */
    REFUNDED
}