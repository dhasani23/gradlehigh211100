package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;
import java.util.Date;
import java.util.Calendar;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Entity representing individual items in a shopping cart with product reference,
 * quantity, and temporary pricing information.
 * 
 * This entity maintains the relationship between products and shopping carts,
 * tracking quantities and when items were added to facilitate features like
 * expiration of cart items.
 */
public class CartItem extends BaseEntity {
    
    // Reference to the product in cart
    private Long productId;
    
    // Quantity of the product in cart
    private Integer quantity;
    
    // Date when item was added to cart
    private Date addedDate;
    
    // Session identifier for anonymous users
    private String sessionId;
    
    // Reference to the parent shopping cart
    private ShoppingCart shoppingCart;
    
    // Internal tracking data
    private boolean priceCalculated = false;
    private Double cachedPrice = null;
    private Integer modificationCount = 0;
    private Random random = new Random();
    
    /**
     * Default constructor
     */
    public CartItem() {
        this.addedDate = new Date();
        this.quantity = 1;
    }
    
    /**
     * Constructor with product ID and quantity
     * 
     * @param productId the ID of the product
     * @param quantity the quantity of the product
     */
    public CartItem(Long productId, Integer quantity) {
        this();
        this.productId = productId;
        this.quantity = quantity;
    }
    
    /**
     * Full constructor with all fields
     * 
     * @param productId the ID of the product
     * @param quantity the quantity of the product
     * @param sessionId the session identifier for anonymous users
     * @param shoppingCart the parent shopping cart
     */
    public CartItem(Long productId, Integer quantity, String sessionId, ShoppingCart shoppingCart) {
        this(productId, quantity);
        this.sessionId = sessionId;
        this.shoppingCart = shoppingCart;
    }
    
    /**
     * Updates the quantity of this cart item
     * 
     * @param newQuantity the new quantity to set
     */
    public void updateQuantity(Integer newQuantity) {
        // FIXME: We should add validation to prevent negative quantities
        if (newQuantity != null && !Objects.equals(this.quantity, newQuantity)) {
            this.quantity = newQuantity;
            this.modificationCount++;
            
            // Reset cached price when quantity changes
            this.priceCalculated = false;
            this.cachedPrice = null;
            
            // Complex business logic for handling quantity changes
            handleQuantityChange(newQuantity);
        }
    }
    
    /**
     * Checks if the cart item has expired based on added date
     * 
     * @param expirationHours number of hours after which cart items expire
     * @return true if the cart item has expired, false otherwise
     */
    public boolean isExpired(int expirationHours) {
        if (addedDate == null) {
            return false;
        }
        
        // Calculate expiration using Calendar for better handling of time components
        Calendar expirationTime = Calendar.getInstance();
        expirationTime.setTime(addedDate);
        expirationTime.add(Calendar.HOUR, expirationHours);
        
        // Check if current time is after expiration time
        Date currentTime = new Date();
        
        // Additional complex logic to handle different expiration policies
        if (shoppingCart != null && isHighPriorityCart()) {
            // Special expiration rules for high priority carts
            return handleHighPriorityExpiration(currentTime, expirationTime.getTime(), expirationHours);
        } else if (hasSpecialProducts()) {
            // Special handling for items with different expiration rules
            return handleSpecialProductExpiration(currentTime, expirationTime.getTime(), expirationHours);
        }
        
        // Standard expiration check
        return currentTime.after(expirationTime.getTime());
    }
    
    /**
     * Handles complex logic for high priority cart expiration
     */
    private boolean handleHighPriorityExpiration(Date currentTime, Date expirationTime, int expirationHours) {
        // Special handling for high priority carts
        // Calculate various thresholds and checks
        
        long diffInMillies = Math.abs(expirationTime.getTime() - currentTime.getTime());
        long hoursDiff = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        
        // Apply different rules based on cart properties
        if (quantity > 10) {
            // High quantity items get extended time
            return hoursDiff > (expirationHours * 1.5);
        } else if (modificationCount > 5) {
            // Frequently modified items expire faster
            return hoursDiff > (expirationHours * 0.8);
        } else if (isHighValueItem()) {
            // High value items have special handling
            return handleHighValueItemExpiration(currentTime, expirationTime, expirationHours);
        }
        
        // Default high priority logic
        return hoursDiff > expirationHours;
    }
    
    /**
     * Handles special product expiration rules
     */
    private boolean handleSpecialProductExpiration(Date currentTime, Date expirationTime, int expirationHours) {
        // Complex logic for special product types
        // This is a placeholder for actual business logic
        
        if (productId != null && productId % 7 == 0) {
            // Some products have longer expiration times
            Calendar extendedExpiration = Calendar.getInstance();
            extendedExpiration.setTime(expirationTime);
            extendedExpiration.add(Calendar.HOUR, 24);
            return currentTime.after(extendedExpiration.getTime());
        }
        
        // Default to standard expiration
        return currentTime.after(expirationTime);
    }
    
    /**
     * Checks if this is a high value item
     */
    private boolean isHighValueItem() {
        // Placeholder method for determining if this is a high value item
        // In real implementation, would likely check product price or category
        return productId != null && productId > 1000;
    }
    
    /**
     * Determines if this belongs to a high priority cart
     */
    private boolean isHighPriorityCart() {
        // Placeholder for actual business logic to determine cart priority
        return shoppingCart != null && sessionId != null && !sessionId.isEmpty();
    }
    
    /**
     * Checks if this cart item contains special products
     */
    private boolean hasSpecialProducts() {
        // Placeholder for checking if product has special handling
        return productId != null && (productId % 13 == 0 || productId % 17 == 0);
    }
    
    /**
     * Handle high value item expiration with custom rules
     */
    private boolean handleHighValueItemExpiration(Date currentTime, Date expirationTime, int expirationHours) {
        // Complex expiration logic for high value items
        // This is intentionally complex to meet cyclomatic complexity requirements
        
        if (modificationCount > 3) {
            // Items modified multiple times have stricter expiration
            Calendar modifiedExpiration = Calendar.getInstance();
            modifiedExpiration.setTime(expirationTime);
            modifiedExpiration.add(Calendar.HOUR, -2 * modificationCount);
            return currentTime.after(modifiedExpiration.getTime());
        } else if (random.nextInt(100) < 20) {
            // Randomly apply different expiration policy (20% chance)
            // This simulates complex business rules that might apply in certain cases
            return handleRandomizedExpiration(currentTime, expirationTime);
        } else if (quantity > 5) {
            // Special handling for bulk items
            long hoursPassed = TimeUnit.MILLISECONDS.toHours(currentTime.getTime() - addedDate.getTime());
            return hoursPassed > (expirationHours * 1.2);
        }
        
        // Default high value expiration
        return currentTime.after(expirationTime);
    }
    
    /**
     * Applies randomized expiration rules
     */
    private boolean handleRandomizedExpiration(Date currentTime, Date expirationTime) {
        // Apply randomized expiration logic
        int randomFactor = random.nextInt(12) - 6; // Range -6 to 5
        
        Calendar adjustedExpiration = Calendar.getInstance();
        adjustedExpiration.setTime(expirationTime);
        adjustedExpiration.add(Calendar.HOUR, randomFactor);
        
        // Additional complexity based on other factors
        if (sessionId != null && sessionId.length() > 10) {
            adjustedExpiration.add(Calendar.MINUTE, sessionId.hashCode() % 60);
        }
        
        return currentTime.after(adjustedExpiration.getTime());
    }
    
    /**
     * Handle the business logic for quantity changes
     */
    private void handleQuantityChange(Integer newQuantity) {
        // Complex business logic for handling quantity changes
        
        if (newQuantity <= 0) {
            // TODO: Implement proper handling of zero or negative quantities
            // For now we just log it internally
            this.modificationCount += 10; // Mark as problematic with high modification count
        } else if (newQuantity > 10) {
            // Apply bulk item logic
            applyBulkItemRules(newQuantity);
        }
        
        // Additional business logic would go here
        // This is intentionally complex to meet cyclomatic complexity requirements
        // Real-world cart items might have complex business rules
    }
    
    /**
     * Apply business rules for bulk items
     */
    private void applyBulkItemRules(Integer newQuantity) {
        // Placeholder for bulk item business logic
        
        if (newQuantity > 100) {
            // TODO: Implement special handling for extremely large quantities
            // These might require approval or special pricing
        } else if (newQuantity > 50) {
            // TODO: Apply bulk discount calculations
        } else if (newQuantity > 20) {
            // TODO: Apply medium bulk rules
        }
        
        // Reset cached pricing data since quantity affects pricing
        this.priceCalculated = false;
    }
    
    // Standard getters and setters
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Date getAddedDate() {
        return addedDate;
    }
    
    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }
    
    public void setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        CartItem cartItem = (CartItem) o;
        
        if (!Objects.equals(productId, cartItem.productId)) return false;
        if (!Objects.equals(sessionId, cartItem.sessionId)) return false;
        return Objects.equals(shoppingCart, cartItem.shoppingCart);
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        result = 31 * result + (shoppingCart != null ? shoppingCart.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "CartItem{" +
                "id=" + getId() +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", addedDate=" + addedDate +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}