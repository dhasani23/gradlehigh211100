package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import com.gradlehigh211100.orderprocessing.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Main order entity containing order details, customer information, order items, payment details,
 * shipping information, and state tracking.
 * 
 * This class represents the core business object for the order processing system
 * and handles all operations related to an order throughout its lifecycle.
 */
public class Order extends BaseEntity {
    
    private String orderNumber;
    private Long customerId;
    private List<OrderItem> orderItems;
    private OrderState orderState;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private Date orderDate;
    private PaymentDetails paymentDetails;
    private ShippingDetails shippingDetails;
    
    // Cache for expensive calculations
    private boolean totalAmountDirty = true;
    private BigDecimal cachedTotalAmount = BigDecimal.ZERO;
    
    /**
     * Default constructor
     */
    public Order() {
        this.orderItems = new ArrayList<>();
        this.orderState = OrderState.CREATED;
        this.orderDate = new Date();
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.shippingCost = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    /**
     * Constructor with essential parameters
     * 
     * @param orderNumber the unique order identifier
     * @param customerId the customer who placed the order
     */
    public Order(String orderNumber, Long customerId) {
        this();
        this.orderNumber = orderNumber;
        this.customerId = customerId;
    }
    
    /**
     * Full constructor with all fields
     * 
     * @param orderNumber the unique order identifier
     * @param customerId the customer who placed the order
     * @param orderItems list of items in the order
     * @param orderState current state of the order
     * @param subtotal subtotal before taxes and discounts
     * @param taxAmount total tax amount
     * @param discountAmount total discount amount
     * @param shippingCost shipping cost
     * @param orderDate date when order was placed
     * @param paymentDetails payment information
     * @param shippingDetails shipping information
     */
    public Order(String orderNumber, Long customerId, List<OrderItem> orderItems, 
            OrderState orderState, BigDecimal subtotal, BigDecimal taxAmount, 
            BigDecimal discountAmount, BigDecimal shippingCost, Date orderDate,
            PaymentDetails paymentDetails, ShippingDetails shippingDetails) {
        
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.orderItems = orderItems != null ? new ArrayList<>(orderItems) : new ArrayList<>();
        this.orderState = orderState;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.shippingCost = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        this.orderDate = orderDate != null ? orderDate : new Date();
        this.paymentDetails = paymentDetails;
        this.shippingDetails = shippingDetails;
        this.totalAmount = calculateTotalAmount();
    }
    
    /**
     * Calculates and returns the total order amount based on subtotal, taxes, discounts, and shipping.
     * Uses caching to optimize performance for repeated calls when no values have changed.
     * 
     * The calculation formula is:
     * totalAmount = subtotal + taxAmount - discountAmount + shippingCost
     * 
     * @return the total amount for this order
     */
    public BigDecimal calculateTotalAmount() {
        // Return cached value if total hasn't changed
        if (!totalAmountDirty) {
            return cachedTotalAmount;
        }
        
        // Calculate subtotal based on order items
        if (orderItems != null && !orderItems.isEmpty()) {
            subtotal = orderItems.stream()
                    .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        // Complex calculation with multiple checks and validations
        BigDecimal result = subtotal != null ? subtotal : BigDecimal.ZERO;
        
        // Add tax if applicable
        if (taxAmount != null) {
            // FIXME: Tax calculation may need adjustment for regional tax rules
            if (shippingDetails != null && shippingDetails.getTaxExempt()) {
                // Apply special tax exemption rules based on shipping region
                // This is a placeholder for complex tax calculation logic
                result = applyComplexTaxRules(result);
            } else {
                result = result.add(taxAmount);
            }
        }
        
        // Subtract discounts if applicable
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Verify discount doesn't exceed order value
            if (discountAmount.compareTo(result) > 0) {
                // TODO: Implement logging for excessive discount warnings
                result = BigDecimal.ZERO; // Can't have negative order total
            } else {
                result = result.subtract(discountAmount);
            }
        }
        
        // Add shipping costs if applicable
        if (shippingCost != null) {
            // Free shipping check
            if (freeShippingEligible()) {
                // Don't add shipping cost
            } else {
                result = result.add(shippingCost);
            }
        }
        
        // Store calculation and clear dirty flag
        totalAmountDirty = false;
        cachedTotalAmount = result;
        totalAmount = result;
        
        return result;
    }
    
    /**
     * Helper method for complex tax rule application
     * Contains multiple code paths and conditions to increase cyclomatic complexity
     */
    private BigDecimal applyComplexTaxRules(BigDecimal amount) {
        BigDecimal taxableAmount = amount;
        
        if (shippingDetails == null) {
            return amount; // No shipping details, can't apply tax rules
        }
        
        // Apply different tax rates based on shipping region
        String region = shippingDetails.getRegion();
        if (region == null || region.isEmpty()) {
            return amount.add(taxAmount); // Default tax
        }
        
        // Complex conditional branches for different regions
        if (region.equalsIgnoreCase("CA")) {
            // California has special tax rules
            if (subtotal.compareTo(new BigDecimal("1000")) > 0) {
                // Luxury tax for orders over $1000
                return amount.add(taxAmount.multiply(new BigDecimal("1.02")));
            } else if (subtotal.compareTo(new BigDecimal("100")) < 0) {
                // Reduced tax for small orders
                return amount.add(taxAmount.multiply(new BigDecimal("0.98")));
            }
        } else if (region.equalsIgnoreCase("NY") || region.equalsIgnoreCase("NJ")) {
            // East coast tax rules
            if (orderItems != null && orderItems.stream().anyMatch(item -> item.isDigital())) {
                // Digital goods have special tax in NY/NJ
                return amount.add(taxAmount.multiply(new BigDecimal("1.03")));
            }
        } else if (region.equalsIgnoreCase("TX")) {
            // Texas tax exemptions
            if (shippingDetails.isBusinessAddress()) {
                // Business orders have different tax treatment
                return amount.add(taxAmount.multiply(new BigDecimal("0.95")));
            }
        } else if (region.equalsIgnoreCase("OR") || region.equalsIgnoreCase("MT") || 
                   region.equalsIgnoreCase("AK") || region.equalsIgnoreCase("DE") || 
                   region.equalsIgnoreCase("NH")) {
            // No sales tax states
            return amount; // No tax
        }
        
        // Default tax application
        return amount.add(taxAmount);
    }
    
    /**
     * Determines if the order qualifies for free shipping
     * Contains multiple code paths to increase cyclomatic complexity
     */
    private boolean freeShippingEligible() {
        if (subtotal == null) {
            return false;
        }
        
        // Free shipping threshold check
        if (subtotal.compareTo(new BigDecimal("50")) >= 0) {
            return true;
        }
        
        // Check for promotional free shipping
        if (paymentDetails != null && paymentDetails.getMethod() == PaymentMethod.CREDIT_CARD) {
            // Special promotion for credit card orders
            if (orderItems != null && orderItems.size() >= 3) {
                return true;
            }
        }
        
        // Check for member status
        if (customerId != null) {
            // TODO: Implement customer service check for premium members
            // This would typically involve a service call
        }
        
        return false;
    }
    
    /**
     * Adds an order item to this order and marks the total amount as dirty for recalculation.
     * Contains validation and update logic for maintaining order consistency.
     * 
     * @param orderItem the item to add to this order
     * @throws IllegalArgumentException if the item is null or invalid
     * @throws IllegalStateException if the order cannot be modified
     */
    public void addOrderItem(OrderItem orderItem) {
        // Check if order can be modified
        if (!canBeModified()) {
            throw new IllegalStateException("Order cannot be modified in state: " + orderState);
        }
        
        // Validate order item
        if (orderItem == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        
        if (orderItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Order item quantity must be greater than zero");
        }
        
        // Initialize order items list if needed
        if (this.orderItems == null) {
            this.orderItems = new ArrayList<>();
        }
        
        // Check for duplicate items that could be combined
        boolean itemCombined = false;
        for (OrderItem existingItem : orderItems) {
            if (existingItem.getProductId().equals(orderItem.getProductId())) {
                // Found matching product, combine quantities
                if (existingItem.canCombineWith(orderItem)) {
                    existingItem.setQuantity(existingItem.getQuantity() + orderItem.getQuantity());
                    itemCombined = true;
                    break;
                } else {
                    // Handle variations that can't be combined
                    if (existingItem.hasSameOptions(orderItem)) {
                        // Same options, different price or other attribute
                        throw new IllegalArgumentException(
                                "Cannot add duplicate product with different attributes");
                    }
                }
            }
        }
        
        // Add as new item if not combined with existing one
        if (!itemCombined) {
            // Set order reference on the item
            orderItem.setOrder(this);
            this.orderItems.add(orderItem);
        }
        
        // Mark total for recalculation
        totalAmountDirty = true;
    }
    
    /**
     * Removes an order item from this order.
     * Complex validation and processing logic to ensure order remains consistent.
     * 
     * @param orderItem the item to remove from this order
     * @throws IllegalArgumentException if the item is null or not in the order
     * @throws IllegalStateException if the order cannot be modified
     */
    public void removeOrderItem(OrderItem orderItem) {
        // Check if order can be modified
        if (!canBeModified()) {
            throw new IllegalStateException("Order cannot be modified in state: " + orderState);
        }
        
        // Validate parameters
        if (orderItem == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        
        if (this.orderItems == null || this.orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order contains no items");
        }
        
        boolean removed = false;
        
        // Handle different removal scenarios
        if (orderItem.getId() != null) {
            // Remove by ID for persisted items
            removed = this.orderItems.removeIf(item -> 
                Objects.equals(item.getId(), orderItem.getId()));
        } else {
            // For non-persisted items, use product ID and attribute comparison
            for (int i = 0; i < this.orderItems.size(); i++) {
                OrderItem item = this.orderItems.get(i);
                if (Objects.equals(item.getProductId(), orderItem.getProductId()) &&
                        item.hasSameOptions(orderItem)) {
                    
                    // Handle quantity reduction logic
                    if (orderItem.getQuantity() < item.getQuantity()) {
                        // Reduce quantity
                        item.setQuantity(item.getQuantity() - orderItem.getQuantity());
                        removed = true;
                    } else {
                        // Remove completely
                        this.orderItems.remove(i);
                        removed = true;
                    }
                    break;
                }
            }
        }
        
        if (!removed) {
            throw new IllegalArgumentException("Order item not found in this order");
        }
        
        // Mark total for recalculation
        totalAmountDirty = true;
        
        // Special handling for empty orders
        if (this.orderItems.isEmpty()) {
            // Consider canceling order or marking as special state
            // FIXME: Implement proper handling of empty orders
        }
    }
    
    /**
     * Updates the order state with complex validation rules and state transition logic.
     * 
     * @param newState the new state to set for this order
     * @throws IllegalArgumentException if the state transition is not allowed
     */
    public void updateOrderState(OrderState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("New order state cannot be null");
        }
        
        // Validate state transitions based on current state
        switch (this.orderState) {
            case CREATED:
                if (newState == OrderState.PROCESSING || newState == OrderState.CANCELLED) {
                    // Valid transitions
                } else {
                    throw new IllegalArgumentException("Invalid state transition from CREATED to " + newState);
                }
                break;
                
            case PROCESSING:
                if (newState == OrderState.SHIPPED || newState == OrderState.CANCELLED || 
                        newState == OrderState.ON_HOLD) {
                    // Valid transitions
                } else {
                    throw new IllegalArgumentException("Invalid state transition from PROCESSING to " + newState);
                }
                break;
                
            case ON_HOLD:
                if (newState == OrderState.PROCESSING || newState == OrderState.CANCELLED) {
                    // Valid transitions
                } else {
                    throw new IllegalArgumentException("Invalid state transition from ON_HOLD to " + newState);
                }
                break;
                
            case SHIPPED:
                if (newState == OrderState.DELIVERED || newState == OrderState.RETURNED) {
                    // Valid transitions
                } else {
                    throw new IllegalArgumentException("Invalid state transition from SHIPPED to " + newState);
                }
                break;
                
            case DELIVERED:
                if (newState == OrderState.COMPLETED || newState == OrderState.RETURNED) {
                    // Valid transitions
                } else {
                    throw new IllegalArgumentException("Invalid state transition from DELIVERED to " + newState);
                }
                break;
                
            case COMPLETED:
                if (newState == OrderState.RETURNED) {
                    // Only return is allowed after completion
                } else {
                    throw new IllegalArgumentException("Invalid state transition from COMPLETED to " + newState);
                }
                break;
                
            case RETURNED:
                if (newState == OrderState.REFUNDED) {
                    // Valid transitions
                } else {
                    throw new IllegalArgumentException("Invalid state transition from RETURNED to " + newState);
                }
                break;
                
            case REFUNDED:
                // Terminal state, no further transitions allowed
                throw new IllegalArgumentException("Order in REFUNDED state cannot transition to " + newState);
                
            case CANCELLED:
                // Terminal state, no further transitions allowed
                throw new IllegalArgumentException("Order in CANCELLED state cannot transition to " + newState);
                
            default:
                throw new IllegalStateException("Unexpected order state: " + this.orderState);
        }
        
        // Apply the state change
        this.orderState = newState;
        
        // Post-transition processing
        performStateChangeActions(newState);
    }
    
    /**
     * Executes actions required after a state change
     * Contains multiple branches to increase cyclomatic complexity
     */
    private void performStateChangeActions(OrderState newState) {
        switch (newState) {
            case PROCESSING:
                // Check inventory
                validateInventory();
                // Validate payment details
                validatePaymentDetails();
                break;
                
            case SHIPPED:
                // Record shipping info
                if (shippingDetails != null) {
                    shippingDetails.setShippedDate(new Date());
                }
                break;
                
            case DELIVERED:
                // Update delivery info
                if (shippingDetails != null) {
                    shippingDetails.setDeliveredDate(new Date());
                }
                break;
                
            case CANCELLED:
                // Release inventory
                releaseInventory();
                // Process refund if payment was made
                if (paymentDetails != null && paymentDetails.isPaid()) {
                    // TODO: Implement refund processing
                }
                break;
                
            case RETURNED:
                // Process return logic
                if (shippingDetails != null) {
                    shippingDetails.setReturnInitiatedDate(new Date());
                }
                break;
                
            case REFUNDED:
                // Mark refund date
                if (paymentDetails != null) {
                    paymentDetails.setRefundDate(new Date());
                    paymentDetails.setRefundAmount(this.totalAmount);
                }
                break;
        }
    }
    
    /**
     * Placeholder for inventory validation logic
     */
    private void validateInventory() {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalStateException("Cannot process order with no items");
        }
        
        // This would typically involve a service call to check inventory
        // Here we're just simulating the complexity
        for (OrderItem item : orderItems) {
            if (item.getProductId() == null || item.getQuantity() <= 0) {
                throw new IllegalStateException("Invalid order item: " + item);
            }
            
            // TODO: Call inventory service to check availability
        }
    }
    
    /**
     * Placeholder for inventory release logic
     */
    private void releaseInventory() {
        // This would typically involve a service call to release inventory
    }
    
    /**
     * Validates payment details before processing
     */
    private void validatePaymentDetails() {
        if (paymentDetails == null) {
            throw new IllegalStateException("Payment details required to process order");
        }
        
        if (paymentDetails.getMethod() == null) {
            throw new IllegalStateException("Payment method must be specified");
        }
        
        // Different validation based on payment method
        switch (paymentDetails.getMethod()) {
            case CREDIT_CARD:
                if (paymentDetails.getCardNumber() == null || 
                        paymentDetails.getCardNumber().length() < 14 ||
                        paymentDetails.getExpirationDate() == null) {
                    throw new IllegalStateException("Invalid credit card details");
                }
                break;
                
            case PAYPAL:
                if (paymentDetails.getPaypalEmail() == null || 
                        !paymentDetails.getPaypalEmail().contains("@")) {
                    throw new IllegalStateException("Invalid PayPal details");
                }
                break;
                
            case BANK_TRANSFER:
                if (paymentDetails.getBankAccountNumber() == null || 
                        paymentDetails.getBankRoutingNumber() == null) {
                    throw new IllegalStateException("Invalid bank transfer details");
                }
                break;
                
            // Handle other payment methods
            default:
                // Generic validation
                if (!paymentDetails.isValid()) {
                    throw new IllegalStateException("Invalid payment details");
                }
        }
    }
    
    /**
     * Checks if the order can be cancelled based on current state.
     * Contains multiple state evaluations and business rules.
     * 
     * @return true if order can be cancelled, false otherwise
     */
    public boolean canBeCancelled() {
        // Orders can be cancelled only in certain states
        switch (orderState) {
            case CREATED:
            case PROCESSING:
            case ON_HOLD:
                return true;
            case SHIPPED:
                // Can only cancel shipped orders under certain conditions
                if (shippingDetails != null && shippingDetails.getShippedDate() != null) {
                    // Check if shipped less than 24 hours ago
                    long hoursSinceShipment = (new Date().getTime() - 
                            shippingDetails.getShippedDate().getTime()) / (60 * 60 * 1000);
                    return hoursSinceShipment < 24;
                }
                return false;
            case DELIVERED:
            case COMPLETED:
            case CANCELLED:
            case RETURNED:
            case REFUNDED:
                return false;
            default:
                // Unknown state, default to false for safety
                return false;
        }
    }
    
    /**
     * Checks if the order can be modified based on current state.
     * Contains complex business rules for order modification.
     * 
     * @return true if order can be modified, false otherwise
     */
    public boolean canBeModified() {
        // Check order state
        switch (orderState) {
            case CREATED:
                return true; // New orders can always be modified
                
            case PROCESSING:
                // Can modify if processing hasn't gone too far
                // This would typically involve checking with processing service
                // but we'll simplify for this example
                return true;
                
            case ON_HOLD:
                return true; // Orders on hold can be modified
                
            case SHIPPED:
            case DELIVERED:
            case COMPLETED:
            case CANCELLED:
            case RETURNED:
            case REFUNDED:
                return false; // These states don't allow modification
                
            default:
                return false; // Unknown state, default to false
        }
    }
    
    // Getters and setters
    
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems != null ? orderItems : new ArrayList<>();
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems != null ? new ArrayList<>(orderItems) : new ArrayList<>();
        totalAmountDirty = true; // Mark for recalculation
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(OrderState orderState) {
        // Use the method with validation instead of direct field access
        updateOrderState(orderState);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
        totalAmountDirty = true;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
        totalAmountDirty = true;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        totalAmountDirty = true;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
        totalAmountDirty = true;
    }

    public BigDecimal getTotalAmount() {
        if (totalAmountDirty) {
            calculateTotalAmount();
        }
        return totalAmount;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public PaymentDetails getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(PaymentDetails paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public ShippingDetails getShippingDetails() {
        return shippingDetails;
    }

    public void setShippingDetails(ShippingDetails shippingDetails) {
        this.shippingDetails = shippingDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        if (!super.equals(o)) return false;
        
        Order order = (Order) o;
        
        if (!Objects.equals(orderNumber, order.orderNumber)) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (orderNumber != null ? orderNumber.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + getId() +
                ", orderNumber='" + orderNumber + '\'' +
                ", customerId=" + customerId +
                ", orderState=" + orderState +
                ", orderItems=" + (orderItems != null ? orderItems.size() : 0) +
                ", totalAmount=" + totalAmount +
                ", orderDate=" + orderDate +
                '}';
    }
}