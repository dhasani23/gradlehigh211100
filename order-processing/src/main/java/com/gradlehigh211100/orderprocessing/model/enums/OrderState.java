package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enumeration representing the possible states of an order in the order processing workflow.
 */
public enum OrderState {
    
    CREATED("Created"),
    VALIDATED("Validated"),
    PAYMENT_PROCESSING("Payment Processing"),
    PAYMENT_APPROVED("Payment Approved"),
    PAYMENT_DECLINED("Payment Declined"),
    FULFILLING("Fulfilling"),
    BACKORDERED("Backordered"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    RETURN_INITIATED("Return Initiated"),
    RETURN_APPROVED("Return Approved"),
    RETURN_REJECTED("Return Rejected"),
    REFUNDED("Refunded");
    
    private final String displayName;
    
    OrderState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}