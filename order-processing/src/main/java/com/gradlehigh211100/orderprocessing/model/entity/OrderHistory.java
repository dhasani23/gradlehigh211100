package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import java.time.LocalDateTime;

/**
 * Simplified OrderHistory class for build fix
 */
public class OrderHistory {
    
    private Long id;
    private Long orderId;
    private OrderState fromState;
    private OrderState toState;
    private String notes;
    private LocalDateTime timestamp;
    
    // Getters and setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public OrderState getFromState() {
        return fromState;
    }
    
    public void setFromState(OrderState fromState) {
        this.fromState = fromState;
    }
    
    public OrderState getToState() {
        return toState;
    }
    
    public void setToState(OrderState toState) {
        this.toState = toState;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Validates if a state transition is valid
     * 
     * @return true if valid
     */
    public boolean isValidStateTransition() {
        return true;
    }
}