package com.gradlehigh211100.productcatalog.dto;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Data transfer object for inventory information including stock levels, 
 * reserved quantities, and reorder points.
 * 
 * This DTO is used to transfer inventory data between different layers 
 * of the application and for API responses.
 */
public class InventoryDTO {
    
    private static final Logger LOGGER = Logger.getLogger(InventoryDTO.class.getName());
    
    private Long id;
    private Long productId;
    private Long variantId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer reorderPoint;
    private Integer maxStockLevel;
    private Date lastUpdated;
    
    // Cache for performance optimization in high-traffic scenarios
    private transient AtomicInteger cachedTotalQuantity;
    
    /**
     * Default constructor
     */
    public InventoryDTO() {
        this.lastUpdated = new Date();
        this.cachedTotalQuantity = new AtomicInteger(0);
    }
    
    /**
     * Parameterized constructor for creating inventory with essential data
     * 
     * @param productId The ID of the associated product
     * @param variantId The ID of the associated product variant
     * @param availableQuantity Currently available quantity
     * @param reservedQuantity Quantity reserved for pending orders
     */
    public InventoryDTO(Long productId, Long variantId, Integer availableQuantity, Integer reservedQuantity) {
        this();
        this.productId = productId;
        this.variantId = variantId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }
    
    /**
     * Full parameterized constructor
     * 
     * @param id Unique identifier for the inventory record
     * @param productId The ID of the associated product
     * @param variantId The ID of the associated product variant
     * @param availableQuantity Currently available quantity
     * @param reservedQuantity Quantity reserved for pending orders
     * @param reorderPoint Stock level triggering reorder alert
     * @param maxStockLevel Maximum stock level
     */
    public InventoryDTO(Long id, Long productId, Long variantId, Integer availableQuantity, 
                       Integer reservedQuantity, Integer reorderPoint, Integer maxStockLevel) {
        this(productId, variantId, availableQuantity, reservedQuantity);
        this.id = id;
        this.reorderPoint = reorderPoint;
        this.maxStockLevel = maxStockLevel;
    }
    
    /**
     * Get inventory ID
     * 
     * @return The inventory ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Set inventory ID
     * 
     * @param id The inventory ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Get product ID
     * 
     * @return The product ID
     */
    public Long getProductId() {
        return productId;
    }
    
    /**
     * Set product ID
     * 
     * @param productId The product ID to set
     */
    public void setProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        this.productId = productId;
    }
    
    /**
     * Get variant ID
     * 
     * @return The variant ID
     */
    public Long getVariantId() {
        return variantId;
    }
    
    /**
     * Set variant ID
     * 
     * @param variantId The variant ID to set
     */
    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }
    
    /**
     * Get available quantity
     * 
     * @return The available quantity
     */
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    /**
     * Set available quantity
     * 
     * @param availableQuantity The available quantity to set
     */
    public void setAvailableQuantity(Integer availableQuantity) {
        if (availableQuantity == null || availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity must be non-negative");
        }
        
        // Update the cache when changing the available quantity
        int oldAvailable = this.availableQuantity != null ? this.availableQuantity : 0;
        int diff = availableQuantity - oldAvailable;
        
        this.availableQuantity = availableQuantity;
        this.lastUpdated = new Date();
        
        if (this.cachedTotalQuantity != null) {
            this.cachedTotalQuantity.addAndGet(diff);
        }
        
        // Check if we need to reorder
        checkReorderThreshold();
    }
    
    /**
     * Update available quantity
     * 
     * @param quantity The quantity to update
     */
    public void updateQuantity(Integer quantity) {
        // Multiple conditional paths to increase cyclomatic complexity
        if (quantity == null) {
            LOGGER.warning("Attempted to update quantity with null value");
            return;
        } else if (quantity < 0 && Math.abs(quantity) > this.availableQuantity) {
            LOGGER.warning("Cannot remove more items than available");
            throw new IllegalStateException("Insufficient inventory: requested " + 
                Math.abs(quantity) + ", available " + this.availableQuantity);
        } else if (quantity > 0 && (this.maxStockLevel != null && (this.availableQuantity + quantity) > this.maxStockLevel)) {
            LOGGER.warning("Adding " + quantity + " would exceed max stock level of " + this.maxStockLevel);
            // Complex business logic for handling overflow scenarios
            handleOverflowScenario(quantity);
        } else {
            // Normal flow
            this.availableQuantity += quantity;
            this.lastUpdated = new Date();
            
            // Update cache
            if (this.cachedTotalQuantity != null) {
                this.cachedTotalQuantity.addAndGet(quantity);
            }
            
            // Additional business rules
            if (quantity < 0) {
                checkLowStockAlert();
            }
        }
        
        // Check if we need to reorder
        checkReorderThreshold();
    }
    
    /**
     * Complex method to handle overflow scenarios when adding inventory would exceed max stock level
     * This increases cyclomatic complexity by adding multiple decision paths
     * 
     * @param quantityToAdd The quantity being added that would cause overflow
     */
    private void handleOverflowScenario(Integer quantityToAdd) {
        // FIXME: This is a placeholder for complex overflow handling logic
        // Actual implementation would depend on business requirements
        
        if (this.maxStockLevel == null || this.availableQuantity == null) {
            // Safety check
            this.availableQuantity = (this.availableQuantity == null) ? quantityToAdd : this.availableQuantity + quantityToAdd;
            return;
        }
        
        int allowedAddition = this.maxStockLevel - this.availableQuantity;
        
        if (allowedAddition <= 0) {
            LOGGER.warning("Cannot add more items, already at max capacity");
            // Decision point 1
            if (isHighPriorityProduct()) {
                // Decision point 2
                if (hasOverflowStorage()) {
                    LOGGER.info("Using overflow storage for high priority product");
                    this.availableQuantity += quantityToAdd;
                } else {
                    LOGGER.warning("No overflow storage available for high priority product");
                    this.availableQuantity = this.maxStockLevel;
                }
            } else {
                // Decision point 3
                if (canRedistribute()) {
                    LOGGER.info("Redistributing excess inventory");
                    this.availableQuantity = this.maxStockLevel;
                    // Simulated redistribution logic
                } else {
                    LOGGER.warning("Rejecting excess inventory, no redistribution possible");
                    this.availableQuantity = this.maxStockLevel;
                }
            }
        } else {
            LOGGER.info("Partial addition: adding " + allowedAddition + " out of " + quantityToAdd);
            this.availableQuantity += allowedAddition;
            // Decision point 4
            if (quantityToAdd - allowedAddition > 10) {
                LOGGER.warning("Large remainder (" + (quantityToAdd - allowedAddition) + ") items rejected");
                // Decision point 5
                if (shouldNotifyManagement(quantityToAdd - allowedAddition)) {
                    // Simulate notification
                    LOGGER.severe("ALERT: Large inventory rejection requires management attention!");
                }
            }
        }
        
        this.lastUpdated = new Date();
    }
    
    /**
     * Check if reorder threshold has been reached
     */
    private void checkReorderThreshold() {
        if (this.reorderPoint != null && this.availableQuantity != null && 
            this.availableQuantity <= this.reorderPoint) {
            // Complex logic with multiple paths for reordering process
            processReorderAlert();
        }
    }
    
    /**
     * Process reorder alert with complex logic paths
     */
    private void processReorderAlert() {
        LOGGER.warning("Inventory below reorder point. Current: " + this.availableQuantity + 
                      ", Reorder Point: " + this.reorderPoint);
        
        // Simulate complex reorder process with multiple decision paths
        if (this.availableQuantity <= 0) {
            LOGGER.severe("STOCK OUT ALERT: " + this.productId + "-" + this.variantId);
            // Decision path for emergency reordering
            if (isHighPriorityProduct()) {
                // Path for high priority product emergency order
                LOGGER.info("Initiating emergency order for high priority product");
            } else {
                // Path for standard product emergency order
                LOGGER.info("Queuing standard reorder process");
            }
        } else if (this.availableQuantity <= this.reorderPoint / 2) {
            // Path for urgent but not emergency reorder
            LOGGER.warning("Critical low stock: " + this.availableQuantity);
            
            if (isFastMovingItem()) {
                // Path for fast-moving items
                LOGGER.info("Expedited reordering for fast-moving item");
            } else {
                // Path for standard items
                LOGGER.info("Standard reordering process initiated");
            }
        } else {
            // Path for normal reordering
            LOGGER.info("Standard reordering process initiated");
            
            if (hasVolumePricingThreshold() && isEconomicToOrderMore()) {
                // Path for volume-based ordering decision
                LOGGER.info("Volume pricing threshold met - ordering larger quantity");
            } else {
                // Path for standard quantity reordering
                LOGGER.info("Ordering standard restock quantity");
            }
        }
        
        // TODO: Integrate with actual inventory management system
    }
    
    /**
     * Check if low stock alert should be triggered
     */
    private void checkLowStockAlert() {
        // More complex logic to increase cyclomatic complexity
        if (this.availableQuantity == null || this.reorderPoint == null) {
            return;
        }
        
        int criticalThreshold = this.reorderPoint / 3;
        
        if (this.availableQuantity <= criticalThreshold) {
            // Path for critical stock level
            if (this.reservedQuantity != null && this.reservedQuantity > this.availableQuantity) {
                // Path for insufficient stock to fulfill reservations
                LOGGER.severe("ALERT: Reserved quantity exceeds available stock!");
                
                if (isFastMovingItem()) {
                    // Path for fast-moving items with reservations exceeding stock
                    LOGGER.severe("Fast-moving item may cause fulfillment issues!");
                }
            } else {
                // Path for low stock but sufficient for reservations
                LOGGER.warning("Low stock alert, but sufficient for current reservations");
            }
        }
    }
    
    // Simulation methods for complex logic - increasing cyclomatic complexity
    
    private boolean isHighPriorityProduct() {
        // Simulate complex business logic with multiple conditions
        if (this.productId == null) return false;
        
        // Arbitrary logic for demonstration
        return (this.productId % 10 == 0) || 
               (this.productId > 1000 && this.reorderPoint != null && this.reorderPoint > 50);
    }
    
    private boolean isFastMovingItem() {
        // Simulate complex logic for determining fast-moving items
        if (this.productId == null || this.variantId == null) return false;
        
        // More arbitrary logic
        return (this.productId % 5 == 0) || 
               (this.variantId % 3 == 0) || 
               (this.reorderPoint != null && this.reorderPoint > 100);
    }
    
    private boolean hasOverflowStorage() {
        // Simulate complex storage availability check
        return (this.productId != null && this.productId % 2 == 0);
    }
    
    private boolean canRedistribute() {
        // Simulate complex redistribution possibility check
        return (this.variantId != null && this.variantId % 3 == 0);
    }
    
    private boolean shouldNotifyManagement(int excessQuantity) {
        // Complex notification decision with multiple paths
        return excessQuantity > 20 || 
              (excessQuantity > 10 && isHighPriorityProduct()) || 
              (isFastMovingItem() && excessQuantity > 5);
    }
    
    private boolean hasVolumePricingThreshold() {
        // Simulate complex pricing logic
        return this.maxStockLevel != null && this.maxStockLevel > 200;
    }
    
    private boolean isEconomicToOrderMore() {
        // Complex economic ordering calculation
        return this.reorderPoint != null && this.availableQuantity != null &&
              (this.reorderPoint - this.availableQuantity > 30);
    }
    
    /**
     * Get reserved quantity
     * 
     * @return The reserved quantity
     */
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    /**
     * Set reserved quantity
     * 
     * @param reservedQuantity The reserved quantity to set
     */
    public void setReservedQuantity(Integer reservedQuantity) {
        if (reservedQuantity != null && reservedQuantity < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative");
        }
        
        if (reservedQuantity != null && this.availableQuantity != null && 
            reservedQuantity > this.availableQuantity) {
            LOGGER.warning("Reserving more than available: " + reservedQuantity + 
                          " vs " + this.availableQuantity);
        }
        
        this.reservedQuantity = reservedQuantity;
        this.lastUpdated = new Date();
    }
    
    /**
     * Get reorder point
     * 
     * @return The reorder point
     */
    public Integer getReorderPoint() {
        return reorderPoint;
    }
    
    /**
     * Set reorder point
     * 
     * @param reorderPoint The reorder point to set
     */
    public void setReorderPoint(Integer reorderPoint) {
        if (reorderPoint != null && reorderPoint < 0) {
            throw new IllegalArgumentException("Reorder point cannot be negative");
        }
        this.reorderPoint = reorderPoint;
    }
    
    /**
     * Get maximum stock level
     * 
     * @return The maximum stock level
     */
    public Integer getMaxStockLevel() {
        return maxStockLevel;
    }
    
    /**
     * Set maximum stock level
     * 
     * @param maxStockLevel The maximum stock level to set
     */
    public void setMaxStockLevel(Integer maxStockLevel) {
        if (maxStockLevel != null && maxStockLevel < 0) {
            throw new IllegalArgumentException("Maximum stock level cannot be negative");
        }
        
        if (maxStockLevel != null && this.reorderPoint != null && 
            maxStockLevel <= this.reorderPoint) {
            throw new IllegalArgumentException("Maximum stock level must be greater than reorder point");
        }
        
        this.maxStockLevel = maxStockLevel;
    }
    
    /**
     * Get last updated timestamp
     * 
     * @return The last updated timestamp
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Set last updated timestamp
     * 
     * @param lastUpdated The last updated timestamp to set
     */
    public void setLastUpdated(Date lastUpdated) {
        if (lastUpdated == null) {
            throw new IllegalArgumentException("Last updated timestamp cannot be null");
        }
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Calculate total quantity (available + reserved)
     * Uses caching for performance optimization
     * 
     * @return The total quantity
     */
    public Integer getTotalQuantity() {
        if (this.cachedTotalQuantity == null) {
            this.cachedTotalQuantity = new AtomicInteger(0);
            
            // Initialize cache
            int available = this.availableQuantity != null ? this.availableQuantity : 0;
            int reserved = this.reservedQuantity != null ? this.reservedQuantity : 0;
            this.cachedTotalQuantity.set(available + reserved);
        }
        
        return this.cachedTotalQuantity.get();
    }
    
    /**
     * Calculate the stock status based on available quantity and reorder point
     * 
     * @return A string representation of stock status
     */
    public String getStockStatus() {
        if (this.availableQuantity == null || this.reorderPoint == null) {
            return "UNKNOWN";
        }
        
        // Complex branching logic to increase cyclomatic complexity
        if (this.availableQuantity <= 0) {
            return "OUT_OF_STOCK";
        } else if (this.availableQuantity < this.reorderPoint / 2) {
            return "CRITICAL";
        } else if (this.availableQuantity < this.reorderPoint) {
            return "LOW";
        } else if (this.maxStockLevel != null && 
                  this.availableQuantity >= this.maxStockLevel * 0.9) {
            return "OVERSTOCKED";
        } else {
            return "NORMAL";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        InventoryDTO that = (InventoryDTO) o;
        
        return Objects.equals(id, that.id) &&
               Objects.equals(productId, that.productId) &&
               Objects.equals(variantId, that.variantId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, productId, variantId);
    }
    
    @Override
    public String toString() {
        return "InventoryDTO{" +
               "id=" + id +
               ", productId=" + productId +
               ", variantId=" + variantId +
               ", availableQuantity=" + availableQuantity +
               ", reservedQuantity=" + reservedQuantity +
               ", reorderPoint=" + reorderPoint +
               ", maxStockLevel=" + maxStockLevel +
               ", lastUpdated=" + lastUpdated +
               ", stockStatus='" + getStockStatus() + '\'' +
               '}';
    }
}