package com.gradlehigh211100.orderprocessing.service;

import com.gradlehigh211100.orderprocessing.model.Order;

/**
 * Service interface for order operations.
 */
public interface OrderService {

    /**
     * Gets an order by ID
     *
     * @param orderId ID of the order
     * @return The order
     */
    Order getOrderById(Long orderId);
}