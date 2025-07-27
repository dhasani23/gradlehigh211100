package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enumeration representing the events that can trigger state transitions in the order processing workflow.
 */
public enum OrderEvent {
    
    VALIDATE("Validate"),
    PROCESS_PAYMENT("Process Payment"),
    PAYMENT_APPROVED("Payment Approved"),
    PAYMENT_DECLINED("Payment Declined"),
    FULFILL("Fulfill"),
    BACKORDER("Backorder"),
    RESTOCK("Restock"),
    SHIPPED("Shipped"),
    DELIVER("Deliver"),
    COMPLETE("Complete"),
    CANCEL("Cancel"),
    RETURN_INITIATED("Return Initiated"),
    RETURN_APPROVED("Return Approved"),
    RETURN_REJECTED("Return Rejected"),
    REFUND_PROCESSED("Refund Processed");
    
    private final String displayName;
    
    OrderEvent(String displayName) {
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