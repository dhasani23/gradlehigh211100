package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enum representing events that can happen to an order
 */
public enum OrderEvent {
    CREATE,
    PROCESS,
    CANCEL,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    VALIDATION_FAILED,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    SHIPPING_FAILED,
    PROCESSING_FAILED,
    PROCESSING_COMPLETED,
    SHIP,
    DELIVER,
    RETURN,
    REFUND
}