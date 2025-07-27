package com.gradlehigh211100.orderprocessing.service;

import com.gradlehigh211100.orderprocessing.repository.OrderRepository;
import com.gradlehigh211100.orderprocessing.service.statemachine.OrderStateMachine;
import com.gradlehigh211100.orderprocessing.service.validation.OrderValidationService;
import com.gradlehigh211100.orderprocessing.service.payment.PaymentProcessingService;
import com.gradlehigh211100.orderprocessing.service.inventory.InventoryReservationService;
import com.gradlehigh211100.orderprocessing.service.shipping.ShippingService;
import com.gradlehigh211100.orderprocessing.model.Order;
import com.gradlehigh211100.orderprocessing.model.OrderRequest;
import com.gradlehigh211100.orderprocessing.model.OrderEvent;
import com.gradlehigh211100.orderprocessing.exception.OrderProcessingException;
import com.gradlehigh211100.orderprocessing.exception.ResourceNotFoundException;
import com.gradlehigh211100.orderprocessing.exception.ValidationException;
import com.gradlehigh211100.orderprocessing.exception.PaymentProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main orchestrating service coordinating the entire order processing workflow,
 * managing state transitions, and integrating with all supporting services.
 * 
 * This service has high cyclomatic complexity due to the numerous business rules
 * and conditional paths in order processing.
 */
@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final OrderValidationService validationService;
    private final PaymentProcessingService paymentService;
    private final InventoryReservationService inventoryService;
    private final ShippingService shippingService;
    
    private static final int PROCESSING_TIMEOUT_SECONDS = 30;
    
    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            OrderStateMachine orderStateMachine,
            OrderValidationService validationService,
            PaymentProcessingService paymentService,
            InventoryReservationService inventoryService,
            ShippingService shippingService) {
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
        this.validationService = validationService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.shippingService = shippingService;
    }

    /**
     * Creates a new order from the provided order request.
     * Includes validation of request data before persisting.
     *
     * @param orderRequest the request containing order details
     * @return the created Order entity
     * @throws ValidationException if the order request fails validation
     */
    @Transactional
    public Order createOrder(OrderRequest orderRequest) {
        logger.info("Creating new order from request: {}", orderRequest);
        
        // Validate order request
        if (!validationService.validateOrderRequest(orderRequest)) {
            logger.error("Order request validation failed: {}", orderRequest);
            throw new ValidationException("Order request failed validation checks");
        }
        
        // Check inventory availability before creating order
        if (!inventoryService.checkInventoryAvailability(orderRequest.getItems())) {
            logger.error("Insufficient inventory for order items: {}", orderRequest.getItems());
            throw new OrderProcessingException("One or more items in your order are not available in requested quantity");
        }
        
        try {
            // Create order entity from request
            Order newOrder = convertRequestToOrder(orderRequest);
            
            // Initialize the order state
            orderStateMachine.initializeState(newOrder);
            
            // Save to repository
            Order savedOrder = orderRepository.save(newOrder);
            logger.info("Order created successfully with ID: {}", savedOrder.getId());
            
            return savedOrder;
        } catch (Exception e) {
            logger.error("Error creating order", e);
            throw new OrderProcessingException("Failed to create order: " + e.getMessage(), e);
        }
    }

    /**
     * Processes an order through the complete workflow including payment processing,
     * inventory reservation, and shipping arrangement.
     *
     * @param orderId the ID of the order to process
     * @throws ResourceNotFoundException if order doesn't exist
     * @throws OrderProcessingException for various processing failures
     */
    @Transactional
    public void processOrder(Long orderId) {
        logger.info("Processing order with ID: {}", orderId);
        
        Order order = getOrderOrThrow(orderId);
        
        // Check if order can be processed (based on current state)
        if (!orderStateMachine.canTransition(order, OrderEvent.PROCESS)) {
            logger.error("Cannot process order {} in state {}", orderId, order.getStatus());
            throw new OrderProcessingException("Order cannot be processed in its current state: " + order.getStatus());
        }
        
        // Validate order one more time before processing
        try {
            if (!validationService.validateOrder(order)) {
                orderStateMachine.transition(order, OrderEvent.VALIDATION_FAILED);
                orderRepository.save(order);
                throw new ValidationException("Order failed validation during processing");
            }
            
            // Attempt payment processing with timeout
            CompletableFuture<Boolean> paymentFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return paymentService.processPayment(order);
                } catch (Exception e) {
                    logger.error("Payment processing error", e);
                    return false;
                }
            });
            
            boolean paymentSuccess = false;
            try {
                paymentSuccess = paymentFuture.get(PROCESSING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("Payment processing timeout or error for order: {}", orderId, e);
                orderStateMachine.transition(order, OrderEvent.PAYMENT_FAILED);
                orderRepository.save(order);
                throw new PaymentProcessingException("Payment processing failed: " + e.getMessage());
            }
            
            if (!paymentSuccess) {
                orderStateMachine.transition(order, OrderEvent.PAYMENT_FAILED);
                orderRepository.save(order);
                throw new PaymentProcessingException("Payment declined for order: " + orderId);
            }
            
            // Update order state after successful payment
            orderStateMachine.transition(order, OrderEvent.PAYMENT_COMPLETED);
            orderRepository.save(order);
            
            // Reserve inventory
            if (!inventoryService.reserveInventory(order)) {
                // Roll back payment and update state
                paymentService.refundPayment(order, order.getTotalAmount());
                orderStateMachine.transition(order, OrderEvent.INVENTORY_FAILED);
                orderRepository.save(order);
                throw new OrderProcessingException("Failed to reserve inventory for order: " + orderId);
            }
            
            // Update state after inventory reservation
            orderStateMachine.transition(order, OrderEvent.INVENTORY_RESERVED);
            orderRepository.save(order);
            
            // Arrange shipping
            if (!shippingService.arrangeShipping(order)) {
                // Roll back inventory and payment
                inventoryService.releaseInventory(order);
                paymentService.refundPayment(order, order.getTotalAmount());
                orderStateMachine.transition(order, OrderEvent.SHIPPING_FAILED);
                orderRepository.save(order);
                throw new OrderProcessingException("Failed to arrange shipping for order: " + orderId);
            }
            
            // Finalize order
            orderStateMachine.transition(order, OrderEvent.PROCESSING_COMPLETED);
            order.setLastUpdated(java.time.LocalDateTime.now());
            orderRepository.save(order);
            
            logger.info("Order processing completed successfully for order ID: {}", orderId);
            
        } catch (ValidationException | PaymentProcessingException e) {
            // These exceptions are already handled with state transitions above
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during order processing", e);
            orderStateMachine.transition(order, OrderEvent.PROCESSING_FAILED);
            orderRepository.save(order);
            throw new OrderProcessingException("Order processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels an order with a specific reason.
     * Performs necessary refunds, inventory release, and state updates.
     *
     * @param orderId the ID of the order to cancel
     * @param reason the reason for cancellation
     * @throws ResourceNotFoundException if order doesn't exist
     * @throws OrderProcessingException if cancellation cannot be performed
     */
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        logger.info("Cancelling order ID: {} with reason: {}", orderId, reason);
        
        Order order = getOrderOrThrow(orderId);
        
        // Check if order can be cancelled in its current state
        if (!orderStateMachine.canTransition(order, OrderEvent.CANCEL)) {
            logger.error("Cannot cancel order {} in state {}", orderId, order.getStatus());
            throw new OrderProcessingException("Order cannot be cancelled in its current state: " + order.getStatus());
        }
        
        try {
            // Different cancellation paths based on order state
            switch (order.getStatus()) {
                case PAYMENT_COMPLETED:
                case PAYMENT_PROCESSING:
                    // Need to issue refund
                    paymentService.refundPayment(order, order.getTotalAmount());
                    break;
                
                case INVENTORY_RESERVED:
                    // Need to release inventory and refund
                    inventoryService.releaseInventory(order);
                    paymentService.refundPayment(order, order.getTotalAmount());
                    break;
                
                case SHIPPING_ARRANGED:
                case SHIPPED:
                    // Need to cancel shipping, release inventory, and refund
                    shippingService.cancelShipping(order);
                    inventoryService.releaseInventory(order);
                    paymentService.refundPayment(order, order.getTotalAmount());
                    break;
                
                case DELIVERED:
                    // Cannot cancel delivered orders
                    logger.error("Cannot cancel order {} as it has been delivered", orderId);
                    throw new OrderProcessingException("Cannot cancel order after delivery");
                    
                case CANCELLED:
                case REFUNDED:
                    logger.warn("Order {} is already cancelled or refunded", orderId);
                    return;
                    
                default:
                    // Other states might not need special handling
                    break;
            }
            
            // Update order state and reason
            orderStateMachine.transition(order, OrderEvent.CANCEL);
            order.setCancellationReason(reason);
            order.setLastUpdated(java.time.LocalDateTime.now());
            orderRepository.save(order);
            
            logger.info("Order {} successfully cancelled", orderId);
            
        } catch (Exception e) {
            logger.error("Error cancelling order: {}", orderId, e);
            throw new OrderProcessingException("Failed to cancel order: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the ID of the order to retrieve
     * @return the requested Order
     * @throws ResourceNotFoundException if order doesn't exist
     */
    public Order getOrderById(Long orderId) {
        logger.debug("Fetching order with ID: {}", orderId);
        return getOrderOrThrow(orderId);
    }

    /**
     * Retrieves all orders for a specific customer.
     *
     * @param customerId the ID of the customer
     * @return a list of orders belonging to the customer
     */
    public List<Order> getOrdersByCustomer(Long customerId) {
        logger.debug("Fetching orders for customer ID: {}", customerId);
        
        List<Order> customerOrders = orderRepository.findByCustomerId(customerId);
        
        logger.debug("Found {} orders for customer ID: {}", customerOrders.size(), customerId);
        return customerOrders;
    }

    /**
     * Updates an order's status based on a specific event.
     * This allows external systems to trigger state transitions.
     *
     * @param orderId the ID of the order to update
     * @param event the event that triggers the state transition
     * @throws ResourceNotFoundException if order doesn't exist
     * @throws OrderProcessingException if the state transition is invalid
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderEvent event) {
        logger.info("Updating order status for ID: {} with event: {}", orderId, event);
        
        Order order = getOrderOrThrow(orderId);
        
        // Check if transition is valid
        if (!orderStateMachine.canTransition(order, event)) {
            logger.error("Cannot transition order {} from state {} with event {}", 
                    orderId, order.getStatus(), event);
            throw new OrderProcessingException(
                    "Invalid state transition from " + order.getStatus() + " with event " + event);
        }
        
        // Perform the transition
        orderStateMachine.transition(order, event);
        order.setLastUpdated(java.time.LocalDateTime.now());
        orderRepository.save(order);
        
        logger.info("Order {} status updated to {} after event {}", orderId, order.getStatus(), event);
        
        // Handle side effects of certain events
        handleStatusUpdateSideEffects(order, event);
    }

    /**
     * Processes a refund for an order.
     * 
     * @param orderId the ID of the order to refund
     * @param refundAmount the amount to refund
     * @throws ResourceNotFoundException if order doesn't exist
     * @throws OrderProcessingException if refund cannot be processed
     */
    @Transactional
    public void refundOrder(Long orderId, BigDecimal refundAmount) {
        logger.info("Processing refund of {} for order ID: {}", refundAmount, orderId);
        
        Order order = getOrderOrThrow(orderId);
        
        // Validate refund amount
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Refund amount must be positive");
        }
        
        if (refundAmount.compareTo(order.getTotalAmount()) > 0) {
            throw new ValidationException("Refund amount cannot exceed order total");
        }
        
        // Check if order can be refunded
        if (!orderStateMachine.canTransition(order, OrderEvent.REFUND)) {
            logger.error("Cannot refund order {} in state {}", orderId, order.getStatus());
            throw new OrderProcessingException("Order cannot be refunded in its current state: " + order.getStatus());
        }
        
        try {
            // Process refund through payment service
            boolean refundProcessed = paymentService.refundPayment(order, refundAmount);
            
            if (!refundProcessed) {
                throw new PaymentProcessingException("Payment gateway rejected refund request");
            }
            
            // Update order state
            orderStateMachine.transition(order, OrderEvent.REFUND);
            order.setRefundAmount(refundAmount);
            order.setLastUpdated(java.time.LocalDateTime.now());
            orderRepository.save(order);
            
            logger.info("Refund processed successfully for order ID: {}", orderId);
            
        } catch (Exception e) {
            logger.error("Error processing refund for order: {}", orderId, e);
            throw new OrderProcessingException("Failed to process refund: " + e.getMessage(), e);
        }
    }

    //-------------------------------------------------------------------------
    // Private helper methods
    //-------------------------------------------------------------------------
    
    /**
     * Helper method to retrieve an order or throw an exception if not found.
     */
    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });
    }
    
    /**
     * Converts an order request to an order entity.
     */
    private Order convertRequestToOrder(OrderRequest orderRequest) {
        // FIXME: Implement proper mapping with validation
        Order order = new Order();
        order.setCustomerId(orderRequest.getCustomerId());
        order.setItems(orderRequest.getItems());
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setBillingAddress(orderRequest.getBillingAddress());
        order.setPaymentDetails(orderRequest.getPaymentDetails());
        
        // Calculate order total
        BigDecimal total = orderRequest.getItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setTotalAmount(total);
        order.setCreatedDate(java.time.LocalDateTime.now());
        order.setLastUpdated(java.time.LocalDateTime.now());
        
        // TODO: Add tax calculation
        // TODO: Add shipping cost calculation
        
        return order;
    }
    
    /**
     * Handle side effects of certain status updates.
     */
    private void handleStatusUpdateSideEffects(Order order, OrderEvent event) {
        // Handle various side effects based on the event
        switch (event) {
            case SHIP:
                // Notify customer of shipping
                // TODO: Implement notification service integration
                logger.info("Order shipped notification would be sent for order: {}", order.getId());
                break;
                
            case DELIVER:
                // Update inventory final counts
                // TODO: Update inventory management system with final delivery confirmation
                logger.info("Final inventory adjustment would be made for order: {}", order.getId());
                break;
                
            case CANCEL:
                // Maybe there are special cancellation workflows
                if (order.getStatus().toString().contains("SHIP")) {
                    // Request return logistics
                    logger.info("Return logistics would be arranged for order: {}", order.getId());
                }
                break;
                
            default:
                // No side effects for other events
                break;
        }
    }
}