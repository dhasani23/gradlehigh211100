package com.gradlehigh211100.productcatalog.service;

import com.gradlehigh211100.productcatalog.repository.InventoryRepository;
import com.gradlehigh211100.productcatalog.model.Inventory;
import com.gradlehigh211100.productcatalog.dto.InventoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inventory service providing business logic for inventory management, 
 * stock level tracking, and low-stock alerts.
 * 
 * This service handles all inventory-related operations including:
 * - Stock updates and tracking
 * - Stock reservation for orders
 * - Low stock monitoring and alerts
 * - Bulk inventory operations
 */
@Service
public class InventoryService {
    
    private static final Logger LOGGER = Logger.getLogger(InventoryService.class.getName());
    
    // Threshold for low stock alerts
    private static final int LOW_STOCK_THRESHOLD = 5;
    
    // Cache for reservations to reduce database load
    private final Map<String, Integer> reservationCache = new ConcurrentHashMap<>();
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Updates stock quantity for a specific product
     * 
     * @param productId the ID of the product to update
     * @param quantity the new quantity value
     * @throws IllegalArgumentException if quantity is negative
     */
    @Transactional
    public void updateStock(Long productId, Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        
        try {
            Inventory inventory = inventoryRepository.findByProductId(productId);
            
            if (inventory == null) {
                LOGGER.warning("Inventory record not found for product ID: " + productId);
                inventory = new Inventory();
                inventory.setProductId(productId);
                inventory.setQuantity(0);
            }
            
            // Log significant stock changes for audit purposes
            if (Math.abs(inventory.getQuantity() - quantity) > 10) {
                LOGGER.info("Significant stock change for product " + productId + 
                        ": " + inventory.getQuantity() + " -> " + quantity);
            }
            
            inventory.setQuantity(quantity);
            inventoryRepository.save(inventory);
            
            // Check if we need to trigger low stock alert
            if (quantity <= LOW_STOCK_THRESHOLD) {
                notificationService.sendLowStockAlert(productId, quantity);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update stock for product " + productId, e);
            throw new RuntimeException("Error updating inventory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reserves stock for an order being processed
     * 
     * @param productId the ID of the product
     * @param variantId the ID of the product variant 
     * @param quantity the quantity to reserve
     * @return true if reservation was successful, false otherwise
     */
    @Transactional
    public Boolean reserveStock(Long productId, Long variantId, Integer quantity) {
        if (productId == null || variantId == null || quantity <= 0) {
            LOGGER.warning("Invalid reservation request: " + 
                    "productId=" + productId + ", variantId=" + variantId + 
                    ", quantity=" + quantity);
            return false;
        }
        
        String reservationKey = buildReservationKey(productId, variantId);
        
        // First check available stock
        int available = checkStockAvailability(productId, variantId);
        
        if (available < quantity) {
            LOGGER.info("Insufficient stock for reservation: requested=" + 
                    quantity + ", available=" + available);
            return false;
        }
        
        try {
            // Critical section - handle with care due to concurrency
            synchronized (this) {
                // Double check after lock acquisition
                available = checkStockAvailability(productId, variantId);
                if (available < quantity) {
                    return false;
                }
                
                // Update reservation cache
                int currentReservation = reservationCache.getOrDefault(reservationKey, 0);
                reservationCache.put(reservationKey, currentReservation + quantity);
                
                // Update physical inventory
                Inventory inventory = inventoryRepository.findByProductIdAndVariantId(productId, variantId);
                inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                inventoryRepository.save(inventory);
                
                // FIXME: Potential race condition if two threads calculate available stock simultaneously
                // TODO: Implement optimistic locking to prevent reservation conflicts
                
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to reserve stock", e);
            return false;
        }
    }
    
    /**
     * Releases previously reserved stock back to available inventory
     * 
     * @param productId the ID of the product
     * @param variantId the ID of the product variant
     * @param quantity the quantity to release
     */
    @Transactional
    public void releaseReservedStock(Long productId, Long variantId, Integer quantity) {
        if (productId == null || variantId == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid parameters for releasing stock");
        }
        
        String reservationKey = buildReservationKey(productId, variantId);
        
        try {
            // Synchronize to prevent concurrent modification issues
            synchronized (this) {
                Inventory inventory = inventoryRepository.findByProductIdAndVariantId(productId, variantId);
                
                if (inventory == null) {
                    LOGGER.severe("Cannot find inventory record for product " + 
                            productId + ", variant " + variantId);
                    throw new IllegalStateException("Inventory record not found");
                }
                
                int currentReserved = inventory.getReservedQuantity();
                
                // Prevent negative reserved quantity
                if (currentReserved < quantity) {
                    LOGGER.warning("Attempted to release more than reserved. Adjusting from " + 
                            quantity + " to " + currentReserved);
                    quantity = currentReserved;
                }
                
                // Update database
                inventory.setReservedQuantity(currentReserved - quantity);
                inventoryRepository.save(inventory);
                
                // Update cache
                if (reservationCache.containsKey(reservationKey)) {
                    int cachedReservation = reservationCache.get(reservationKey);
                    int newValue = Math.max(0, cachedReservation - quantity);
                    
                    if (newValue > 0) {
                        reservationCache.put(reservationKey, newValue);
                    } else {
                        reservationCache.remove(reservationKey);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to release reserved stock", e);
            throw new RuntimeException("Error releasing stock: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks available stock for a product variant
     * 
     * @param productId the ID of the product
     * @param variantId the ID of the product variant
     * @return available quantity (total minus reserved)
     */
    public Integer checkStockAvailability(Long productId, Long variantId) {
        try {
            Inventory inventory = inventoryRepository.findByProductIdAndVariantId(productId, variantId);
            
            if (inventory == null) {
                return 0;
            }
            
            // Calculate available quantity
            int totalQuantity = inventory.getQuantity();
            int reservedQuantity = inventory.getReservedQuantity();
            
            // Add reservation from cache (double-accounting protection)
            String reservationKey = buildReservationKey(productId, variantId);
            int cachedReservations = reservationCache.getOrDefault(reservationKey, 0);
            
            // Ensure we don't double-count reservations already in DB
            // but also account for any pending reservations not yet committed
            int effectiveReserved = Math.max(reservedQuantity, cachedReservations);
            
            return Math.max(0, totalQuantity - effectiveReserved);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check stock availability", e);
            return 0;
        }
    }
    
    /**
     * Retrieves a list of all inventory items with stock below the low stock threshold
     * 
     * @return List of low stock inventory items
     */
    public List<Inventory> getLowStockItems() {
        List<Inventory> lowStockItems = new ArrayList<>();
        
        try {
            // Get all inventory and filter for low stock
            List<Inventory> allInventory = inventoryRepository.findAll();
            
            for (Inventory item : allInventory) {
                int availableStock = item.getQuantity() - item.getReservedQuantity();
                
                if (availableStock <= LOW_STOCK_THRESHOLD) {
                    lowStockItems.add(item);
                }
            }
            
            return lowStockItems;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve low stock items", e);
            throw new RuntimeException("Error checking low stock items", e);
        }
    }
    
    /**
     * Processes all low stock alerts by checking inventory levels
     * and sending notifications for items below threshold
     */
    @Transactional
    public void processLowStockAlerts() {
        try {
            List<Inventory> lowStockItems = getLowStockItems();
            
            if (lowStockItems.isEmpty()) {
                LOGGER.info("No low stock items found during alert processing");
                return;
            }
            
            LOGGER.info("Processing low stock alerts for " + lowStockItems.size() + " items");
            
            // Process each low stock item with complex business logic
            for (Inventory item : lowStockItems) {
                // Skip if notification was already sent recently (within 24 hours)
                if (item.getLastAlertSent() != null && 
                        System.currentTimeMillis() - item.getLastAlertSent().getTime() < 24 * 60 * 60 * 1000) {
                    continue;
                }
                
                // Calculate actual available quantity
                int availableStock = item.getQuantity() - item.getReservedQuantity();
                
                // Different notification urgency based on stock level
                if (availableStock <= 0) {
                    notificationService.sendOutOfStockAlert(item.getProductId(), item.getVariantId());
                } else if (availableStock <= LOW_STOCK_THRESHOLD / 2) {
                    notificationService.sendCriticalLowStockAlert(item.getProductId(), item.getVariantId(), availableStock);
                } else {
                    notificationService.sendLowStockAlert(item.getProductId(), availableStock);
                }
                
                // Update last alert timestamp
                item.setLastAlertSent(new java.sql.Timestamp(System.currentTimeMillis()));
                inventoryRepository.save(item);
                
                // Add intentional delay to prevent notification system overload
                // TODO: Replace with batch processing mechanism for better performance
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process low stock alerts", e);
            throw new RuntimeException("Error processing low stock alerts", e);
        }
    }
    
    /**
     * Performs bulk update of inventory data for multiple products
     * 
     * @param inventoryUpdates list of inventory updates to process
     */
    @Transactional
    public void bulkUpdateInventory(List<InventoryDTO> inventoryUpdates) {
        if (inventoryUpdates == null || inventoryUpdates.isEmpty()) {
            LOGGER.warning("Empty inventory update list provided to bulk update");
            return;
        }
        
        LOGGER.info("Processing bulk inventory update for " + inventoryUpdates.size() + " items");
        
        // Track successes and failures for detailed reporting
        int successCount = 0;
        int failureCount = 0;
        List<String> failureDetails = new ArrayList<>();
        
        try {
            for (InventoryDTO update : inventoryUpdates) {
                try {
                    // Validate update data
                    if (update.getProductId() == null || update.getQuantity() == null || update.getQuantity() < 0) {
                        throw new IllegalArgumentException("Invalid inventory update data: " + update);
                    }
                    
                    Inventory inventory = inventoryRepository.findByProductId(update.getProductId());
                    boolean isNewRecord = false;
                    
                    if (inventory == null) {
                        inventory = new Inventory();
                        inventory.setProductId(update.getProductId());
                        inventory.setVariantId(update.getVariantId());
                        inventory.setReservedQuantity(0);
                        isNewRecord = true;
                    }
                    
                    // Handle specific update logic based on update type
                    switch (update.getUpdateType()) {
                        case SET:
                            inventory.setQuantity(update.getQuantity());
                            break;
                        case INCREMENT:
                            inventory.setQuantity(inventory.getQuantity() + update.getQuantity());
                            break;
                        case DECREMENT:
                            int newQuantity = inventory.getQuantity() - update.getQuantity();
                            if (newQuantity < 0) {
                                LOGGER.warning("Attempted to reduce quantity below 0 for product " + 
                                        update.getProductId() + ". Setting to 0 instead.");
                                newQuantity = 0;
                            }
                            inventory.setQuantity(newQuantity);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown update type: " + update.getUpdateType());
                    }
                    
                    // Apply any additional update properties if present
                    if (update.getLocation() != null) {
                        inventory.setLocation(update.getLocation());
                    }
                    
                    if (update.getAisleNumber() != null) {
                        inventory.setAisleNumber(update.getAisleNumber());
                    }
                    
                    if (update.getBinNumber() != null) {
                        inventory.setBinNumber(update.getBinNumber());
                    }
                    
                    // Save the updated inventory record
                    inventoryRepository.save(inventory);
                    successCount++;
                    
                    // Check if this update requires a low stock alert
                    if (inventory.getQuantity() <= LOW_STOCK_THRESHOLD) {
                        // Add to deferred processing queue instead of immediate notification
                        // to prevent notification storm during bulk updates
                        // FIXME: Implement proper message queue for this
                        if (isNewRecord || inventory.getLastAlertSent() == null) {
                            notificationService.scheduleLowStockAlert(inventory.getProductId(), inventory.getQuantity());
                        }
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    failureDetails.add("Failed to update product " + update.getProductId() + ": " + e.getMessage());
                    LOGGER.log(Level.WARNING, "Error during bulk update for product " + update.getProductId(), e);
                }
            }
            
            LOGGER.info("Bulk update completed. Success: " + successCount + ", Failures: " + failureCount);
            
            // If there were any failures, log detailed report
            if (failureCount > 0) {
                StringBuilder report = new StringBuilder("Bulk update failures:\n");
                for (String detail : failureDetails) {
                    report.append(" - ").append(detail).append("\n");
                }
                LOGGER.warning(report.toString());
                
                // Throw exception if more than 10% of updates failed
                if (failureCount > inventoryUpdates.size() * 0.1) {
                    throw new RuntimeException("Bulk update had significant failures: " + 
                            failureCount + " out of " + inventoryUpdates.size());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Critical failure during bulk inventory update", e);
            throw new RuntimeException("Bulk inventory update failed: " + e.getMessage(), e);
        }
    }
    
    // Helper method to build a consistent key for the reservation cache
    private String buildReservationKey(Long productId, Long variantId) {
        return productId + "-" + variantId;
    }
}