package com.gradlehigh211100.orderprocessing.model;

import com.gradlehigh211100.orderprocessing.model.enums.OrderEvent;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the history of state transitions for an order.
 */
@Entity
@Table(name = "order_history")
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState previousState;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState currentState;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderEvent event;
    
    @Column(nullable = false)
    private LocalDateTime transitionTimestamp;
    
    @Column(nullable = false)
    private String transitionPerformedBy;
    
    @Column
    private Boolean approved;
    
    @Column
    private String approvedBy;
    
    @Column(length = 1000)
    private String approvalNotes;
    
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

    public OrderState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(OrderState previousState) {
        this.previousState = previousState;
    }

    public OrderState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(OrderState currentState) {
        this.currentState = currentState;
    }

    public OrderEvent getEvent() {
        return event;
    }

    public void setEvent(OrderEvent event) {
        this.event = event;
    }

    public LocalDateTime getTransitionTimestamp() {
        return transitionTimestamp;
    }

    public void setTransitionTimestamp(LocalDateTime transitionTimestamp) {
        this.transitionTimestamp = transitionTimestamp;
    }

    public String getTransitionPerformedBy() {
        return transitionPerformedBy;
    }

    public void setTransitionPerformedBy(String transitionPerformedBy) {
        this.transitionPerformedBy = transitionPerformedBy;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    @Override
    public String toString() {
        return "OrderHistory{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", previousState=" + previousState +
                ", currentState=" + currentState +
                ", event=" + event +
                ", transitionTimestamp=" + transitionTimestamp +
                '}';
    }
}