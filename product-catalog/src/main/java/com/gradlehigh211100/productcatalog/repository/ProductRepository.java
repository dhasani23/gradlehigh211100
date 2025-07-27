package com.gradlehigh211100.productcatalog.repository;

import com.gradlehigh211100.productcatalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository interface for product CRUD operations and custom queries.
 * This interface provides methods for querying the product database with various filtering criteria.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its SKU (Stock Keeping Unit) in a case-insensitive manner.
     * 
     * @param sku the product SKU to search for
     * @return an Optional containing the found product or empty if not found
     */
    Optional<Product> findBySkuIgnoreCase(String sku);
    
    /**
     * Find all products that belong to a specific category.
     * 
     * @param categoryId the ID of the category to filter products by
     * @return a list of products in the specified category
     */
    List<Product> findByCategoryId(Long categoryId);
    
    /**
     * Find all products from a specific brand in a case-insensitive manner.
     * 
     * @param brand the brand name to search for
     * @return a list of products from the specified brand
     */
    List<Product> findByBrandIgnoreCase(String brand);
    
    /**
     * Find all active products and order them by name in ascending order.
     * 
     * @return a list of active products ordered by name
     */
    List<Product> findByIsActiveTrueOrderByNameAsc();
    
    /**
     * Find products that contain the given name string (partial match) in a case-insensitive manner.
     * 
     * @param name the name substring to search for
     * @return a list of products matching the name pattern
     */
    List<Product> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find products with base prices within the specified range (inclusive).
     * 
     * @param minPrice the minimum price in the range
     * @param maxPrice the maximum price in the range
     * @return a list of products within the price range
     */
    List<Product> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Custom query to find featured products that are active and have inventory available.
     * This demonstrates a more complex query using JPQL.
     * 
     * @param minimumStock the minimum stock level products must have
     * @return a list of featured products with available inventory
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isFeatured = true AND p.stockQuantity >= :minimumStock ORDER BY p.name")
    List<Product> findFeaturedProductsWithAvailableStock(@Param("minimumStock") Integer minimumStock);
    
    /**
     * Find products that have been recently updated.
     * 
     * @param days number of days to look back
     * @return a list of recently updated products
     */
    @Query(value = "SELECT p.* FROM products p WHERE p.last_updated >= (CURRENT_DATE - INTERVAL :days DAY)", nativeQuery = true)
    List<Product> findRecentlyUpdatedProducts(@Param("days") Integer days);
    
    /**
     * Find top selling products based on order quantity.
     * This demonstrates a native SQL query approach for complex operations.
     * 
     * @param limit the maximum number of products to return
     * @return a list of top selling products
     */
    @Query(value = 
           "SELECT p.* FROM products p " +
           "JOIN order_items oi ON p.id = oi.product_id " +
           "GROUP BY p.id " +
           "ORDER BY SUM(oi.quantity) DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<Product> findTopSellingProducts(@Param("limit") Integer limit);
    
    /**
     * Find products that are low in stock and need reordering.
     * 
     * @param threshold the stock level threshold below which products are considered low
     * @return a list of products that are low in stock
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity <= p.reorderThreshold AND p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
    
    /**
     * Find products with no recent orders in the given period.
     * 
     * TODO: Optimize this query for better performance with large datasets
     * FIXME: The date calculation may not work consistently across different database systems
     * 
     * @param months number of months to look back
     * @return a list of products with no recent orders
     */
    @Query(value = 
           "SELECT p.* FROM products p " +
           "LEFT JOIN order_items oi ON p.id = oi.product_id " +
           "LEFT JOIN orders o ON oi.order_id = o.id AND o.order_date >= (CURRENT_DATE - INTERVAL :months MONTH) " +
           "WHERE o.id IS NULL AND p.is_active = true", 
           nativeQuery = true)
    List<Product> findProductsWithNoRecentOrders(@Param("months") Integer months);
}