package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Entity tracking the complete history and state transitions of orders including
 * timestamps, state changes, and audit information.
 * 
 * This class provides a comprehensive audit trail for order state transitions,
 * capturing metadata such as who made the change, when it occurred, and why.
 */
@Entity
@Table(name = "order_history")
public class OrderHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", nullable = false)
    private OrderState previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false)
    private OrderState newState;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "state_change_date", nullable = false)
    private Date stateChangeDate;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "notes", length = 2000)
    private String notes;

    /**
     * Default constructor required by JPA
     */
    public OrderHistory() {
        // Default constructor required by JPA
    }

    /**
     * Convenience constructor for quickly creating order history entries
     *
     * @param order         the order being tracked
     * @param previousState the previous order state
     * @param newState      the new order state
     * @param changeReason  the reason for the state change
     * @param changedBy     identifier of user or system making the change
     */
    public OrderHistory(Order order, OrderState previousState, OrderState newState, 
                        String changeReason, String changedBy) {
        this.order = order;
        this.previousState = previousState;
        this.newState = newState;
        this.stateChangeDate = new Date();
        this.changeReason = changeReason;
        this.changedBy = changedBy;
    }

    /**
     * Records a state change in order history.
     * This method creates a historical record of an order's state transition.
     * 
     * @param order     the order whose state is changing
     * @param fromState the previous order state
     * @param toState   the new order state
     * @param reason    the reason for the state change
     * @throws IllegalArgumentException if order is null or states are invalid
     */
    public void recordStateChange(Order order, OrderState fromState, OrderState toState, String reason) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        if (fromState == null || toState == null) {
            throw new IllegalArgumentException("Order states cannot be null");
        }
        
        if (fromState == toState) {
            // FIXME: Consider if we should allow recording history when state doesn't change
            throw new IllegalArgumentException("Cannot record state change when states are the same");
        }
        
        // Validate state transition is allowed
        if (!isValidStateTransition(fromState, toState)) {
            throw new IllegalArgumentException(
                    "Invalid state transition from " + fromState + " to " + toState);
        }
        
        this.order = order;
        this.previousState = fromState;
        this.newState = toState;
        this.stateChangeDate = new Date();
        this.changeReason = reason;
        
        // TODO: Implement user context to get current user instead of hardcoding
        this.changedBy = "SYSTEM";
    }

    /**
     * Calculates the duration since the state change occurred in milliseconds.
     *
     * @return the time in milliseconds since the state change
     * @throws IllegalStateException if stateChangeDate is null
     */
    public long getStateChangeDuration() {
        if (stateChangeDate == null) {
            throw new IllegalStateException("State change date is not set");
        }
        
        return System.currentTimeMillis() - stateChangeDate.getTime();
    }
    
    /**
     * Gets the duration since state change in a specific time unit
     *
     * @param timeUnit the time unit for the result
     * @return the duration in the specified time unit
     */
    public long getStateChangeDuration(TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        
        return timeUnit.convert(getStateChangeDuration(), TimeUnit.MILLISECONDS);
    }

    /**
     * Validates if the state transition from one state to another is allowed.
     * This implements complex business rules for order state transitions.
     * 
     * @param fromState the original state
     * @param toState the target state
     * @return true if the transition is allowed, false otherwise
     */
    private boolean isValidStateTransition(OrderState fromState, OrderState toState) {
        // Complex state transition validation logic
        switch (fromState) {
            case NEW:
                // From NEW state, can only go to PENDING, PROCESSING, or CANCELLED
                return toState == OrderState.PENDING || 
                       toState == OrderState.PROCESSING ||
                       toState == OrderState.CANCELLED;
                       
            case PENDING:
                // From PENDING, can go to PROCESSING, PAYMENT_REQUIRED, or CANCELLED
                return toState == OrderState.PROCESSING || 
                       toState == OrderState.PAYMENT_REQUIRED ||
                       toState == OrderState.CANCELLED;
                       
            case PROCESSING:
                // From PROCESSING, can go to SHIPPED, ON_HOLD, or CANCELLED
                return toState == OrderState.SHIPPED || 
                       toState == OrderState.ON_HOLD ||
                       toState == OrderState.CANCELLED;
                       
            case PAYMENT_REQUIRED:
                // From PAYMENT_REQUIRED, can only go to PENDING or CANCELLED
                return toState == OrderState.PENDING || 
                       toState == OrderState.CANCELLED;
                       
            case SHIPPED:
                // From SHIPPED, can only go to DELIVERED or RETURNED
                return toState == OrderState.DELIVERED || 
                       toState == OrderState.RETURNED;
                       
            case ON_HOLD:
                // From ON_HOLD, can go back to PROCESSING or to CANCELLED
                return toState == OrderState.PROCESSING || 
                       toState == OrderState.CANCELLED;
                       
            case DELIVERED:
                // From DELIVERED, can only go to RETURNED or COMPLETED
                return toState == OrderState.RETURNED || 
                       toState == OrderState.COMPLETED;
                       
            case RETURNED:
                // From RETURNED, can only go to REFUNDED or CANCELLED
                return toState == OrderState.REFUNDED || 
                       toState == OrderState.CANCELLED;
                       
            case REFUNDED:
                // From REFUNDED, can only go to COMPLETED
                return toState == OrderState.COMPLETED;
                
            case COMPLETED:
            case CANCELLED:
                // Terminal states - no further transitions allowed
                return false;
                
            default:
                // Unknown state
                return false;
        }
    }

    /**
     * Checks if the state change indicates a critical business event
     * that might require special attention or notifications.
     *
     * @return true if this is a critical state change
     */
    public boolean isCriticalStateChange() {
        // Complex business logic to determine critical state changes
        
        // Cancellation after processing has begun is critical
        if (newState == OrderState.CANCELLED && 
           (previousState == OrderState.PROCESSING || 
            previousState == OrderState.SHIPPED)) {
            return true;
        }
        
        // Returns are always critical
        if (newState == OrderState.RETURNED) {
            return true;
        }
        
        // Orders put on hold are critical
        if (newState == OrderState.ON_HOLD) {
            return true;
        }
        
        // Check for any other business-critical transitions
        if (isHighValueOrder() && (newState == OrderState.CANCELLED || 
                                   newState == OrderState.REFUNDED)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Determines if this is a high-value order that requires special handling
     * 
     * @return true if the order is considered high value
     */
    private boolean isHighValueOrder() {
        // TODO: Implement high value determination logic
        // This would typically check the order total against a threshold
        try {
            // Complex business logic with multiple conditions
            if (order != null && order.getOrderTotal() != null) {
                double threshold = calculateDynamicThreshold();
                return order.getOrderTotal().doubleValue() > threshold;
            }
        } catch (Exception e) {
            // FIXME: Proper exception handling needed
            return false;
        }
        
        return false;
    }
    
    /**
     * Calculates a dynamic threshold for high-value orders based on
     * various business factors.
     *
     * @return the calculated threshold value
     */
    private double calculateDynamicThreshold() {
        // This is a placeholder for complex business logic
        double baseThreshold = 1000.0;
        
        // Apply various business rules to adjust the threshold
        if (order.getCustomer().isVip()) {
            baseThreshold *= 1.5;
        }
        
        // Adjust threshold based on product category
        if (order.hasCategory("ELECTRONICS")) {
            baseThreshold *= 1.2;
        } else if (order.hasCategory("CLOTHING")) {
            baseThreshold *= 0.8;
        }
        
        // Further adjustment based on geographic location
        String region = order.getShippingAddress().getRegion();
        if ("INTERNATIONAL".equals(region)) {
            baseThreshold *= 1.3;
        }
        
        return baseThreshold;
    }

    // Getters and Setters
    
    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(OrderState previousState) {
        this.previousState = previousState;
    }

    public OrderState getNewState() {
        return newState;
    }

    public void setNewState(OrderState newState) {
        this.newState = newState;
    }

    public Date getStateChangeDate() {
        return stateChangeDate;
    }

    public void setStateChangeDate(Date stateChangeDate) {
        this.stateChangeDate = stateChangeDate;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        OrderHistory that = (OrderHistory) o;
        
        return Objects.equals(getId(), that.getId()) &&
               Objects.equals(order, that.order) &&
               previousState == that.previousState &&
               newState == that.newState &&
               Objects.equals(stateChangeDate, that.stateChangeDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), order, previousState, newState, stateChangeDate);
    }

    @Override
    public String toString() {
        return "OrderHistory{" +
                "id=" + getId() +
                ", order=" + (order != null ? order.getId() : "null") +
                ", previousState=" + previousState +
                ", newState=" + newState +
                ", stateChangeDate=" + stateChangeDate +
                ", changedBy='" + changedBy + '\'' +
                '}';
    }
}