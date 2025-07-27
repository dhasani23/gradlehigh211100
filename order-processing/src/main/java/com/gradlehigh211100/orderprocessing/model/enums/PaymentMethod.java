package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Represents the various methods of payment available for orders.
 */
public enum PaymentMethod {
    /**
     * Payment via credit card
     */
    CREDIT_CARD,
    
    /**
     * Payment via PayPal
     */
    PAYPAL,
    
    /**
     * Payment via direct bank transfer
     */
    BANK_TRANSFER,
    
    /**
     * Payment on delivery
     */
    COD,
    
    /**
     * Payment via gift card
     */
    GIFT_CARD,
    
    /**
     * Payment via store credit
     */
    STORE_CREDIT
}