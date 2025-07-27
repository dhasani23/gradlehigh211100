package com.gradlehigh211100.productcatalog.service;

import org.springframework.stereotype.Service;

/**
 * Service for inventory management
 */
@Service
public class InventoryService {

    /**
     * Gets the available quantity of a product
     *
     * @param productId ID of the product
     * @return Available quantity
     */
    public int getAvailableQuantity(Long productId) {
        return 100;
    }

    /**
     * Reduces the stock of a product
     *
     * @param productId ID of the product
     * @param quantity Quantity to reduce
     * @return true if successful
     */
    public boolean reduceStock(Long productId, Integer quantity) {
        return true;
    }

    /**
     * Increases the stock of a product
     *
     * @param productId ID of the product
     * @param quantity Quantity to increase
     * @return true if successful
     */
    public boolean increaseStock(Long productId, Integer quantity) {
        return true;
    }

    /**
     * Increases the stock of a product
     *
     * @param productId ID of the product
     * @param quantity Quantity to increase
     * @return true if successful
     */
    public boolean increaseStock(Long productId, int quantity) {
        return true;
    }

    /**
     * Marks an order as shipped
     *
     * @param orderId ID of the order
     * @return true if successful
     */
    public boolean markAsShipped(Long orderId) {
        return true;
    }
}