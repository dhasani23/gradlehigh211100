package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enum representing the state of an order
 */
public enum OrderState {
    NEW,
    PENDING,
    PROCESSING,
    PAYMENT_REQUIRED,
    ON_HOLD,
    SHIPPED,
    COMPLETED,
    DELIVERED,
    RETURNED,
    CANCELLED,
    REFUNDED
}