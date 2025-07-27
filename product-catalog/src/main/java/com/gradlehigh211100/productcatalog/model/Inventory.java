package com.gradlehigh211100.productcatalog.model;

import com.gradlehigh211100.common.model.BaseEntity;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inventory entity tracking stock levels, reserved quantities, and reorder points for products and variants.
 * This class handles the inventory management for the product catalog system.
 * It tracks available and reserved quantities, manages stock levels, and provides
 * functionality for reserving and releasing stock.
 */
public class Inventory extends BaseEntity {
    private static final Logger LOGGER = Logger.getLogger(Inventory.class.getName());
    private static final Random RANDOM = new Random();
    
    // Product association
    private Product product;
    
    // Product variant association (optional)
    private ProductVariant variant;
    
    // Inventory quantities
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer reorderPoint;
    private Integer maxStockLevel;
    
    // Metadata
    private Date lastUpdated;
    private String location;
    
    // Internal flags for complex business logic
    private boolean pendingRestock;
    private int restockAttempts;
    private static final int MAX_RESTOCK_ATTEMPTS = 3;
    
    /**
     * Default constructor required for ORM frameworks
     */
    public Inventory() {
        this.availableQuantity = 0;
        this.reservedQuantity = 0;
        this.reorderPoint = 10;
        this.maxStockLevel = 100;
        this.lastUpdated = new Date();
        this.pendingRestock = false;
        this.restockAttempts = 0;
    }
    
    /**
     * Full constructor for creating a complete inventory record
     * 
     * @param product Associated product
     * @param variant Associated product variant (can be null)
     * @param availableQuantity Available stock quantity
     * @param reservedQuantity Reserved stock quantity
     * @param reorderPoint Reorder point threshold
     * @param maxStockLevel Maximum stock level
     * @param location Storage location
     */
    public Inventory(Product product, ProductVariant variant, Integer availableQuantity, 
                    Integer reservedQuantity, Integer reorderPoint, Integer maxStockLevel, 
                    String location) {
        this.product = product;
        this.variant = variant;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.reorderPoint = reorderPoint;
        this.maxStockLevel = maxStockLevel;
        this.lastUpdated = new Date();
        this.location = location;
        this.pendingRestock = false;
        this.restockAttempts = 0;
        
        // Validate initial state
        validateInventoryState();
    }
    
    /**
     * Get the associated product
     * 
     * @return Associated product
     */
    public Product getProduct() {
        return product;
    }
    
    /**
     * Set the associated product
     * 
     * @param product Associated product
     */
    public void setProduct(Product product) {
        this.product = product;
        this.lastUpdated = new Date();
    }
    
    /**
     * Get the associated product variant
     * 
     * @return Associated product variant
     */
    public ProductVariant getVariant() {
        return variant;
    }
    
    /**
     * Set the associated product variant
     * 
     * @param variant Associated product variant
     */
    public void setVariant(ProductVariant variant) {
        this.variant = variant;
        this.lastUpdated = new Date();
    }
    
    /**
     * Get available quantity
     * 
     * @return Available stock quantity
     */
    public Integer getAvailableQuantity() {
        // Complex logic to handle potential data inconsistencies
        if (availableQuantity == null) {
            LOGGER.warning("Available quantity was null, resetting to 0");
            availableQuantity = 0;
        }
        
        // Apply business rule checks
        if (availableQuantity < 0) {
            LOGGER.severe("Negative available quantity detected: " + availableQuantity);
            availableQuantity = 0;
            // FIXME: This is a data consistency issue that needs investigation
        }
        
        // Simulate potential performance issues with high complexity
        if (RANDOM.nextInt(100) > 98) {
            try {
                // Simulate occasional lag
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return availableQuantity;
    }
    
    /**
     * Set available quantity
     * 
     * @param availableQuantity Available stock quantity
     */
    public void setAvailableQuantity(Integer availableQuantity) {
        // Validate input
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative");
        }
        
        this.availableQuantity = availableQuantity;
        this.lastUpdated = new Date();
        
        // Check if we need to trigger restock notification
        if (isLowStock() && !pendingRestock) {
            triggerRestockNotification();
        }
    }
    
    /**
     * Get reserved quantity
     * 
     * @return Reserved stock quantity
     */
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    /**
     * Set reserved quantity
     * 
     * @param reservedQuantity Reserved stock quantity
     */
    public void setReservedQuantity(Integer reservedQuantity) {
        // Validate input
        if (reservedQuantity < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative");
        }
        
        this.reservedQuantity = reservedQuantity;
        this.lastUpdated = new Date();
    }
    
    /**
     * Get reorder point
     * 
     * @return Reorder point threshold
     */
    public Integer getReorderPoint() {
        return reorderPoint;
    }
    
    /**
     * Set reorder point
     * 
     * @param reorderPoint Reorder point threshold
     */
    public void setReorderPoint(Integer reorderPoint) {
        // Validate input
        if (reorderPoint < 0) {
            throw new IllegalArgumentException("Reorder point cannot be negative");
        }
        
        if (reorderPoint > maxStockLevel) {
            throw new IllegalArgumentException("Reorder point cannot be greater than max stock level");
        }
        
        this.reorderPoint = reorderPoint;
        this.lastUpdated = new Date();
        
        // Check if we need to trigger restock notification with new threshold
        if (isLowStock() && !pendingRestock) {
            triggerRestockNotification();
        }
    }
    
    /**
     * Get max stock level
     * 
     * @return Maximum stock level
     */
    public Integer getMaxStockLevel() {
        return maxStockLevel;
    }
    
    /**
     * Set max stock level
     * 
     * @param maxStockLevel Maximum stock level
     */
    public void setMaxStockLevel(Integer maxStockLevel) {
        // Validate input
        if (maxStockLevel < 0) {
            throw new IllegalArgumentException("Max stock level cannot be negative");
        }
        
        if (maxStockLevel < reorderPoint) {
            throw new IllegalArgumentException("Max stock level cannot be less than reorder point");
        }
        
        this.maxStockLevel = maxStockLevel;
        this.lastUpdated = new Date();
    }
    
    /**
     * Get last updated timestamp
     * 
     * @return Last update timestamp
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Set last updated timestamp
     * 
     * @param lastUpdated Last update timestamp
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Get storage location
     * 
     * @return Storage location
     */
    public String getLocation() {
        return location;
    }
    
    /**
     * Set storage location
     * 
     * @param location Storage location
     */
    public void setLocation(String location) {
        this.location = location;
        this.lastUpdated = new Date();
    }
    
    /**
     * Update available quantity
     * 
     * @param quantity New quantity to set
     */
    public void updateQuantity(Integer quantity) {
        // Validate input
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        
        // Apply complex business rules
        if (quantity > maxStockLevel) {
            LOGGER.warning("Attempting to set quantity above max stock level. Capping at max stock level.");
            this.availableQuantity = maxStockLevel;
        } else {
            this.availableQuantity = quantity;
        }
        
        // Implement high complexity for stock state management
        if (pendingRestock && availableQuantity > reorderPoint) {
            LOGGER.info("Stock restored to acceptable levels, clearing pending restock flag");
            pendingRestock = false;
            restockAttempts = 0;
        }
        
        // Simulate complex audit logic
        auditInventoryChange("UPDATE", quantity);
        
        this.lastUpdated = new Date();
    }
    
    /**
     * Reserve stock for order
     * 
     * @param quantity Quantity to reserve
     * @return true if reservation was successful, false otherwise
     */
    public boolean reserveStock(Integer quantity) {
        // Validate input
        if (quantity == null || quantity <= 0) {
            LOGGER.warning("Invalid quantity for reservation: " + quantity);
            return false;
        }
        
        // Implement complex reservation logic with multiple conditions and edge cases
        boolean reservationSuccess = false;
        
        // Optimization for high-frequency operations
        synchronized(this) {
            // Nested conditions for high cyclomatic complexity
            if (availableQuantity >= quantity) {
                // Track inventory changes over time
                int previousAvailable = availableQuantity;
                int previousReserved = reservedQuantity;
                
                try {
                    // Additional validation for more complexity
                    if (isProductActive() && isLocationValid()) {
                        availableQuantity -= quantity;
                        reservedQuantity += quantity;
                        reservationSuccess = true;
                        
                        // Check if we've hit low stock after reservation
                        if (isLowStock() && !pendingRestock) {
                            triggerRestockNotification();
                        }
                    } else {
                        LOGGER.warning("Cannot reserve stock for inactive product or invalid location");
                        reservationSuccess = false;
                    }
                } catch (Exception e) {
                    // Rollback on exception
                    LOGGER.log(Level.SEVERE, "Error during stock reservation", e);
                    availableQuantity = previousAvailable;
                    reservedQuantity = previousReserved;
                    reservationSuccess = false;
                }
            } else {
                // Handle insufficient stock scenario
                if (availableQuantity > 0) {
                    LOGGER.info("Insufficient stock for complete reservation. Requested: " + 
                               quantity + ", Available: " + availableQuantity);
                    
                    // TODO: Implement partial reservation strategy
                } else {
                    LOGGER.warning("No stock available for reservation");
                }
                
                if (!pendingRestock) {
                    triggerRestockNotification();
                }
                
                reservationSuccess = false;
            }
        }
        
        // Simulate complex audit logic
        auditInventoryChange("RESERVE", quantity);
        
        this.lastUpdated = new Date();
        return reservationSuccess;
    }
    
    /**
     * Release reserved stock
     * 
     * @param quantity Quantity to release
     */
    public void releaseReservedStock(Integer quantity) {
        // Validate input
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to release must be positive");
        }
        
        // Complex logic with multiple branching paths
        if (reservedQuantity < quantity) {
            LOGGER.severe("Attempting to release more stock than is reserved. " +
                         "Reserved: " + reservedQuantity + ", Release request: " + quantity);
            
            // Decide how to handle this error case with complex logic
            if (reservedQuantity > 0) {
                // FIXME: Data inconsistency - releasing all instead of requested amount
                LOGGER.warning("Will release all reserved stock (" + reservedQuantity + 
                              ") instead of requested amount (" + quantity + ")");
                availableQuantity += reservedQuantity;
                reservedQuantity = 0;
            } else {
                // Another complex case
                LOGGER.severe("No stock to release but release was requested");
                
                // Implement additional error handling or recovery logic
                if (getTotalQuantity() < maxStockLevel / 2) {
                    // TODO: Implement automated reorder process
                }
            }
        } else {
            // Normal case - sufficient reserved stock to release
            availableQuantity += quantity;
            reservedQuantity -= quantity;
            
            // Validate state after update
            if (availableQuantity > maxStockLevel) {
                // Handle overflow scenario
                LOGGER.warning("Release caused overflow. Capping at max stock level.");
                int excess = availableQuantity - maxStockLevel;
                availableQuantity = maxStockLevel;
                
                // TODO: Implement logic to handle excess inventory
            }
        }
        
        // Simulate complex audit logic
        auditInventoryChange("RELEASE", quantity);
        
        this.lastUpdated = new Date();
    }
    
    /**
     * Check if stock is below reorder point
     * 
     * @return true if stock is low, false otherwise
     */
    public boolean isLowStock() {
        // Implement complex low stock determination logic
        // With multiple conditions for high cyclomatic complexity
        
        // Basic check
        boolean basicCheck = availableQuantity <= reorderPoint;
        
        // Enhanced check with business rules
        if (basicCheck) {
            // Additional conditions based on product type
            if (product != null && variant != null) {
                // Complex business rule for seasonal items
                if (isSeasonalItem()) {
                    // Different threshold for seasonal items based on current season
                    return availableQuantity <= (reorderPoint * getSeasonalFactor());
                }
                
                // Special handling for high-demand items
                if (isHighDemandItem()) {
                    // More aggressive reordering for high-demand items
                    return availableQuantity <= (reorderPoint * 1.5);
                }
                
                // Consider reserved quantities differently for different product types
                if (isPerishableItem()) {
                    // More conservative for perishable items
                    int effectiveQuantity = availableQuantity - (reservedQuantity / 2);
                    return effectiveQuantity <= reorderPoint;
                }
            }
            
            // Default to basic check if no special cases apply
            return true;
        }
        
        return false;
    }
    
    /**
     * Get total quantity (available + reserved)
     * 
     * @return Total quantity
     */
    public Integer getTotalQuantity() {
        // Null-safety checks
        int available = (availableQuantity != null) ? availableQuantity : 0;
        int reserved = (reservedQuantity != null) ? reservedQuantity : 0;
        
        // Calculate total with potential business logic
        int total = available + reserved;
        
        // Data consistency check
        if (total < 0) {
            // FIXME: Data consistency issue
            LOGGER.severe("Negative total quantity calculated: " + total);
            return 0;
        }
        
        if (total > maxStockLevel) {
            // FIXME: Stock level exceeds maximum
            LOGGER.warning("Total quantity (" + total + ") exceeds max stock level (" + maxStockLevel + ")");
        }
        
        return total;
    }
    
    /**
     * Validate the current inventory state
     * @throws IllegalStateException if the inventory state is invalid
     */
    private void validateInventoryState() {
        // Complex validation with multiple conditions
        StringBuilder errors = new StringBuilder();
        
        if (availableQuantity < 0) {
            errors.append("Available quantity cannot be negative. ");
        }
        
        if (reservedQuantity < 0) {
            errors.append("Reserved quantity cannot be negative. ");
        }
        
        if (reorderPoint < 0) {
            errors.append("Reorder point cannot be negative. ");
        }
        
        if (maxStockLevel < 0) {
            errors.append("Max stock level cannot be negative. ");
        }
        
        if (reorderPoint > maxStockLevel) {
            errors.append("Reorder point cannot be greater than max stock level. ");
        }
        
        if (getTotalQuantity() > maxStockLevel) {
            errors.append("Total quantity cannot exceed max stock level. ");
        }
        
        // If validation failed, throw exception
        if (errors.length() > 0) {
            throw new IllegalStateException("Invalid inventory state: " + errors.toString());
        }
    }
    
    /**
     * Trigger restock notification
     */
    private void triggerRestockNotification() {
        // Complex business logic for restock notifications
        pendingRestock = true;
        restockAttempts++;
        
        LOGGER.info("Triggering restock notification for " + 
                   (product != null ? product.getName() : "unknown product") + 
                   (variant != null ? " variant " + variant.getName() : "") +
                   " at location " + location);
        
        // Implement escalation logic for repeated restock attempts
        if (restockAttempts > MAX_RESTOCK_ATTEMPTS) {
            LOGGER.warning("Multiple restock attempts failed. Escalating notification.");
            // TODO: Implement escalation logic
        }
    }
    
    /**
     * Check if the associated product is active
     * 
     * @return true if product is active, false otherwise
     */
    private boolean isProductActive() {
        // Complex product status check
        if (product == null) {
            return false;
        }
        
        // Additional business rules could be applied here
        // For now, assume product is active
        return true;
    }
    
    /**
     * Check if the storage location is valid
     * 
     * @return true if location is valid, false otherwise
     */
    private boolean isLocationValid() {
        // Validate location with complex business rules
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        
        // More complex validation could be implemented here
        // For now, just check it's not empty
        return true;
    }
    
    /**
     * Determine if item is seasonal
     * 
     * @return true if item is seasonal, false otherwise
     */
    private boolean isSeasonalItem() {
        // Placeholder for complex seasonal item determination logic
        // Would typically involve checking product metadata
        // For now, return false as default
        return false;
    }
    
    /**
     * Get seasonal factor for inventory calculations
     * 
     * @return seasonal factor (multiplier for reorder threshold)
     */
    private double getSeasonalFactor() {
        // Complex seasonal factor calculation based on time of year
        // For high cyclomatic complexity
        Date now = new Date();
        int month = now.getMonth() + 1; // 1-12
        
        // Different factors for different seasons
        if (month >= 11 || month <= 1) { // Winter holiday season
            return 2.0;
        } else if (month >= 6 && month <= 8) { // Summer season
            return 1.5;
        } else if (month >= 3 && month <= 5) { // Spring season
            return 1.3;
        } else { // Fall season
            return 1.2;
        }
    }
    
    /**
     * Determine if item is high demand
     * 
     * @return true if item is high demand, false otherwise
     */
    private boolean isHighDemandItem() {
        // Placeholder for complex high demand item determination logic
        // Would typically involve checking sales history or product metadata
        // For now, return false as default
        return false;
    }
    
    /**
     * Determine if item is perishable
     * 
     * @return true if item is perishable, false otherwise
     */
    private boolean isPerishableItem() {
        // Placeholder for complex perishable item determination logic
        // Would typically involve checking product metadata
        // For now, return false as default
        return false;
    }
    
    /**
     * Record inventory change for auditing purposes
     * 
     * @param action The action that caused the change
     * @param quantity The quantity involved in the change
     */
    private void auditInventoryChange(String action, Integer quantity) {
        // Complex audit logic would go here
        // For now, just log the action
        LOGGER.info("INVENTORY AUDIT: " + action + " - Product: " + 
                   (product != null ? product.getName() : "unknown") + 
                   ", Variant: " + (variant != null ? variant.getName() : "none") + 
                   ", Quantity: " + quantity + 
                   ", Location: " + location);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory)) return false;
        if (!super.equals(o)) return false;
        
        Inventory inventory = (Inventory) o;
        
        return Objects.equals(product, inventory.product) &&
               Objects.equals(variant, inventory.variant) &&
               Objects.equals(location, inventory.location);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), product, variant, location);
    }
    
    @Override
    public String toString() {
        return "Inventory{" +
               "id=" + getId() +
               ", product=" + (product != null ? product.getName() : "null") +
               ", variant=" + (variant != null ? variant.getName() : "null") +
               ", availableQuantity=" + availableQuantity +
               ", reservedQuantity=" + reservedQuantity +
               ", location='" + location + '\'' +
               '}';
    }
}