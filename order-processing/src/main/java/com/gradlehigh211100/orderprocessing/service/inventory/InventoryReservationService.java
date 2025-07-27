package com.gradlehigh211100.orderprocessing.service.inventory;

import com.gradlehigh211100.orderprocessing.model.entity.Order;
import com.gradlehigh211100.orderprocessing.model.entity.OrderItem;
import com.gradlehigh211100.orderprocessing.model.reservation.ReservationResult;
import com.gradlehigh211100.productcatalog.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service responsible for managing inventory reservation and release operations
 * ensuring product availability during order processing and handling stock allocation.
 * 
 * This service handles the complex logic of reserving inventory items for orders,
 * checking availability, and releasing inventory when orders are cancelled or modified.
 */
@Service
public class InventoryReservationService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 200;
    
    // Service for inventory operations
    private final InventoryService inventoryService;
    
    // Logger for inventory events
    private final Logger logger = LoggerFactory.getLogger(InventoryReservationService.class);
    
    // Keeps track of currently in-progress reservations to prevent race conditions
    private final Map<Long, Lock> productReservationLocks = new ConcurrentHashMap<>();
    
    // Keeps track of which orders have reserved which products and quantities
    private final Map<Long, Map<Long, Integer>> orderReservations = new ConcurrentHashMap<>();

    /**
     * Constructor for dependency injection
     * 
     * @param inventoryService the inventory service
     */
    @Autowired
    public InventoryReservationService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        logger.info("InventoryReservationService initialized");
    }

    /**
     * Reserves inventory for all items in the order.
     * This is a critical operation that must ensure inventory consistency
     * across concurrent reservation attempts.
     *
     * @param order the order containing items to reserve
     * @return a ReservationResult indicating success or failure with details
     */
    @Transactional
    public ReservationResult reserveInventory(Order order) {
        if (order == null) {
            logger.error("Cannot reserve inventory for null order");
            ReservationResult result = new ReservationResult(false);
            result.setMessage("Cannot reserve inventory for null order");
            return result;
        }

        logger.info("Starting inventory reservation for order ID: {}", order.getId());
        ReservationResult result = new ReservationResult(true);
        List<Long> reservedProducts = new ArrayList<>();

        try {
            // First phase: check availability of all products
            for (OrderItem item : order.getItems()) {
                if (!checkAvailability(item.getProductId(), item.getQuantity())) {
                    logger.warn("Product ID {} not available in requested quantity {}", 
                            item.getProductId(), item.getQuantity());
                    result.setSuccessful(false);
                    result.addUnavailableProduct(item.getProductId(), item.getQuantity());
                }
            }

            // If all products are available, proceed with reservation
            if (result.isSuccessful()) {
                for (OrderItem item : order.getItems()) {
                    boolean reserved = false;
                    int attempts = 0;

                    // Retry logic for reservation
                    while (!reserved && attempts < MAX_RETRY_ATTEMPTS) {
                        reserved = reserveProduct(item.getProductId(), item.getQuantity(), order.getId());
                        if (reserved) {
                            reservedProducts.add(item.getProductId());
                        } else {
                            attempts++;
                            if (attempts < MAX_RETRY_ATTEMPTS) {
                                logger.warn("Reservation attempt {} failed for product ID: {}. Retrying...", 
                                        attempts, item.getProductId());
                                Thread.sleep(RETRY_DELAY_MS);
                            }
                        }
                    }

                    if (!reserved) {
                        logger.error("Failed to reserve product ID: {} after {} attempts", 
                                item.getProductId(), MAX_RETRY_ATTEMPTS);
                        result.setSuccessful(false);
                        result.addUnavailableProduct(item.getProductId(), item.getQuantity());
                        break;
                    }
                }
            }

            // If reservation failed, rollback any reserved products
            if (!result.isSuccessful()) {
                for (Long productId : reservedProducts) {
                    // Find the corresponding item to get the quantity
                    for (OrderItem item : order.getItems()) {
                        if (item.getProductId().equals(productId)) {
                            releaseProduct(productId, item.getQuantity(), order.getId());
                            break;
                        }
                    }
                }
                result.setMessage("Inventory reservation failed: Some products are not available in the requested quantity");
            } else {
                result.setMessage("Inventory successfully reserved for order ID: " + order.getId());
                logger.info("Successfully reserved inventory for order ID: {}", order.getId());
            }
        } catch (Exception e) {
            logger.error("Error during inventory reservation for order ID: {}", order.getId(), e);
            result.setSuccessful(false);
            result.setMessage("Inventory reservation failed due to system error: " + e.getMessage());
            
            // Rollback any reserved products in case of exception
            for (Long productId : reservedProducts) {
                try {
                    // Find the corresponding item to get the quantity
                    for (OrderItem item : order.getItems()) {
                        if (item.getProductId().equals(productId)) {
                            releaseProduct(productId, item.getQuantity(), order.getId());
                            break;
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error during rollback of reservation for product ID: {}", productId, ex);
                }
            }
        }

        return result;
    }

    /**
     * Releases reserved inventory for cancelled orders.
     * This method ensures all products reserved for an order are released
     * back into available inventory.
     *
     * @param order the order containing items to release
     */
    @Transactional
    public void releaseInventory(Order order) {
        if (order == null) {
            logger.error("Cannot release inventory for null order");
            return;
        }

        logger.info("Releasing reserved inventory for order ID: {}", order.getId());

        try {
            Map<Long, Integer> reservations = orderReservations.get(order.getId());
            
            if (reservations != null) {
                for (Map.Entry<Long, Integer> entry : reservations.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();
                    releaseProduct(productId, quantity, order.getId());
                }
                
                orderReservations.remove(order.getId());
                logger.info("Successfully released all inventory for order ID: {}", order.getId());
            } else {
                // Order might have products not properly tracked in orderReservations
                logger.warn("No reservation record found for order ID: {}. Attempting to release based on order items.", order.getId());
                
                for (OrderItem item : order.getItems()) {
                    try {
                        releaseProduct(item.getProductId(), item.getQuantity(), order.getId());
                    } catch (Exception e) {
                        logger.error("Failed to release product ID: {} for order ID: {}", 
                                item.getProductId(), order.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error releasing inventory for order ID: {}", order.getId(), e);
            // FIXME: Consider implementing a retry mechanism or queuing failed releases for later processing
        }
    }

    /**
     * Checks if a product is available in the required quantity.
     * This method verifies inventory without making any reservations.
     *
     * @param productId the product ID to check
     * @param quantity the quantity needed
     * @return true if product is available in required quantity, false otherwise
     */
    public boolean checkAvailability(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            logger.warn("Invalid check availability request: productId={}, quantity={}", productId, quantity);
            return false;
        }

        try {
            logger.debug("Checking availability of product ID: {} for quantity: {}", productId, quantity);
            int availableQuantity = inventoryService.getAvailableQuantity(productId);
            boolean isAvailable = availableQuantity >= quantity;
            
            if (!isAvailable) {
                logger.warn("Product ID: {} not available. Requested: {}, Available: {}", 
                        productId, quantity, availableQuantity);
            }
            
            return isAvailable;
        } catch (Exception e) {
            logger.error("Error checking availability for product ID: {}", productId, e);
            return false;
        }
    }

    /**
     * Reserves a specific product quantity for an order.
     * Uses a locking mechanism to ensure thread-safety during reservation.
     *
     * @param productId the product ID to reserve
     * @param quantity the quantity to reserve
     * @param orderId the order ID requesting the reservation
     * @return true if reservation was successful, false otherwise
     */
    public boolean reserveProduct(Long productId, Integer quantity, Long orderId) {
        if (productId == null || quantity == null || quantity <= 0 || orderId == null) {
            logger.warn("Invalid reserve product request: productId={}, quantity={}, orderId={}", 
                    productId, quantity, orderId);
            return false;
        }

        Lock lock = productReservationLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        boolean lockAcquired = false;

        try {
            // Try to acquire lock for this product with timeout
            lockAcquired = lock.tryLock();
            if (!lockAcquired) {
                logger.warn("Could not acquire lock for product ID: {} - concurrent reservation in progress", productId);
                return false;
            }

            // Check current availability
            int availableQuantity = inventoryService.getAvailableQuantity(productId);
            if (availableQuantity < quantity) {
                logger.warn("Insufficient inventory for product ID: {}. Requested: {}, Available: {}", 
                        productId, quantity, availableQuantity);
                return false;
            }

            // Attempt to reserve in inventory system
            boolean reserved = inventoryService.reduceStock(productId, quantity);
            
            if (reserved) {
                // Track this reservation
                orderReservations.computeIfAbsent(orderId, k -> new ConcurrentHashMap<>())
                    .merge(productId, quantity, Integer::sum);
                
                logger.info("Successfully reserved {} units of product ID: {} for order ID: {}", 
                        quantity, productId, orderId);
                return true;
            } else {
                logger.error("Failed to reserve product ID: {} through inventory service", productId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error reserving product ID: {} for order ID: {}", productId, orderId, e);
            return false;
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
            
            // Cleanup empty lock entries to prevent memory leaks
            if (lock instanceof ReentrantLock && !((ReentrantLock) lock).hasQueuedThreads()) {
                productReservationLocks.remove(productId);
            }
        }
    }

    /**
     * Releases a specific product reservation.
     * Returns the reserved quantity back to available inventory.
     *
     * @param productId the product ID to release
     * @param quantity the quantity to release
     * @param orderId the order ID that made the reservation
     */
    public void releaseProduct(Long productId, Integer quantity, Long orderId) {
        if (productId == null || quantity == null || quantity <= 0 || orderId == null) {
            logger.warn("Invalid release product request: productId={}, quantity={}, orderId={}", 
                    productId, quantity, orderId);
            return;
        }

        Lock lock = productReservationLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        boolean lockAcquired = false;

        try {
            // Try to acquire lock for this product with timeout
            lockAcquired = lock.tryLock();
            if (!lockAcquired) {
                logger.warn("Could not acquire lock for product ID: {} during release - will retry", productId);
                // TODO: Implement retry mechanism or add to a queue for later processing
                return;
            }

            // Update tracking data
            Map<Long, Integer> orderProducts = orderReservations.get(orderId);
            if (orderProducts != null) {
                Integer reservedQuantity = orderProducts.get(productId);
                if (reservedQuantity != null) {
                    // Calculate actual quantity to release (never release more than was reserved)
                    int actualReleaseQuantity = Math.min(quantity, reservedQuantity);
                    
                    if (actualReleaseQuantity > 0) {
                        // Release in inventory system
                        boolean released = inventoryService.increaseStock(productId, actualReleaseQuantity);
                        
                        if (released) {
                            // Update our tracking
                            if (reservedQuantity == actualReleaseQuantity) {
                                orderProducts.remove(productId);
                            } else {
                                orderProducts.put(productId, reservedQuantity - actualReleaseQuantity);
                            }
                            
                            // Clean up empty entries
                            if (orderProducts.isEmpty()) {
                                orderReservations.remove(orderId);
                            }
                            
                            logger.info("Released {} units of product ID: {} for order ID: {}", 
                                    actualReleaseQuantity, productId, orderId);
                        } else {
                            logger.error("Failed to release product ID: {} through inventory service", productId);
                        }
                    } else {
                        logger.warn("Attempted to release zero quantity for product ID: {}", productId);
                    }
                } else {
                    logger.warn("No reservation found for product ID: {} in order ID: {}", productId, orderId);
                }
            } else {
                logger.warn("No reservation record found for order ID: {}", orderId);
                
                // Try to release anyway, could be data inconsistency
                inventoryService.increaseStock(productId, quantity);
                logger.info("Released {} units of product ID: {} for order ID: {} without tracking record", 
                        quantity, productId, orderId);
            }
        } catch (Exception e) {
            logger.error("Error releasing product ID: {} for order ID: {}", productId, orderId, e);
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
            
            // Cleanup empty lock entries
            if (lock instanceof ReentrantLock && !((ReentrantLock) lock).hasQueuedThreads()) {
                productReservationLocks.remove(productId);
            }
        }
    }

    /**
     * Confirms inventory reservation for shipped order.
     * This finalizes the reservation and removes tracking data.
     *
     * @param orderId the order ID to confirm
     */
    @Transactional
    public void confirmReservation(Long orderId) {
        if (orderId == null) {
            logger.error("Cannot confirm reservation for null order ID");
            return;
        }

        logger.info("Confirming inventory reservation for order ID: {}", orderId);

        try {
            // Once order is shipped, we can remove it from our tracking
            Map<Long, Integer> reservations = orderReservations.remove(orderId);
            
            if (reservations != null) {
                logger.info("Confirmed and finalized inventory reservation for order ID: {} with {} product(s)", 
                        orderId, reservations.size());
                
                // Additional business logic for confirmed orders can be added here
                // For example, updating inventory analytics, recording sales data, etc.
                
                // FIXME: Implement integration with analytics system once available
                
                // TODO: Consider implementing callbacks to notify other systems
                // about the finalized inventory allocation
            } else {
                logger.warn("No reservation record found to confirm for order ID: {}", orderId);
            }
            
            // Update inventory system to mark these items as sold rather than just reserved
            boolean updated = inventoryService.markAsShipped(orderId);
            
            if (!updated) {
                logger.error("Failed to update inventory system for shipped order ID: {}", orderId);
                // TODO: Implement retry mechanism or error alert
            }
        } catch (Exception e) {
            logger.error("Error confirming reservation for order ID: {}", orderId, e);
        }
    }
}