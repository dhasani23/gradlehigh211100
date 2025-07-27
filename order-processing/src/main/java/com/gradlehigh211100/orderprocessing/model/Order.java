package com.gradlehigh211100.orderprocessing.model;

import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import java.time.LocalDateTime;

/**
 * Simplified Order class to fix build
 */
public class Order {
    private Long id;
    private String status;
    private LocalDateTime lastUpdated;
    private OrderState state;

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
    
    public OrderState getState() {
        return state;
    }
    
    public void setState(OrderState state) {
        this.state = state;
    }
}