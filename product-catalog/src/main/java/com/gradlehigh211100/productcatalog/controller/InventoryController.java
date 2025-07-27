package com.gradlehigh211100.productcatalog.controller;

import com.gradlehigh211100.productcatalog.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller providing API endpoints for inventory management operations
 * including stock level updates and queries.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int MAX_BULK_OPERATIONS = 100;
    
    @Autowired
    private InventoryService inventoryService;
    
    // Cache for tracking API rate limiting
    private final Map<String, AtomicInteger> apiCallsCounter = new HashMap<>();
    
    /**
     * Update stock quantity endpoint
     * 
     * @param productId The product ID to update
     * @param quantity The new quantity value
     * @return ResponseEntity with appropriate status
     */
    @PutMapping("/product/{productId}/stock")
    public ResponseEntity<Void> updateStock(@PathVariable Long productId, 
                                           @RequestParam Integer quantity) {
        logger.info("Updating stock for product ID: {} to quantity: {}", productId, quantity);
        
        // Input validation with complex conditions
        if (productId == null || productId <= 0) {
            logger.error("Invalid product ID: {}", productId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        if (quantity == null) {
            logger.error("Quantity cannot be null for product ID: {}", productId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        // Rate limiting check
        String clientIp = "client-ip"; // In real implementation, get from request
        if (!checkRateLimit(clientIp, "updateStock")) {
            logger.warn("Rate limit exceeded for client: {}", clientIp);
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
        
        try {
            // Business logic with error handling
            if (quantity < 0) {
                logger.warn("Attempting to set negative quantity: {} for product: {}", quantity, productId);
                if (Math.abs(quantity) > 1000) {
                    logger.error("Potentially erroneous large negative quantity");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else if (quantity > 10000) {
                // Special handling for large quantities
                logger.warn("Large quantity update detected: {}", quantity);
                // Might need approval workflow in a real system
            }
            
            boolean success = inventoryService.updateProductStock(productId, quantity);
            
            // Handle different failure scenarios
            if (!success) {
                if (!inventoryService.productExists(productId)) {
                    logger.error("Product not found: {}", productId);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                } else {
                    logger.error("Failed to update stock for product ID: {}", productId);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            
            // Log threshold crossing events
            if (quantity <= LOW_STOCK_THRESHOLD) {
                logger.warn("Product ID {} is now at low stock level: {}", productId, quantity);
                // Trigger alerts or notifications in a real system
            }
            
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Exception while updating stock for product ID: {}", productId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Check stock availability endpoint
     * 
     * @param productId The product ID to check
     * @param variantId The variant ID to check (optional)
     * @return ResponseEntity containing available quantity
     */
    @GetMapping("/product/{productId}/stock")
    public ResponseEntity<Integer> checkStockAvailability(@PathVariable Long productId,
                                                        @RequestParam(required = false) Long variantId) {
        logger.info("Checking stock availability for product ID: {}, variant ID: {}", productId, variantId);
        
        // Input validation
        if (productId == null || productId <= 0) {
            logger.error("Invalid product ID: {}", productId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            int availableStock;
            
            // Complex business logic with different paths
            if (variantId != null) {
                if (variantId <= 0) {
                    logger.error("Invalid variant ID: {}", variantId);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                
                // Check if product exists first
                if (!inventoryService.productExists(productId)) {
                    logger.error("Product not found: {}", productId);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                
                // Check if variant exists
                if (!inventoryService.variantExists(productId, variantId)) {
                    logger.error("Variant ID: {} not found for product ID: {}", variantId, productId);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                
                // Get variant stock
                availableStock = inventoryService.getVariantStock(productId, variantId);
            } else {
                // Check if product exists
                if (!inventoryService.productExists(productId)) {
                    logger.error("Product not found: {}", productId);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                
                // Get product stock across all variants
                availableStock = inventoryService.getProductStock(productId);
            }
            
            // Additional handling for special cases
            if (availableStock < 0) {
                logger.error("Negative stock found for product ID: {}, variant ID: {}", productId, variantId);
                // FIXME: Database inconsistency - stock should never be negative
                availableStock = 0;
            }
            
            logger.info("Stock availability for product ID: {}, variant ID: {} is {}", 
                    productId, variantId, availableStock);
            return ResponseEntity.ok(availableStock);
        } catch (Exception e) {
            logger.error("Exception while checking stock for product ID: {}", productId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Reserve stock endpoint
     * 
     * @param productId The product ID to reserve stock from
     * @param variantId The variant ID to reserve stock from
     * @param quantity The quantity to reserve
     * @return ResponseEntity containing success or failure status
     */
    @PostMapping("/product/{productId}/reserve")
    public ResponseEntity<Boolean> reserveStock(@PathVariable Long productId,
                                             @RequestParam Long variantId,
                                             @RequestParam Integer quantity) {
        logger.info("Reserving stock for product ID: {}, variant ID: {}, quantity: {}", 
                productId, variantId, quantity);
        
        // Input validation with complex conditions
        if (productId == null || productId <= 0) {
            logger.error("Invalid product ID: {}", productId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        if (variantId == null || variantId <= 0) {
            logger.error("Invalid variant ID: {}", variantId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        if (quantity == null || quantity <= 0) {
            logger.error("Invalid quantity: {}", quantity);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            // Check if product and variant exist
            if (!inventoryService.productExists(productId)) {
                logger.error("Product not found: {}", productId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            if (!inventoryService.variantExists(productId, variantId)) {
                logger.error("Variant ID: {} not found for product ID: {}", variantId, productId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Check if there's enough stock
            int availableStock = inventoryService.getVariantStock(productId, variantId);
            if (availableStock < quantity) {
                logger.warn("Insufficient stock for product ID: {}, variant ID: {}. Requested: {}, Available: {}", 
                        productId, variantId, quantity, availableStock);
                return ResponseEntity.ok(false);
            }
            
            // Try to reserve stock
            boolean reserved = inventoryService.reserveStock(productId, variantId, quantity);
            
            // Post reservation checks and actions
            if (reserved) {
                logger.info("Successfully reserved {} units for product ID: {}, variant ID: {}", 
                        quantity, productId, variantId);
                
                // Check if stock level is now low
                int remainingStock = inventoryService.getVariantStock(productId, variantId);
                if (remainingStock <= LOW_STOCK_THRESHOLD) {
                    logger.warn("Product ID: {}, variant ID: {} is now at low stock level: {}", 
                            productId, variantId, remainingStock);
                    // TODO: Implement notification system for low stock alerts
                }
                
                return ResponseEntity.ok(true);
            } else {
                logger.error("Failed to reserve stock for product ID: {}, variant ID: {}", productId, variantId);
                return ResponseEntity.ok(false);
            }
        } catch (Exception e) {
            logger.error("Exception while reserving stock for product ID: {}", productId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Release reserved stock endpoint
     * 
     * @param productId The product ID to release stock for
     * @param variantId The variant ID to release stock for
     * @param quantity The quantity to release
     * @return ResponseEntity with appropriate status
     */
    @PostMapping("/product/{productId}/release")
    public ResponseEntity<Void> releaseReservedStock(@PathVariable Long productId,
                                                  @RequestParam Long variantId,
                                                  @RequestParam Integer quantity) {
        logger.info("Releasing reserved stock for product ID: {}, variant ID: {}, quantity: {}", 
                productId, variantId, quantity);
        
        // Input validation
        if (productId == null || productId <= 0) {
            logger.error("Invalid product ID: {}", productId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        if (variantId == null || variantId <= 0) {
            logger.error("Invalid variant ID: {}", variantId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        if (quantity == null || quantity <= 0) {
            logger.error("Invalid quantity: {}", quantity);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            // Check if product and variant exist
            if (!inventoryService.productExists(productId)) {
                logger.error("Product not found: {}", productId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            if (!inventoryService.variantExists(productId, variantId)) {
                logger.error("Variant ID: {} not found for product ID: {}", variantId, productId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Check if there's enough reserved stock
            int reservedStock = inventoryService.getReservedStock(productId, variantId);
            if (reservedStock < quantity) {
                logger.warn("Insufficient reserved stock for product ID: {}, variant ID: {}. " +
                        "Requested to release: {}, Actually reserved: {}", 
                        productId, variantId, quantity, reservedStock);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            // Release the reserved stock
            boolean released = inventoryService.releaseReservedStock(productId, variantId, quantity);
            
            if (released) {
                logger.info("Successfully released {} units for product ID: {}, variant ID: {}", 
                        quantity, productId, variantId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.error("Failed to release reserved stock for product ID: {}, variant ID: {}", 
                        productId, variantId);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            logger.error("Exception while releasing reserved stock for product ID: {}", productId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get low stock items endpoint
     * 
     * @return ResponseEntity containing list of low stock items
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryDTO>> getLowStockItems() {
        logger.info("Getting low stock items");
        
        try {
            // Different thresholds for different product types
            Map<String, Integer> categoryThresholds = new HashMap<>();
            categoryThresholds.put("ELECTRONICS", 10);
            categoryThresholds.put("CLOTHING", 5);
            categoryThresholds.put("FOOD", 20);
            categoryThresholds.put("DEFAULT", LOW_STOCK_THRESHOLD);
            
            // Get low stock items
            List<InventoryDTO> lowStockItems = inventoryService.getLowStockItems(categoryThresholds);
            
            // Additional processing based on business rules
            if (lowStockItems.isEmpty()) {
                logger.info("No low stock items found");
            } else {
                logger.warn("Found {} items with low stock", lowStockItems.size());
                
                // TODO: Implement automatic reordering for critical items
                for (InventoryDTO item : lowStockItems) {
                    if (item.getQuantity() <= 0) {
                        logger.error("Product {} is out of stock", item.getProductId());
                        // In a real system, trigger immediate reorder or alert
                    }
                }
            }
            
            return ResponseEntity.ok(lowStockItems);
        } catch (Exception e) {
            logger.error("Exception while getting low stock items", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Bulk update inventory endpoint
     * 
     * @param inventoryUpdates List of inventory items to update
     * @return ResponseEntity with appropriate status
     */
    @PutMapping("/bulk-update")
    public ResponseEntity<Void> bulkUpdateInventory(@RequestBody List<InventoryDTO> inventoryUpdates) {
        logger.info("Processing bulk inventory update with {} items", 
                inventoryUpdates != null ? inventoryUpdates.size() : 0);
        
        // Input validation
        if (inventoryUpdates == null || inventoryUpdates.isEmpty()) {
            logger.error("No inventory updates provided");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        if (inventoryUpdates.size() > MAX_BULK_OPERATIONS) {
            logger.error("Bulk update exceeds maximum allowed operations: {}", inventoryUpdates.size());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            // Group updates by product category for specialized processing
            Map<String, List<InventoryDTO>> updatesByCategory = new HashMap<>();
            
            // Process each update
            for (InventoryDTO update : inventoryUpdates) {
                // Validate each update
                if (update.getProductId() == null || update.getProductId() <= 0) {
                    logger.error("Invalid product ID in bulk update: {}", update.getProductId());
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                
                if (update.getQuantity() == null) {
                    logger.error("Missing quantity for product ID: {}", update.getProductId());
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                
                // Check if product exists
                if (!inventoryService.productExists(update.getProductId())) {
                    logger.error("Product not found: {}", update.getProductId());
                    // In bulk operations, we might want to continue with other updates
                    continue;
                }
                
                // Group by category for specialized processing
                String category = inventoryService.getProductCategory(update.getProductId());
                updatesByCategory.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(update);
            }
            
            // Process updates by category
            for (Map.Entry<String, List<InventoryDTO>> entry : updatesByCategory.entrySet()) {
                String category = entry.getKey();
                List<InventoryDTO> categoryUpdates = entry.getValue();
                
                logger.info("Processing {} updates for category: {}", categoryUpdates.size(), category);
                
                // Different handling based on category
                switch (category) {
                    case "PERISHABLE":
                        // Special handling for perishable items
                        processBulkPerishableUpdates(categoryUpdates);
                        break;
                    case "HAZARDOUS":
                        // Special handling for hazardous items
                        processBulkHazardousUpdates(categoryUpdates);
                        break;
                    default:
                        // Standard processing
                        processBulkStandardUpdates(categoryUpdates);
                        break;
                }
            }
            
            logger.info("Bulk inventory update completed successfully");
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Exception during bulk inventory update", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Process standard item updates
     * 
     * @param updates List of standard items to update
     */
    private void processBulkStandardUpdates(List<InventoryDTO> updates) {
        for (InventoryDTO update : updates) {
            try {
                // Regular update logic
                boolean success = inventoryService.updateProductStock(update.getProductId(), update.getQuantity());
                
                if (!success) {
                    logger.error("Failed to update stock for product ID: {}", update.getProductId());
                }
            } catch (Exception e) {
                logger.error("Error updating product ID: {}", update.getProductId(), e);
            }
        }
    }
    
    /**
     * Process perishable item updates
     * 
     * @param updates List of perishable items to update
     */
    private void processBulkPerishableUpdates(List<InventoryDTO> updates) {
        // Special handling for perishable items
        for (InventoryDTO update : updates) {
            try {
                // Check expiration date
                if (update.getExpirationDate() == null) {
                    logger.error("Missing expiration date for perishable product ID: {}", update.getProductId());
                    continue;
                }
                
                // TODO: Implement expiration date validation and handling
                boolean success = inventoryService.updatePerishableProductStock(
                        update.getProductId(), update.getQuantity(), update.getExpirationDate());
                
                if (!success) {
                    logger.error("Failed to update perishable stock for product ID: {}", update.getProductId());
                }
            } catch (Exception e) {
                logger.error("Error updating perishable product ID: {}", update.getProductId(), e);
            }
        }
    }
    
    /**
     * Process hazardous item updates
     * 
     * @param updates List of hazardous items to update
     */
    private void processBulkHazardousUpdates(List<InventoryDTO> updates) {
        // Special handling for hazardous items
        for (InventoryDTO update : updates) {
            try {
                // Check safety information
                if (update.getSafetyInformation() == null || update.getSafetyInformation().isEmpty()) {
                    logger.error("Missing safety information for hazardous product ID: {}", update.getProductId());
                    continue;
                }
                
                // TODO: Implement safety validation and handling
                boolean success = inventoryService.updateHazardousProductStock(
                        update.getProductId(), update.getQuantity(), update.getSafetyInformation());
                
                if (!success) {
                    logger.error("Failed to update hazardous stock for product ID: {}", update.getProductId());
                }
            } catch (Exception e) {
                logger.error("Error updating hazardous product ID: {}", update.getProductId(), e);
            }
        }
    }
    
    /**
     * Check rate limit for API calls
     * 
     * @param clientId Client identifier
     * @param operation The operation being performed
     * @return True if within rate limit, false otherwise
     */
    private boolean checkRateLimit(String clientId, String operation) {
        // Simple rate limiting implementation
        String key = clientId + ":" + operation;
        AtomicInteger counter = apiCallsCounter.computeIfAbsent(key, k -> new AtomicInteger(0));
        
        // For demonstration purposes - in a real system would use a time window
        int calls = counter.incrementAndGet();
        
        // Different limits for different operations
        int limit;
        switch (operation) {
            case "updateStock":
                limit = 10;
                break;
            case "bulkUpdateInventory":
                limit = 5;
                break;
            default:
                limit = 20;
        }
        
        // Reset counter periodically in a real implementation
        
        return calls <= limit;
    }
}