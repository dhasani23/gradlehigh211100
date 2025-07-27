package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enumeration of available payment methods in the system.
 */
public enum PaymentMethod {
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    PAYPAL("PayPal"),
    BANK_TRANSFER("Bank Transfer"),
    CRYPTOCURRENCY("Cryptocurrency"),
    GIFT_CARD("Gift Card"),
    CASH_ON_DELIVERY("Cash on Delivery");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Find payment method by its display name
     * 
     * @param name The display name to search for
     * @return The matching PaymentMethod or null if not found
     */
    public static PaymentMethod fromDisplayName(String name) {
        for (PaymentMethod method : values()) {
            if (method.displayName.equalsIgnoreCase(name)) {
                return method;
            }
        }
        return null;
    }
}