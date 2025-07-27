package com.gradlehigh211100.orderprocessing.model.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * OrderState represents the lifecycle states of an order within the order processing system.
 * This enum defines all possible states an order can be in during its lifecycle.
 * 
 * The state transitions follow a specific workflow pattern but allow for exceptional paths
 * such as cancellation or refund which can occur at various points.
 */
public enum OrderState {
    
    /**
     * Initial state when an order is first created in the system.
     * Orders in this state have not been checked for validity.
     */
    PENDING("Pending", 0) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == VALIDATED || nextState == CANCELLED;
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Arrays.asList(VALIDATED, CANCELLED);
        }
    },
    
    /**
     * Order has passed all validation checks and is ready for payment processing.
     * Data integrity and business rule validations have been completed.
     */
    VALIDATED("Validated", 1) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == PAYMENT_PROCESSING || nextState == CANCELLED;
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Arrays.asList(PAYMENT_PROCESSING, CANCELLED);
        }
    },
    
    /**
     * Payment for the order is currently being processed.
     * The system is waiting for confirmation from a payment gateway or processor.
     */
    PAYMENT_PROCESSING("Payment Processing", 2) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == PAID || nextState == CANCELLED;
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Arrays.asList(PAID, CANCELLED);
        }
    },
    
    /**
     * Payment has been successfully processed and confirmed.
     * The order is now financially secured.
     */
    PAID("Paid", 3) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == INVENTORY_RESERVED || nextState == CANCELLED || nextState == REFUNDED;
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Arrays.asList(INVENTORY_RESERVED, CANCELLED, REFUNDED);
        }
        
        @Override
        public void executeStateSpecificLogic() {
            // FIXME: This payment confirmation logic sometimes fails to send confirmation emails
            logStateChange("Payment confirmed");
            
            // Trigger payment confirmation notification
            if (new Random().nextInt(100) > 95) {
                // Simulate occasional failure in payment gateway callback
                logStateChange("WARNING: Failed to process payment gateway callback");
            }
        }
    },
    
    /**
     * Inventory has been checked and reserved for this order.
     * Products cannot be sold to other customers now.
     */
    INVENTORY_RESERVED("Inventory Reserved", 4) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == SHIPPED || nextState == CANCELLED || nextState == REFUNDED;
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Arrays.asList(SHIPPED, CANCELLED, REFUNDED);
        }
        
        @Override
        public void executeStateSpecificLogic() {
            // TODO: Implement warehouse synchronization to prevent inventory conflicts
            logStateChange("Inventory has been reserved");
            
            // Check if any items are on backorder
            if (new Random().nextInt(100) > 80) {
                logStateChange("Some items may be on backorder - requires manual verification");
            }
        }
    },
    
    /**
     * Order has been packaged and shipped to the customer.
     * A shipping carrier has taken possession of the package.
     */
    SHIPPED("Shipped", 5) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == DELIVERED || nextState == CANCELLED || nextState == REFUNDED;
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Arrays.asList(DELIVERED, CANCELLED, REFUNDED);
        }
        
        @Override
        public void executeStateSpecificLogic() {
            logStateChange("Order shipped to customer");
            
            // Generate tracking information
            String trackingId = generateTrackingId();
            // FIXME: Tracking ID sometimes contains invalid characters for certain carriers
            
            // Update shipping status in external systems
            try {
                updateExternalShippingStatus(trackingId);
            } catch (Exception ex) {
                logStateChange("Failed to update external shipping status: " + ex.getMessage());
            }
        }
        
        private String generateTrackingId() {
            // Complex tracking ID generation logic
            StringBuilder trackingId = new StringBuilder();
            Random random = new Random();
            
            // Carrier prefix
            String[] carriers = {"UPS", "FDX", "DHL", "USPS"};
            trackingId.append(carriers[random.nextInt(carriers.length)]);
            trackingId.append("-");
            
            // Numeric part with checksum
            int checksum = 0;
            for (int i = 0; i < 10; i++) {
                int digit = random.nextInt(10);
                trackingId.append(digit);
                checksum += i % 2 == 0 ? digit : digit * 3;
            }
            
            trackingId.append((checksum % 10 == 0) ? 0 : 10 - (checksum % 10));
            
            return trackingId.toString();
        }
        
        private void updateExternalShippingStatus(String trackingId) {
            // Simulated external API call
            if (trackingId == null || trackingId.isEmpty()) {
                throw new IllegalArgumentException("Tracking ID cannot be null or empty");
            }
            
            logStateChange("Updated external shipping system with tracking ID: " + trackingId);
        }
    },
    
    /**
     * Order has been successfully delivered to the customer.
     * This is typically the final state for successful orders.
     */
    DELIVERED("Delivered", 6) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return nextState == REFUNDED; // Once delivered, can only be refunded
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Collections.singletonList(REFUNDED);
        }
        
        @Override
        public void executeStateSpecificLogic() {
            logStateChange("Order confirmed delivered to customer");
            
            // Check if delivery confirmation is required
            if (isSignatureRequired()) {
                // TODO: Implement digital signature verification system
                logStateChange("Delivery confirmation signature received");
            }
            
            // Start return eligibility window
            calculateReturnEligibilityWindow();
        }
        
        private boolean isSignatureRequired() {
            return new Random().nextBoolean(); // Simplified logic
        }
        
        private void calculateReturnEligibilityWindow() {
            // Complex business logic for determining return window based on
            // product types, customer location, and regulatory requirements
            int returnWindowDays = 14; // Default return window
            
            // TODO: Adjust return window based on product categories and local regulations
            logStateChange("Return window set for " + returnWindowDays + " days");
        }
    },
    
    /**
     * Order has been cancelled and will not be processed further.
     * Cancellation can occur at multiple points in the order lifecycle.
     */
    CANCELLED("Cancelled", 7) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return false; // Terminal state, no further transitions allowed
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Collections.emptyList();
        }
        
        @Override
        public void executeStateSpecificLogic() {
            logStateChange("Order has been cancelled");
            
            // Release inventory if it was reserved
            releaseInventoryIfReserved();
            
            // Process refund if payment was collected
            processRefundIfNeeded();
        }
        
        private void releaseInventoryIfReserved() {
            // Complex logic to determine if inventory needs to be released
            boolean wasInventoryReserved = false; // This would be determined dynamically
            
            if (wasInventoryReserved) {
                // Release inventory back to available stock
                logStateChange("Reserved inventory has been released");
            }
        }
        
        private void processRefundIfNeeded() {
            // Determine if payment was processed and needs refunding
            boolean wasPaymentProcessed = false; // This would be determined dynamically
            
            if (wasPaymentProcessed) {
                // Trigger refund process
                logStateChange("Payment refund has been initiated");
            }
        }
    },
    
    /**
     * Order has been refunded, either partially or fully.
     * This is typically a terminal state in the order lifecycle.
     */
    REFUNDED("Refunded", 8) {
        @Override
        public boolean canTransitionTo(OrderState nextState) {
            return false; // Terminal state, no further transitions allowed
        }
        
        @Override
        public List<OrderState> getPossibleNextStates() {
            return Collections.emptyList();
        }
        
        @Override
        public void executeStateSpecificLogic() {
            logStateChange("Order has been refunded");
            
            // Process return if applicable
            processReturnIfApplicable();
            
            // Update inventory if items returned
            updateInventoryForReturn();
        }
        
        private void processReturnIfApplicable() {
            // Check if physical items were returned
            boolean physicalReturn = false; // This would be determined dynamically
            
            if (physicalReturn) {
                // Handle return logistics
                logStateChange("Return shipment processing initiated");
                
                // TODO: Implement return quality control inspection workflow
            }
        }
        
        private void updateInventoryForReturn() {
            // Only update inventory if items are returned in resellable condition
            boolean resellableCondition = false; // This would be determined dynamically
            
            if (resellableCondition) {
                logStateChange("Returned items added back to inventory");
            } else {
                logStateChange("Returned items marked for disposal/recycling");
            }
        }
    };
    
    // Private instance variables
    private final String displayName;
    private final int orderIndex;
    private static final Map<String, OrderState> displayNameMap = new HashMap<>();
    
    // Static initialization for lookups
    static {
        for (OrderState state : OrderState.values()) {
            displayNameMap.put(state.getDisplayName().toUpperCase(), state);
        }
    }
    
    /**
     * Constructor for the OrderState enum.
     * 
     * @param displayName Human-readable display name for this state
     * @param orderIndex Sequential index representing the normal flow order
     */
    OrderState(String displayName, int orderIndex) {
        this.displayName = displayName;
        this.orderIndex = orderIndex;
    }
    
    /**
     * Gets the human-readable display name for this order state.
     * 
     * @return Display name for UI presentation
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the sequential index of this state in normal order flow.
     * 
     * @return The order index value
     */
    public int getOrderIndex() {
        return orderIndex;
    }
    
    /**
     * Checks if a transition from this state to the specified next state is valid.
     * 
     * @param nextState The target state to transition to
     * @return true if transition is allowed, false otherwise
     */
    public abstract boolean canTransitionTo(OrderState nextState);
    
    /**
     * Gets a list of all possible next states from the current state.
     * 
     * @return List of valid next states
     */
    public abstract List<OrderState> getPossibleNextStates();
    
    /**
     * Executes state-specific business logic when entering this state.
     * Default implementation does nothing, but can be overridden by specific states.
     */
    public void executeStateSpecificLogic() {
        // Default implementation does nothing
    }
    
    /**
     * Finds an OrderState by its display name (case-insensitive).
     * 
     * @param displayName The display name to search for
     * @return The matching OrderState, or empty if not found
     */
    public static Optional<OrderState> fromDisplayName(String displayName) {
        if (displayName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(displayNameMap.get(displayName.toUpperCase()));
    }
    
    /**
     * Gets all order states that are considered "active" (not terminal states).
     * 
     * @return List of active order states
     */
    public static List<OrderState> getActiveStates() {
        return Arrays.stream(values())
                .filter(state -> !state.equals(CANCELLED) && !state.equals(REFUNDED))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all order states that are considered "terminal" (end states).
     * 
     * @return List of terminal order states
     */
    public static List<OrderState> getTerminalStates() {
        List<OrderState> terminals = new ArrayList<>();
        terminals.add(DELIVERED);
        terminals.add(CANCELLED);
        terminals.add(REFUNDED);
        return terminals;
    }
    
    /**
     * Determines if this state is a terminal state (no further transitions possible).
     * 
     * @return true if this is a terminal state, false otherwise
     */
    public boolean isTerminalState() {
        return getPossibleNextStates().isEmpty();
    }
    
    /**
     * Determines if this state represents a problematic state (cancelled or refunded).
     * 
     * @return true if this is a problematic state, false otherwise
     */
    public boolean isProblematicState() {
        return this == CANCELLED || this == REFUNDED;
    }
    
    /**
     * Helper method to log state changes with consistent formatting.
     * 
     * @param message The message to log
     */
    protected void logStateChange(String message) {
        String logEntry = String.format("[%s] %s: %s", 
                this.name(), 
                java.time.LocalDateTime.now().toString(), 
                message);
        
        // In a real implementation, this would use a logging framework
        System.out.println(logEntry);
    }
}