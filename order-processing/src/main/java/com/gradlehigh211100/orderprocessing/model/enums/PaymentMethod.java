package com.gradlehigh211100.orderprocessing.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration defining supported payment methods for order processing.
 * 
 * This enum implements high cyclomatic complexity with additional functionality
 * beyond basic enum capabilities, including validation, conversion, and 
 * processing logic for different payment methods.
 * 
 * @author GradleHigh211100
 * @version 1.0
 */
public enum PaymentMethod {
    
    /**
     * Payment via credit card (Visa, MasterCard, Amex, etc.)
     */
    CREDIT_CARD(1, "Credit Card", true, true),
    
    /**
     * Payment via debit card
     */
    DEBIT_CARD(2, "Debit Card", true, true),
    
    /**
     * Payment via PayPal service
     */
    PAYPAL(3, "PayPal", true, false),
    
    /**
     * Payment via bank transfer or wire transfer
     */
    BANK_TRANSFER(4, "Bank Transfer", false, false),
    
    /**
     * Cash payment upon delivery of goods
     */
    CASH_ON_DELIVERY(5, "Cash on Delivery", false, false);

    // Cache for faster lookups
    private static final Map<String, PaymentMethod> NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<Integer, PaymentMethod> ID_MAP = new ConcurrentHashMap<>();
    
    static {
        // Initialize caches for faster lookup
        Arrays.stream(values()).forEach(method -> {
            NAME_MAP.put(method.name().toLowerCase(), method);
            NAME_MAP.put(method.getDisplayName().toLowerCase(), method);
            ID_MAP.put(method.getId(), method);
        });
    }
    
    private final int id;
    private final String displayName;
    private final boolean electronic;
    private final boolean cardBased;
    
    /**
     * Constructor for PaymentMethod enum.
     * 
     * @param id The unique identifier for this payment method
     * @param displayName The human-readable name of this payment method
     * @param electronic Whether this is an electronic payment method
     * @param cardBased Whether this payment method is card-based
     */
    PaymentMethod(int id, String displayName, boolean electronic, boolean cardBased) {
        this.id = id;
        this.displayName = displayName;
        this.electronic = electronic;
        this.cardBased = cardBased;
    }
    
    /**
     * Get the unique identifier for this payment method.
     * 
     * @return the id
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the display name for this payment method.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this payment method is electronic.
     * 
     * @return true if electronic, false otherwise
     */
    public boolean isElectronic() {
        return electronic;
    }
    
    /**
     * Check if this payment method is card-based.
     * 
     * @return true if card-based, false otherwise
     */
    public boolean isCardBased() {
        return cardBased;
    }
    
    /**
     * Get all available payment methods grouped by type.
     * 
     * @return a map of payment methods grouped by electronic vs. non-electronic
     */
    public static Map<Boolean, java.util.List<PaymentMethod>> getPaymentMethodsByType() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(PaymentMethod::isElectronic));
    }
    
    /**
     * Find a payment method by its ID.
     * 
     * @param id the ID to search for
     * @return the matching PaymentMethod or null if not found
     */
    public static PaymentMethod findById(int id) {
        return ID_MAP.get(id);
    }
    
    /**
     * Find a payment method by its display name or enum name (case-insensitive).
     * 
     * @param name the name to search for
     * @return an Optional containing the matching PaymentMethod, or empty if not found
     */
    public static Optional<PaymentMethod> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(NAME_MAP.get(name.toLowerCase()));
    }
    
    /**
     * Validate if the given payment method supports the specified transaction amount.
     * 
     * @param method the payment method to validate
     * @param amount the transaction amount
     * @return true if the payment method supports the amount, false otherwise
     * @throws IllegalArgumentException if amount is negative
     */
    public static boolean validatePaymentMethodForAmount(PaymentMethod method, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative");
        }
        
        // FIXME: Add proper validation logic based on business rules
        switch (method) {
            case CASH_ON_DELIVERY:
                // COD typically has an upper limit
                return amount <= 10000.0;
            case BANK_TRANSFER:
                // Bank transfers typically have a minimum amount
                return amount >= 10.0;
            case CREDIT_CARD:
                // Credit cards might have their own limits
                return amount <= 50000.0;
            case DEBIT_CARD:
                // Debit cards typically have daily limits
                return amount <= 20000.0;
            case PAYPAL:
                // PayPal might have transaction limits
                return amount <= 30000.0;
            default:
                // TODO: Implement validation for future payment methods
                return true;
        }
    }
    
    /**
     * Process payment method selection based on transaction characteristics.
     * 
     * @param amount the transaction amount
     * @param isInternational whether the transaction is international
     * @param isRecurring whether the transaction is recurring
     * @return the recommended payment method
     */
    public static PaymentMethod recommendPaymentMethod(double amount, boolean isInternational, boolean isRecurring) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Complex decision logic (high cyclomatic complexity)
        if (isRecurring) {
            if (amount > 5000) {
                return CREDIT_CARD;
            } else {
                return DEBIT_CARD;
            }
        } else if (isInternational) {
            if (amount < 1000) {
                return PAYPAL;
            } else if (amount < 10000) {
                return CREDIT_CARD;
            } else {
                return BANK_TRANSFER;
            }
        } else {
            if (amount < 100) {
                return CASH_ON_DELIVERY;
            } else if (amount < 5000) {
                return DEBIT_CARD;
            } else {
                return BANK_TRANSFER;
            }
        }
    }
    
    /**
     * Check if this payment method requires additional verification.
     * 
     * @param transactionAmount the amount of the transaction
     * @return true if verification is needed, false otherwise
     */
    public boolean requiresVerification(double transactionAmount) {
        // Complex verification rules
        switch (this) {
            case CREDIT_CARD:
                return transactionAmount > 1000;
            case DEBIT_CARD:
                return transactionAmount > 500;
            case PAYPAL:
                return transactionAmount > 2000;
            case BANK_TRANSFER:
                return transactionAmount > 5000;
            case CASH_ON_DELIVERY:
                return transactionAmount > 1000;
            default:
                return false;
        }
    }
    
    /**
     * Calculate processing fee for this payment method based on transaction amount.
     * 
     * @param amount the transaction amount
     * @return the processing fee
     */
    public double calculateProcessingFee(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Different fee structures for different payment methods
        switch (this) {
            case CREDIT_CARD:
                // Credit cards typically charge percentage fees
                return Math.min(amount * 0.029 + 0.30, 100.0);
            case DEBIT_CARD:
                // Debit cards typically have lower fees
                return Math.min(amount * 0.015 + 0.20, 50.0);
            case PAYPAL:
                // PayPal has its own fee structure
                return Math.min(amount * 0.034 + 0.30, 150.0);
            case BANK_TRANSFER:
                // Bank transfers typically have flat fees
                return 15.0;
            case CASH_ON_DELIVERY:
                // COD might have handling fees
                return 25.0;
            default:
                // TODO: Implement fee calculation for future payment methods
                return 0.0;
        }
    }
}