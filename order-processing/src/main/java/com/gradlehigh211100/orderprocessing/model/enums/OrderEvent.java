package com.gradlehigh211100.orderprocessing.model.enums;

/**
 * Enumeration defining all possible events that can trigger state transitions 
 * in the order state machine.
 * 
 * Each event represents a specific action that can be performed on an order
 * during its lifecycle, from creation to delivery or cancellation.
 * 
 * @since 1.0
 */
public enum OrderEvent {
    
    /**
     * Event triggered when creating a new order.
     * This is the initial event that starts the order lifecycle.
     */
    CREATE("Order Creation", "Creates a new order in the system"),
    
    /**
     * Event triggered to validate an order.
     * Validates order details including customer information and product availability.
     */
    VALIDATE("Order Validation", "Validates order details and constraints"),
    
    /**
     * Event triggered to process payment for an order.
     * Handles payment processing through integrated payment gateways.
     */
    PROCESS_PAYMENT("Payment Processing", "Processes payment for the order"),
    
    /**
     * Event triggered to reserve inventory for an order.
     * Ensures products are available in stock and reserves them.
     */
    RESERVE_INVENTORY("Inventory Reservation", "Reserves inventory items for the order"),
    
    /**
     * Event triggered when the order is shipped.
     * Updates order status and initiates shipping process.
     */
    SHIP("Order Shipment", "Marks order as shipped and initiates shipping process"),
    
    /**
     * Event triggered when the order is delivered.
     * Finalizes the order process with successful delivery.
     */
    DELIVER("Order Delivery", "Marks order as delivered to customer"),
    
    /**
     * Event triggered to cancel an order.
     * Cancels the order at any point in the workflow if needed.
     */
    CANCEL("Order Cancellation", "Cancels the order and stops further processing"),
    
    /**
     * Event triggered to refund an order.
     * Processes refund for cancelled or returned orders.
     */
    REFUND("Order Refund", "Processes refund for the order");
    
    // Instance variables for additional event metadata
    private final String displayName;
    private final String description;
    private final long createdTimestamp;
    
    /**
     * Private constructor for OrderEvent enum.
     * 
     * @param displayName Human-readable name for the event
     * @param description Detailed description of the event's purpose
     */
    OrderEvent(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
        this.createdTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the display name of this event.
     * 
     * @return The human-readable name for the event
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of this event.
     * 
     * @return The detailed description of the event
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the timestamp when this enum instance was created.
     * 
     * @return The creation timestamp
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    /**
     * Checks if this event is related to order fulfillment.
     * 
     * @return true if the event is part of the fulfillment process
     */
    public boolean isFulfillmentEvent() {
        switch (this) {
            case SHIP:
            case DELIVER:
            case RESERVE_INVENTORY:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this event is related to financial operations.
     * 
     * @return true if the event involves financial transactions
     */
    public boolean isFinancialEvent() {
        switch (this) {
            case PROCESS_PAYMENT:
            case REFUND:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Determines if this event can occur after another specified event.
     * Implements basic event sequence validation logic.
     * 
     * @param previousEvent The event that occurred before
     * @return true if this event can validly follow the previous event
     */
    public boolean canFollowAfter(OrderEvent previousEvent) {
        if (previousEvent == null) {
            return this == CREATE;
        }
        
        switch (previousEvent) {
            case CREATE:
                return this == VALIDATE || this == CANCEL;
            case VALIDATE:
                return this == PROCESS_PAYMENT || this == CANCEL;
            case PROCESS_PAYMENT:
                return this == RESERVE_INVENTORY || this == REFUND || this == CANCEL;
            case RESERVE_INVENTORY:
                return this == SHIP || this == CANCEL;
            case SHIP:
                return this == DELIVER || this == CANCEL;
            case DELIVER:
                return this == REFUND;
            case CANCEL:
                return this == REFUND;
            case REFUND:
                return false; // No events should follow a refund
            default:
                // FIXME: This default case should never be reached, improve error handling
                return false;
        }
    }
    
    /**
     * Returns a String representation of the OrderEvent including additional metadata.
     * 
     * @return String representation of the event
     */
    @Override
    public String toString() {
        return String.format("%s (%s): %s", 
                             name(),
                             displayName, 
                             description);
    }
    
    /**
     * Parses a string to get the corresponding OrderEvent, with case-insensitive matching.
     * 
     * @param eventName The name of the event to parse
     * @return The matching OrderEvent or null if no match
     */
    public static OrderEvent fromString(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return null;
        }
        
        try {
            // Try exact match first
            return valueOf(eventName);
        } catch (IllegalArgumentException e) {
            // Try case-insensitive match
            for (OrderEvent event : values()) {
                if (event.name().equalsIgnoreCase(eventName)) {
                    return event;
                }
            }
            
            // Try matching display name
            for (OrderEvent event : values()) {
                if (event.getDisplayName().equalsIgnoreCase(eventName)) {
                    return event;
                }
            }
            
            // TODO: Consider adding fuzzy matching for more resilient event lookup
            return null;
        }
    }
    
    /**
     * Gets an array of events that can validly follow the specified event.
     * 
     * @param currentEvent The current event
     * @return Array of valid next events
     */
    public static OrderEvent[] getValidNextEvents(OrderEvent currentEvent) {
        if (currentEvent == null) {
            return new OrderEvent[] { CREATE };
        }
        
        switch (currentEvent) {
            case CREATE:
                return new OrderEvent[] { VALIDATE, CANCEL };
            case VALIDATE:
                return new OrderEvent[] { PROCESS_PAYMENT, CANCEL };
            case PROCESS_PAYMENT:
                return new OrderEvent[] { RESERVE_INVENTORY, REFUND, CANCEL };
            case RESERVE_INVENTORY:
                return new OrderEvent[] { SHIP, CANCEL };
            case SHIP:
                return new OrderEvent[] { DELIVER, CANCEL };
            case DELIVER:
                return new OrderEvent[] { REFUND };
            case CANCEL:
                return new OrderEvent[] { REFUND };
            case REFUND:
                return new OrderEvent[0]; // No events should follow a refund
            default:
                // FIXME: Handle this case better in production code
                return new OrderEvent[0];
        }
    }
}