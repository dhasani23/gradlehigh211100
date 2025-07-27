package com.gradlehigh211100.productcatalog.repository;

import com.gradlehigh211100.productcatalog.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository interface for inventory CRUD operations and stock level queries.
 * Provides specialized methods for inventory management, stock level tracking,
 * and reordering functionality.
 * 
 * This repository is responsible for:
 * - Managing inventory records
 * - Tracking product stock levels
 * - Identifying items that need reordering
 * - Handling inventory updates
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    /**
     * Find inventory record by product ID
     *
     * @param productId The ID of the product
     * @return Optional containing the inventory if found, empty otherwise
     */
    Optional<Inventory> findByProductId(Long productId);
    
    /**
     * Find inventory record by variant ID
     *
     * @param variantId The ID of the product variant
     * @return Optional containing the inventory if found, empty otherwise
     */
    Optional<Inventory> findByVariantId(Long variantId);
    
    /**
     * Find inventory items with available quantity less than or equal to the specified threshold
     * Used to identify items that are low in stock and may need attention.
     *
     * @param threshold The quantity threshold to check against
     * @return List of inventory items with stock at or below the threshold
     */
    List<Inventory> findByAvailableQuantityLessThanEqual(Integer threshold);
    
    /**
     * Find inventory items where available quantity is less than their reorder point
     * This identifies items that need to be reordered based on their individual reorder settings.
     *
     * @return List of inventory items that need to be reordered
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderPoint")
    List<Inventory> findByAvailableQuantityLessThanReorderPoint();
    
    /**
     * Update available quantity for a specific inventory item
     *
     * @param id The inventory ID to update
     * @param quantity The new available quantity value
     * @return Number of records updated (should be 1 if successful)
     * 
     * FIXME: This method may not handle concurrent updates correctly - consider using optimistic locking
     * TODO: Add validation to prevent negative inventory quantities
     */
    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.availableQuantity = :quantity, i.lastUpdated = CURRENT_TIMESTAMP WHERE i.id = :id")
    Integer updateAvailableQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    /**
     * Complex query to find inventory items that need attention based on various criteria:
     * - Low stock (below reorder point)
     * - No recent orders in the last 30 days
     * - High demand items (sales velocity exceeds certain threshold)
     *
     * @param daysSinceLastOrder Days threshold for considering "no recent orders"
     * @return List of inventory items needing attention
     */
    @Query(value = "SELECT i.* FROM inventory i " +
            "LEFT JOIN orders o ON i.product_id = o.product_id " +
            "WHERE i.available_quantity <= i.reorder_point " +
            "AND (SELECT MAX(order_date) FROM orders WHERE product_id = i.product_id) < CURRENT_DATE - :daysSinceLastOrder " +
            "AND i.sales_velocity > i.reorder_threshold " +
            "ORDER BY (i.reorder_point - i.available_quantity) DESC", 
            nativeQuery = true)
    List<Inventory> findCriticalInventoryItems(@Param("daysSinceLastOrder") Integer daysSinceLastOrder);
    
    /**
     * Find inventory items that have not had any sales within a specified period
     * but still have significant stock levels.
     *
     * @param daysWithoutSales Number of days with no sales to consider
     * @param minimumQuantity Minimum quantity threshold to include in results
     * @return List of potentially obsolete inventory
     */
    @Query(value = "SELECT i.* FROM inventory i " +
            "WHERE i.last_sale_date < CURRENT_DATE - :daysWithoutSales " +
            "AND i.available_quantity >= :minimumQuantity " +
            "ORDER BY i.available_quantity DESC", 
            nativeQuery = true)
    List<Inventory> findPotentialObsoleteInventory(@Param("daysWithoutSales") Integer daysWithoutSales, 
                                                  @Param("minimumQuantity") Integer minimumQuantity);
    
    /**
     * Calculate total inventory value for reporting purposes
     * 
     * @return The total value of all inventory items
     */
    @Query("SELECT SUM(i.availableQuantity * i.unitCost) FROM Inventory i")
    Double calculateTotalInventoryValue();
    
    /**
     * Find inventory items with inconsistent quantities based on actual stock counts
     * 
     * @param threshold The percentage threshold for considering an inconsistency
     * @return List of inventory items with possible discrepancies
     * 
     * TODO: Add stock counting functionality to verify inventory accuracy
     */
    @Query(value = "SELECT i.* FROM inventory i " +
            "JOIN stock_counts sc ON i.id = sc.inventory_id " +
            "WHERE ABS(i.available_quantity - sc.counted_quantity) / i.available_quantity > :threshold / 100.0 " +
            "ORDER BY ABS(i.available_quantity - sc.counted_quantity) / i.available_quantity DESC",
            nativeQuery = true)
    List<Inventory> findInventoryDiscrepancies(@Param("threshold") Double threshold);
}