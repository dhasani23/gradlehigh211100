package com.gradlehigh211100.productcatalog.repository;

import com.gradlehigh211100.productcatalog.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository interface for product variant CRUD operations and variant-specific queries.
 * This interface provides methods to interact with product variants in the database
 * with a focus on custom query operations.
 *
 * @author gradlehigh211100
 * @version 1.0
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Find all product variants for a specific product.
     *
     * @param productId the ID of the product to find variants for
     * @return List of product variants belonging to the specified product
     */
    List<ProductVariant> findByProductId(Long productId);

    /**
     * Find a product variant by its SKU in a case-insensitive manner.
     * This method is optimized for unique SKU lookups.
     *
     * @param sku the SKU to search for, case insensitive
     * @return Optional containing the product variant if found, empty otherwise
     */
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);

    /**
     * Find all active product variants for a specific product.
     * This is useful for frontend product display where only active variants should be shown.
     *
     * @param productId the ID of the product to find active variants for
     * @return List of active product variants belonging to the specified product
     */
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    /**
     * Find all product variants with a specific size.
     * This method supports inventory management and size-based filtering.
     *
     * @param size the size to search for
     * @return List of product variants matching the specified size
     */
    List<ProductVariant> findBySize(String size);

    /**
     * Find all product variants with a specific color.
     * This method supports inventory management and color-based filtering.
     *
     * @param color the color to search for
     * @return List of product variants matching the specified color
     */
    List<ProductVariant> findByColor(String color);

    /**
     * Find all product variants with stock levels below the specified threshold.
     * This helps in inventory management by identifying products needing restocking.
     *
     * @param threshold the minimum stock level
     * @return List of product variants with stock below the threshold
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stockQuantity < :threshold")
    List<ProductVariant> findVariantsWithLowStock(@Param("threshold") Integer threshold);

    /**
     * Find all product variants that match multiple criteria.
     * This complex query demonstrates higher cyclomatic complexity by handling
     * multiple optional search parameters.
     *
     * @param productId the ID of the product (optional)
     * @param size the size to search for (optional)
     * @param color the color to search for (optional)
     * @param minPrice minimum price threshold (optional)
     * @param maxPrice maximum price threshold (optional)
     * @return List of product variants matching the specified criteria
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE " +
           "(:productId IS NULL OR pv.productId = :productId) AND " +
           "(:size IS NULL OR pv.size = :size) AND " +
           "(:color IS NULL OR pv.color = :color) AND " +
           "(:minPrice IS NULL OR pv.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR pv.price <= :maxPrice) AND " +
           "pv.isActive = true")
    List<ProductVariant> findVariantsByMultipleCriteria(
            @Param("productId") Long productId,
            @Param("size") String size,
            @Param("color") String color,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);

    /**
     * Find product variants that have been updated since the specified timestamp.
     * This method is useful for synchronization with external systems.
     *
     * @param timestamp the timestamp to compare against
     * @return List of product variants updated since the given timestamp
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.updatedAt >= :timestamp")
    List<ProductVariant> findVariantsUpdatedSince(@Param("timestamp") java.util.Date timestamp);

    /**
     * Calculate sales performance metrics for variants of a specific product.
     * This demonstrates a complex query with aggregation.
     *
     * @param productId the ID of the product to calculate metrics for
     * @return List of arrays containing [variantId, totalSales, averageRating]
     */
    @Query(value = 
           "SELECT pv.id, " +
           "COALESCE(SUM(oi.quantity), 0) as totalSales, " +
           "COALESCE(AVG(r.rating), 0) as averageRating " +
           "FROM product_variant pv " +
           "LEFT JOIN order_item oi ON pv.id = oi.variant_id " +
           "LEFT JOIN product_review r ON pv.product_id = r.product_id " +
           "WHERE pv.product_id = :productId " +
           "GROUP BY pv.id", 
           nativeQuery = true)
    List<Object[]> getVariantSalesMetrics(@Param("productId") Long productId);
    
    /**
     * Update the stock quantity for a specific variant.
     * This method demonstrates a modifying query operation.
     *
     * @param variantId the ID of the variant to update
     * @param quantity the new stock quantity
     * @return the number of records updated
     */
    @Query("UPDATE ProductVariant pv SET pv.stockQuantity = :quantity WHERE pv.id = :variantId")
    int updateStockQuantity(@Param("variantId") Long variantId, @Param("quantity") Integer quantity);
    
    // TODO: Add method to find variants with conflicting SKUs across different products
    
    // FIXME: The findByColorAndSizeAndProductId query needs optimization for large datasets
}