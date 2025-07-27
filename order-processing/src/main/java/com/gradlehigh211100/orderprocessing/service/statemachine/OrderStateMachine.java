package com.gradlehigh211100.orderprocessing.service.statemachine;

import com.gradlehigh211100.orderprocessing.model.enums.OrderEvent;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import com.gradlehigh211100.orderprocessing.repository.OrderHistoryRepository;
import com.gradlehigh211100.orderprocessing.repository.OrderRepository;
import com.gradlehigh211100.orderprocessing.model.Order;
import com.gradlehigh211100.orderprocessing.model.OrderHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * State machine service managing order state transitions, validating state changes, 
 * and orchestrating the order workflow process.
 * 
 * This service implements a high-complexity order management workflow with multiple
 * possible state transitions, validation rules, and conditional logic.
 */
@Service
public class OrderStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(OrderStateMachine.class);
    
    private static final String ORDER_ID_HEADER = "order_id";
    
    // The Spring State Machine factory to create state machine instances
    private final StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;
    
    // Repository for order persistence
    private final OrderRepository orderRepository;
    
    // Repository for order history tracking
    private final OrderHistoryRepository orderHistoryRepository;
    
    // Cache to store currently active state machines
    private final java.util.Map<Long, StateMachine<OrderState, OrderEvent>> activeStateMachines;
    
    @Autowired
    public OrderStateMachine(StateMachineFactory<OrderState, OrderEvent> stateMachineFactory,
                           OrderRepository orderRepository,
                           OrderHistoryRepository orderHistoryRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.orderRepository = orderRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.activeStateMachines = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    /**
     * Transitions an order to a new state based on the provided event.
     * 
     * This method handles the complex logic of:
     * 1. Retrieving or creating a state machine for the order
     * 2. Validating if the transition is allowed
     * 3. Sending the event to the state machine
     * 4. Persisting the new state
     * 5. Recording the transition history
     *
     * @param orderId The ID of the order to transition
     * @param event The event triggering the state transition
     * @return boolean True if transition was successful, false otherwise
     */
    @Transactional
    public boolean transitionOrder(Long orderId, OrderEvent event) {
        logger.debug("Attempting to transition order ID {} with event {}", orderId, event);
        
        // Retrieve order from repository
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
                
        OrderState currentState = order.getState();
        
        // Check if transition is valid
        if (!canTransition(currentState, event)) {
            logger.warn("Invalid transition attempt for order {}: {} -> {}", orderId, currentState, event);
            return false;
        }
        
        try {
            // Get or create state machine for this order
            StateMachine<OrderState, OrderEvent> stateMachine = getOrCreateStateMachine(orderId, currentState);
            
            // Send the event to the state machine
            boolean success = sendEventToStateMachine(stateMachine, event, orderId);
            
            if (success) {
                // Get the new state from the state machine
                OrderState newState = stateMachine.getState().getId();
                
                // Record the transition in history
                recordStateTransition(orderId, currentState, newState, event);
                
                // Update order state in database
                order.setState(newState);
                orderRepository.save(order);
                
                logger.info("Order {} successfully transitioned: {} -> {} via event {}", 
                        orderId, currentState, newState, event);
                return true;
            } else {
                logger.error("Failed to transition order {} with event {}", orderId, event);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during order transition process", e);
            throw new RuntimeException("Failed to process order state transition", e);
        }
    }
    
    /**
     * Checks if the given event can trigger a state transition from the current state.
     * Implements complex validation rules based on the current state and the requested event.
     *
     * @param currentState The current state of the order
     * @param event The event to check
     * @return boolean True if transition is valid, false otherwise
     */
    public boolean canTransition(OrderState currentState, OrderEvent event) {
        logger.debug("Validating transition: {} -> {}", currentState, event);
        
        // Complex validation rules with nested conditions
        switch (currentState) {
            case CREATED:
                return event == OrderEvent.VALIDATE || event == OrderEvent.CANCEL;
                
            case VALIDATED:
                return event == OrderEvent.PROCESS_PAYMENT || event == OrderEvent.CANCEL;
                
            case PAYMENT_PROCESSING:
                return event == OrderEvent.PAYMENT_APPROVED || 
                       event == OrderEvent.PAYMENT_DECLINED ||
                       event == OrderEvent.CANCEL;
                       
            case PAYMENT_APPROVED:
                return event == OrderEvent.FULFILL || event == OrderEvent.CANCEL;
                
            case FULFILLING:
                return event == OrderEvent.SHIPPED || 
                       event == OrderEvent.BACKORDER || 
                       event == OrderEvent.CANCEL;
                       
            case BACKORDERED:
                return event == OrderEvent.RESTOCK || 
                       event == OrderEvent.CANCEL || 
                       event == OrderEvent.FULFILL;
                       
            case SHIPPED:
                return event == OrderEvent.DELIVER || event == OrderEvent.RETURN_INITIATED;
                
            case DELIVERED:
                return event == OrderEvent.COMPLETE || event == OrderEvent.RETURN_INITIATED;
                
            case RETURN_INITIATED:
                return event == OrderEvent.RETURN_APPROVED || event == OrderEvent.RETURN_REJECTED;
                
            case RETURN_APPROVED:
                return event == OrderEvent.REFUND_PROCESSED;
                
            case CANCELLED:
            case COMPLETED:
            case REFUNDED:
            case PAYMENT_DECLINED:
            case RETURN_REJECTED:
                // Terminal states - no further transitions allowed
                return false;
                
            default:
                logger.warn("Unknown state encountered: {}", currentState);
                return false;
        }
    }

    /**
     * Gets all possible events that can be triggered from the current state.
     * This method provides a comprehensive list of valid transitions based on 
     * the order's current state.
     *
     * @param currentState The current state of the order
     * @return List<OrderEvent> List of possible events from the current state
     */
    public List<OrderEvent> getPossibleTransitions(OrderState currentState) {
        logger.debug("Getting possible transitions for state: {}", currentState);
        
        List<OrderEvent> possibleEvents = new ArrayList<>();
        
        // For each possible event, check if it's valid for the current state
        for (OrderEvent event : OrderEvent.values()) {
            if (canTransition(currentState, event)) {
                possibleEvents.add(event);
            }
        }
        
        return possibleEvents;
    }

    /**
     * Initializes a state machine with the specified initial state.
     * This method handles the complex configuration of the state machine,
     * including setting up listeners, interceptors and the initial state.
     *
     * @param initialState The initial state for the state machine
     */
    public void initializeStateMachine(OrderState initialState) {
        logger.debug("Initializing state machine with initial state: {}", initialState);
        
        StateMachine<OrderState, OrderEvent> newStateMachine = stateMachineFactory.getStateMachine(UUID.randomUUID());
        
        try {
            newStateMachine.stop();
            
            List<StateMachineAccess<OrderState, OrderEvent>> accessors = newStateMachine.getStateMachineAccessor();
            
            for (StateMachineAccess<OrderState, OrderEvent> accessor : accessors) {
                accessor.resetStateMachine(new DefaultStateMachineContext<>(initialState, null, null, null));
            }
            
            // Configure any additional state machine settings
            configureStateMachineEventListeners(newStateMachine);
            
            newStateMachine.start();
            
            logger.info("State machine initialized successfully with state: {}", initialState);
        } catch (Exception e) {
            logger.error("Failed to initialize state machine", e);
            throw new RuntimeException("State machine initialization failed", e);
        }
    }

    /**
     * Records a state transition in the order history.
     * This method creates a detailed audit trail of all state changes,
     * capturing the before and after states, the triggering event, and timestamp.
     *
     * @param orderId The ID of the order
     * @param fromState The state before transition
     * @param toState The state after transition
     * @param event The event that triggered the transition
     */
    @Transactional
    public void recordStateTransition(Long orderId, OrderState fromState, OrderState toState, OrderEvent event) {
        logger.debug("Recording state transition: Order ID {} from {} to {} via {}", 
                orderId, fromState, toState, event);
        
        try {
            // Ensure the order exists
            orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Cannot record transition for non-existent order: " + orderId));
            
            // Create history record
            OrderHistory history = new OrderHistory();
            history.setOrderId(orderId);
            history.setPreviousState(fromState);
            history.setCurrentState(toState);
            history.setEvent(event);
            history.setTransitionTimestamp(LocalDateTime.now());
            history.setTransitionPerformedBy(getCurrentUser());
            
            // Add additional audit information if needed
            if (requiresApproval(fromState, toState)) {
                history.setApproved(true);
                history.setApprovedBy(getApprover(orderId, event));
                history.setApprovalNotes(generateApprovalNotes(fromState, toState, event));
            }
            
            // Save to repository
            orderHistoryRepository.save(history);
            
            logger.info("State transition recorded successfully for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to record state transition", e);
            throw new RuntimeException("State transition recording failed", e);
        }
    }

    /**
     * Helper method to get or create a state machine for an order.
     * If a state machine already exists for this order, it returns that.
     * Otherwise, it creates a new one with the specified initial state.
     *
     * @param orderId The ID of the order
     * @param currentState The current state of the order
     * @return StateMachine<OrderState, OrderEvent> The state machine for this order
     */
    private StateMachine<OrderState, OrderEvent> getOrCreateStateMachine(Long orderId, OrderState currentState) {
        // Try to get existing state machine from cache
        StateMachine<OrderState, OrderEvent> stateMachine = activeStateMachines.get(orderId);
        
        if (stateMachine == null) {
            // Create new state machine
            stateMachine = stateMachineFactory.getStateMachine(UUID.randomUUID());
            
            // Initialize with current state
            stateMachine.stop();
            
            List<StateMachineAccess<OrderState, OrderEvent>> accessors = stateMachine.getStateMachineAccessor();
            
            for (StateMachineAccess<OrderState, OrderEvent> accessor : accessors) {
                accessor.resetStateMachine(new DefaultStateMachineContext<>(currentState, null, null, null));
            }
            
            // Add listeners, interceptors, etc.
            configureStateMachineEventListeners(stateMachine);
            
            stateMachine.start();
            
            // Cache the state machine for future use
            activeStateMachines.put(orderId, stateMachine);
            
            logger.debug("Created new state machine for order ID: {}", orderId);
        }
        
        return stateMachine;
    }

    /**
     * Sends an event to a state machine with order ID in the header.
     *
     * @param stateMachine The state machine to receive the event
     * @param event The event to send
     * @param orderId The ID of the order for message headers
     * @return boolean True if the event was accepted, false otherwise
     */
    private boolean sendEventToStateMachine(StateMachine<OrderState, OrderEvent> stateMachine, 
                                          OrderEvent event, Long orderId) {
        // Build message with order ID in header
        Message<OrderEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(ORDER_ID_HEADER, orderId)
                .build();
                
        // Send event to state machine and return result
        return stateMachine.sendEvent(message);
    }

    /**
     * Configures listeners for state machine events.
     * Sets up handlers for state change events, transition events, etc.
     *
     * @param stateMachine The state machine to configure
     */
    private void configureStateMachineEventListeners(StateMachine<OrderState, OrderEvent> stateMachine) {
        // Example listener setup - actual implementation would be more complex
        stateMachine.addStateChangeListener(event -> {
            logger.debug("State changed from {} to {}", 
                    event.getSource().getId(), event.getTarget().getId());
        });
    }

    /**
     * Gets the username of the currently logged-in user.
     * Used for audit trail purposes.
     *
     * @return String The username of the current user
     */
    private String getCurrentUser() {
        // FIXME: Implement proper user context retrieval from security context
        return "system"; // Placeholder
    }

    /**
     * Determines if a state transition requires approval.
     *
     * @param fromState The state before transition
     * @param toState The state after transition
     * @return boolean True if approval is required, false otherwise
     */
    private boolean requiresApproval(OrderState fromState, OrderState toState) {
        // Example logic for transitions that require approval
        if (toState == OrderState.CANCELLED && 
                (fromState == OrderState.PAYMENT_APPROVED || 
                 fromState == OrderState.FULFILLING || 
                 fromState == OrderState.SHIPPED)) {
            return true;
        }
        
        if (fromState == OrderState.RETURN_INITIATED && 
                (toState == OrderState.RETURN_APPROVED || 
                 toState == OrderState.RETURN_REJECTED)) {
            return true;
        }
        
        return false;
    }

    /**
     * Gets the username of the person who approved the transition.
     *
     * @param orderId The ID of the order
     * @param event The event that triggered the transition
     * @return String The username of the approver
     */
    private String getApprover(Long orderId, OrderEvent event) {
        // TODO: Implement logic to retrieve actual approver from approval system
        return "manager";
    }

    /**
     * Generates approval notes for a transition.
     *
     * @param fromState The state before transition
     * @param toState The state after transition
     * @param event The event that triggered the transition
     * @return String Notes about the approval
     */
    private String generateApprovalNotes(OrderState fromState, OrderState toState, OrderEvent event) {
        StringBuilder notes = new StringBuilder();
        notes.append("Transition from ").append(fromState).append(" to ").append(toState);
        notes.append(" triggered by ").append(event);
        notes.append(" at ").append(LocalDateTime.now());
        
        // Add conditional notes based on transition type
        if (event == OrderEvent.CANCEL) {
            notes.append(". Cancellation approved after verification.");
        } else if (event == OrderEvent.RETURN_APPROVED) {
            notes.append(". Return approved after inspection.");
        }
        
        return notes.toString();
    }
    
    /**
     * Cleans up resources when an order workflow is completed.
     * Removes the state machine from the cache to free up resources.
     *
     * @param orderId The ID of the order
     */
    public void cleanupOrderStateMachine(Long orderId) {
        activeStateMachines.remove(orderId);
        logger.debug("Cleaned up state machine for order ID: {}", orderId);
    }
    
    /**
     * Gets complex statistical data about state transitions.
     * This method analyzes transition patterns to identify bottlenecks,
     * common paths, and anomalies in the order workflow.
     *
     * @return Map<String, Object> Statistics about state transitions
     */
    public java.util.Map<String, Object> getStateTransitionStatistics() {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        
        try {
            // Calculate average time spent in each state
            statistics.put("averageTimeInStates", calculateAverageTimeInStates());
            
            // Calculate most common transition paths
            statistics.put("commonTransitionPaths", findCommonTransitionPaths());
            
            // Calculate transition failure rates
            statistics.put("transitionFailureRates", calculateTransitionFailureRates());
            
            return statistics;
        } catch (Exception e) {
            logger.error("Failed to calculate state transition statistics", e);
            throw new RuntimeException("Statistics calculation failed", e);
        }
    }
    
    /**
     * Calculates the average time orders spend in each state.
     *
     * @return Map<OrderState, Double> Average time (in minutes) for each state
     */
    private java.util.Map<OrderState, Double> calculateAverageTimeInStates() {
        // TODO: Implement calculation logic using order history data
        return new java.util.HashMap<>();
    }
    
    /**
     * Identifies the most common transition paths through the state machine.
     *
     * @return List<List<OrderState>> Most common transition paths
     */
    private List<List<OrderState>> findCommonTransitionPaths() {
        // TODO: Implement path analysis logic
        return new ArrayList<>();
    }
    
    /**
     * Calculates the failure rate for each type of transition.
     *
     * @return Map<OrderEvent, Double> Failure rate for each event type
     */
    private java.util.Map<OrderEvent, Double> calculateTransitionFailureRates() {
        // TODO: Implement failure rate calculation
        return new java.util.HashMap<>();
    }
}