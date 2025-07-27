package com.gradlehigh211100.productcatalog.service;

import com.gradlehigh211100.productcatalog.model.InventoryDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service interface for inventory management operations
 */
public interface InventoryService {
    
    /**
     * Update product stock quantity
     * 
     * @param productId Product ID
     * @param quantity New quantity
     * @return True if update was successful
     */
    boolean updateProductStock(Long productId, Integer quantity);
    
    /**
     * Get product stock quantity
     * 
     * @param productId Product ID
     * @return Current stock quantity
     */
    int getProductStock(Long productId);
    
    /**
     * Get variant stock quantity
     * 
     * @param productId Product ID
     * @param variantId Variant ID
     * @return Current variant stock quantity
     */
    int getVariantStock(Long productId, Long variantId);
    
    /**
     * Get quantity of reserved stock
     * 
     * @param productId Product ID
     * @param variantId Variant ID
     * @return Current reserved quantity
     */
    int getReservedStock(Long productId, Long variantId);
    
    /**
     * Check if product exists
     * 
     * @param productId Product ID
     * @return True if product exists
     */
    boolean productExists(Long productId);
    
    /**
     * Check if variant exists
     * 
     * @param productId Product ID
     * @param variantId Variant ID
     * @return True if variant exists
     */
    boolean variantExists(Long productId, Long variantId);
    
    /**
     * Reserve stock for a product variant
     * 
     * @param productId Product ID
     * @param variantId Variant ID
     * @param quantity Quantity to reserve
     * @return True if reservation was successful
     */
    boolean reserveStock(Long productId, Long variantId, Integer quantity);
    
    /**
     * Release previously reserved stock
     * 
     * @param productId Product ID
     * @param variantId Variant ID
     * @param quantity Quantity to release
     * @return True if release was successful
     */
    boolean releaseReservedStock(Long productId, Long variantId, Integer quantity);
    
    /**
     * Get list of items with low stock
     * 
     * @param categoryThresholds Map of thresholds by category
     * @return List of inventory items with low stock
     */
    List<InventoryDTO> getLowStockItems(Map<String, Integer> categoryThresholds);
    
    /**
     * Get product category
     * 
     * @param productId Product ID
     * @return Product category name
     */
    String getProductCategory(Long productId);
    
    /**
     * Update stock for perishable products
     * 
     * @param productId Product ID
     * @param quantity New quantity
     * @param expirationDate Expiration date
     * @return True if update was successful
     */
    boolean updatePerishableProductStock(Long productId, Integer quantity, Date expirationDate);
    
    /**
     * Update stock for hazardous products
     * 
     * @param productId Product ID
     * @param quantity New quantity
     * @param safetyInformation Safety information
     * @return True if update was successful
     */
    boolean updateHazardousProductStock(Long productId, Integer quantity, Map<String, String> safetyInformation);
}