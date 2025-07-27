package com.gradlehigh211100.orderprocessing.repository;

import com.gradlehigh211100.orderprocessing.model.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for shopping cart data access including cart persistence,
 * retrieval, and cleanup operations.
 * 
 * This repository handles all database operations related to shopping carts,
 * including finding carts by various criteria and performing maintenance
 * operations like cleanup of abandoned carts.
 * 
 * @since 1.0
 */
@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    /**
     * Finds a shopping cart for a specific user.
     * 
     * @param userId The ID of the user whose cart to find
     * @return Optional containing the shopping cart if found, empty otherwise
     */
    Optional<ShoppingCart> findByUserId(Long userId);
    
    /**
     * Finds a shopping cart associated with a specific session.
     * This is particularly useful for guests or users who haven't logged in yet.
     * 
     * @param sessionId The session identifier
     * @return Optional containing the shopping cart if found, empty otherwise
     */
    Optional<ShoppingCart> findBySessionId(String sessionId);
    
    /**
     * Finds an active shopping cart for a specific user.
     * Users may have multiple carts but only one should be active at a time.
     * 
     * @param userId The ID of the user whose cart to find
     * @param isActive Flag indicating if the cart is active
     * @return Optional containing the active shopping cart if found, empty otherwise
     */
    Optional<ShoppingCart> findByUserIdAndIsActive(Long userId, Boolean isActive);
    
    /**
     * Finds all shopping carts that haven't been modified since a specified date.
     * This method is used to identify abandoned carts for cleanup or marketing purposes.
     * 
     * FIXME: Consider adding pagination for large datasets to prevent memory issues
     * 
     * @param date The cutoff date for last modification
     * @return List of abandoned shopping carts
     */
    @Query("SELECT c FROM ShoppingCart c WHERE c.lastModifiedDate < :date")
    List<ShoppingCart> findByLastModifiedDateBefore(@Param("date") Date date);
    
    /**
     * Deletes shopping carts that haven't been modified since a specified date.
     * This is used for database maintenance to remove abandoned carts.
     * 
     * @param date The cutoff date for last modification
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ShoppingCart c WHERE c.lastModifiedDate < :date")
    void deleteByLastModifiedDateBefore(@Param("date") Date date);
    
    /**
     * Finds carts that contain a specific product.
     * This can be used for marketing analytics.
     * 
     * @param productId The ID of the product to search for
     * @return List of shopping carts containing the product
     * 
     * TODO: Implement method that joins cart items to find carts with specific products
     */
    @Query(value = "SELECT sc.* FROM shopping_cart sc " +
           "JOIN shopping_cart_item sci ON sc.id = sci.shopping_cart_id " +
           "WHERE sci.product_id = :productId", nativeQuery = true)
    List<ShoppingCart> findCartsByProductId(@Param("productId") Long productId);
    
    /**
     * Counts the number of active carts in the system.
     * Useful for analytics and monitoring system load.
     * 
     * @param isActive Flag indicating if carts are active
     * @return Count of active carts
     */
    Long countByIsActive(Boolean isActive);
    
    /**
     * Updates the last access time for a cart to prevent it from being 
     * flagged as abandoned prematurely.
     * 
     * @param cartId The ID of the cart to update
     * @param lastAccessDate The new last access date
     * @return Number of rows updated (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ShoppingCart c SET c.lastAccessDate = :lastAccessDate WHERE c.id = :cartId")
    int updateLastAccessDate(@Param("cartId") Long cartId, @Param("lastAccessDate") Date lastAccessDate);
    
    /**
     * Merges an anonymous cart (associated with session) with a user cart 
     * after login.
     * 
     * TODO: Implement complex merge operation that consolidates items from both carts
     * 
     * @param sessionId The session ID with the anonymous cart
     * @param userId The user ID to associate the cart with
     * @return The updated cart ID
     */
    @Modifying
    @Transactional
    @Query(value = "CALL merge_shopping_carts(:sessionId, :userId)", nativeQuery = true)
    Long mergeCartsAfterLogin(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}