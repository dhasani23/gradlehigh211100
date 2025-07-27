package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Entity representing individual items within an order including product reference, 
 * quantity, unit price, discounts, and total amount calculations.
 * 
 * This class handles all the operations related to order items including:
 * - Price calculations with various business rules
 * - Discount application logic
 * - Inventory impact calculations
 * - Quantity validation and updates
 */
public class OrderItem extends BaseEntity {

    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private Order order;
    
    // Cache for complex calculations
    private boolean recalculationNeeded = true;
    private static final int PRECISION = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Default constructor
     */
    public OrderItem() {
        this.quantity = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalPrice = BigDecimal.ZERO;
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param productId the product ID
     * @param productName the product name
     * @param productSku the product SKU
     * @param quantity the quantity ordered
     * @param unitPrice the unit price
     */
    public OrderItem(Long productId, String productName, String productSku, 
                    Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountAmount = BigDecimal.ZERO;
        calculateTotalPrice();
    }

    /**
     * Calculates and returns the total price for this order item.
     * The calculation considers quantity, unit price, and any applicable discounts.
     * 
     * Formula: totalPrice = (quantity * unitPrice) - discountAmount
     * 
     * @return the calculated total price
     */
    public BigDecimal calculateTotalPrice() {
        // Handle null values to prevent NPE
        if (quantity == null) {
            quantity = 0;
        }
        
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }
        
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        
        // Perform basic calculation
        BigDecimal grossTotal = unitPrice.multiply(new BigDecimal(quantity));
        
        // Apply business rules for discounts based on quantity tiers
        BigDecimal effectiveDiscount = calculateEffectiveDiscount(grossTotal);
        
        // Apply any special product-specific discount rules
        if (hasSpecialDiscountRules()) {
            effectiveDiscount = applySpecialDiscountRules(effectiveDiscount, grossTotal);
        }
        
        // FIXME: Discount sometimes exceeds the total price causing negative totals
        if (effectiveDiscount.compareTo(grossTotal) > 0) {
            effectiveDiscount = grossTotal;
        }
        
        // Calculate final price with discounts
        this.totalPrice = grossTotal.subtract(effectiveDiscount)
                .setScale(PRECISION, ROUNDING_MODE);
        
        this.discountAmount = effectiveDiscount;
        this.recalculationNeeded = false;
        
        return this.totalPrice;
    }
    
    /**
     * Complex logic to determine the effective discount based on various business rules
     * 
     * @param grossTotal the gross total before discounts
     * @return the calculated effective discount
     */
    private BigDecimal calculateEffectiveDiscount(BigDecimal grossTotal) {
        // Basic discount is what's already applied
        BigDecimal baseDiscount = this.discountAmount;
        
        // Apply quantity tier discounts
        if (quantity >= 100) {
            // 15% discount for bulk orders over 100 items
            BigDecimal bulkDiscount = grossTotal.multiply(new BigDecimal("0.15"));
            baseDiscount = baseDiscount.add(bulkDiscount);
        } else if (quantity >= 50) {
            // 10% discount for semi-bulk orders between 50-99 items
            BigDecimal semiBulkDiscount = grossTotal.multiply(new BigDecimal("0.10"));
            baseDiscount = baseDiscount.add(semiBulkDiscount);
        } else if (quantity >= 10) {
            // 5% discount for mini-bulk orders between 10-49 items
            BigDecimal miniBulkDiscount = grossTotal.multiply(new BigDecimal("0.05"));
            baseDiscount = baseDiscount.add(miniBulkDiscount);
        }
        
        // TODO: Implement seasonal discount rules based on current date
        
        return baseDiscount;
    }
    
    /**
     * Determines if this product has special discount rules
     * 
     * @return true if special discount rules apply
     */
    private boolean hasSpecialDiscountRules() {
        // Check if this SKU is in the list of products with special discounts
        if (productSku == null) {
            return false;
        }
        
        // TODO: Replace with actual business logic from discount service
        return productSku.startsWith("SPECIAL") || 
               productSku.endsWith("DISC") ||
               productSku.contains("PROMO");
    }
    
    /**
     * Applies special product-specific discount rules
     * 
     * @param currentDiscount the current discount amount
     * @param grossTotal the gross total before discounts
     * @return the adjusted discount amount
     */
    private BigDecimal applySpecialDiscountRules(BigDecimal currentDiscount, BigDecimal grossTotal) {
        // Complex logic for applying special discount rules based on product type
        if (productSku == null) {
            return currentDiscount;
        }
        
        BigDecimal adjustedDiscount = currentDiscount;
        
        // Different discount rules based on product SKU patterns
        if (productSku.startsWith("SPECIAL")) {
            // Special products get additional 5% off
            BigDecimal additionalDiscount = grossTotal.multiply(new BigDecimal("0.05"));
            adjustedDiscount = adjustedDiscount.add(additionalDiscount);
        } else if (productSku.endsWith("DISC")) {
            // Discount products have fixed discount amount
            adjustedDiscount = adjustedDiscount.add(new BigDecimal("5.00"));
        } else if (productSku.contains("PROMO")) {
            // Promo products get percentage off based on quantity thresholds
            if (quantity >= 3) {
                // Buy 3 or more, get 20% off
                BigDecimal promoDiscount = grossTotal.multiply(new BigDecimal("0.20"));
                adjustedDiscount = adjustedDiscount.add(promoDiscount);
            }
        }
        
        return adjustedDiscount;
    }

    /**
     * Applies discount to this order item.
     * After applying the discount, total price is recalculated.
     * 
     * @param discount the discount amount to apply
     */
    public void applyDiscount(BigDecimal discount) {
        // Input validation
        if (discount == null) {
            throw new IllegalArgumentException("Discount cannot be null");
        }
        
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        
        // Multiple discount application strategies
        // Strategy 1: Replace existing discount
        this.discountAmount = discount;
        
        // Strategy 2: Add to existing discount
        // this.discountAmount = this.discountAmount.add(discount);
        
        // Strategy 3: Take max of current discount and new discount
        // this.discountAmount = this.discountAmount.max(discount);
        
        // FIXME: Sometimes applying multiple discounts causes incorrect totals
        
        // Mark for recalculation
        this.recalculationNeeded = true;
        calculateTotalPrice();
    }

    /**
     * Updates the quantity and recalculates total price.
     * Contains validation logic for quantity limits.
     * 
     * @param newQuantity the new quantity to set
     * @throws IllegalArgumentException if quantity is invalid
     */
    public void updateQuantity(Integer newQuantity) {
        // Input validation with specific business rules
        if (newQuantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        
        // Maximum quantity check (business rule)
        if (newQuantity > 1000) {
            throw new IllegalArgumentException("Maximum quantity allowed is 1000");
        }
        
        // Store old quantity for event logging
        Integer oldQuantity = this.quantity;
        this.quantity = newQuantity;
        
        // Business logic for quantity changes
        if (newQuantity > oldQuantity) {
            // Quantity increased - check if we should apply volume discount
            applyVolumeDiscountIfApplicable();
        }
        
        // Mark for recalculation
        this.recalculationNeeded = true;
        
        // Special business rule: if quantity reaches certain threshold, apply special pricing
        if (newQuantity >= 50 && (oldQuantity == null || oldQuantity < 50)) {
            applyBulkPricing();
        }
        
        calculateTotalPrice();
        
        // TODO: Notify inventory system about quantity change
    }
    
    /**
     * Applies volume discount based on quantity thresholds
     */
    private void applyVolumeDiscountIfApplicable() {
        // Apply automatic volume discount for large quantities
        if (quantity >= 100) {
            // Additional discount logic for very large orders
            BigDecimal volumeDiscount = unitPrice.multiply(new BigDecimal(quantity))
                    .multiply(new BigDecimal("0.05"));
            this.discountAmount = this.discountAmount.add(volumeDiscount);
        }
    }
    
    /**
     * Applies special bulk pricing when quantity threshold is reached
     */
    private void applyBulkPricing() {
        // Special pricing for bulk orders
        BigDecimal bulkDiscount = unitPrice.multiply(new BigDecimal("0.08"))
                .multiply(new BigDecimal(quantity));
        this.discountAmount = this.discountAmount.add(bulkDiscount);
        
        // TODO: Log application of bulk pricing discount
    }
    
    /**
     * Checks if item has minimum required fields
     * @return true if item is valid
     */
    public boolean isValid() {
        return productId != null && 
               productName != null && !productName.isEmpty() &&
               productSku != null && !productSku.isEmpty() &&
               quantity != null && quantity > 0 &&
               unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    // Getters and Setters
    
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    // Setter directly calls updateQuantity to ensure business logic is applied
    public void setQuantity(Integer quantity) {
        updateQuantity(quantity);
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
        
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        
        this.unitPrice = unitPrice;
        this.recalculationNeeded = true;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        } else if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount amount cannot be negative");
        } else {
            this.discountAmount = discountAmount;
        }
        this.recalculationNeeded = true;
    }

    public BigDecimal getTotalPrice() {
        if (recalculationNeeded) {
            calculateTotalPrice();
        }
        return totalPrice;
    }

    protected void setTotalPrice(BigDecimal totalPrice) {
        // Protected to prevent direct manipulation
        this.totalPrice = totalPrice;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        OrderItem orderItem = (OrderItem) o;
        
        return Objects.equals(getId(), orderItem.getId()) &&
               Objects.equals(productId, orderItem.productId) &&
               Objects.equals(productSku, orderItem.productSku) &&
               Objects.equals(order, orderItem.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), productId, productSku, order);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + getId() +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", productSku='" + productSku + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", discountAmount=" + discountAmount +
                ", totalPrice=" + totalPrice +
                '}';
    }
}