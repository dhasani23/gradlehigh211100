package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import java.time.LocalDateTime;

/**
 * Simplified Order entity class to fix build
 */
public class Order {
    
    private Long id;
    private String status;
    private LocalDateTime lastUpdated;
    private OrderState orderState;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public OrderState getOrderState() {
        return orderState;
    }
    
    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }
    
    /**
     * Updates the order state
     *
     * @param newState The new state
     * @param reason The reason for the change
     * @param changedBy Who changed it
     */
    public void updateState(OrderState newState, String reason, String changedBy) {
        OrderState oldState = this.orderState;
        this.orderState = newState;
        this.lastUpdated = LocalDateTime.now();
        
        // In a real implementation, we would create a history record here
    }
}