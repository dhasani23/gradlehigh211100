package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Entity representing an order in the system.
 * Contains information about the customer, order items, and current state.
 */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    
    @Column(name = "order_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_state", nullable = false)
    private OrderState orderState;
    
    @Column(name = "order_total", precision = 10, scale = 2)
    private BigDecimal orderTotal;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Embedded
    private Address shippingAddress;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
        @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
        @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "billing_zipcode")),
        @AttributeOverride(name = "country", column = @Column(name = "billing_country")),
        @AttributeOverride(name = "region", column = @Column(name = "billing_region"))
    })
    private Address billingAddress;
    
    @ElementCollection
    @CollectionTable(name = "order_categories", 
                    joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "category")
    private Set<String> categories = new HashSet<>();
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("stateChangeDate DESC")
    private List<OrderHistory> history = new ArrayList<>();
    
    /**
     * Default constructor required by JPA
     */
    public Order() {
        // Required by JPA
    }
    
    /**
     * Constructor for creating a new order
     * 
     * @param orderNumber the unique order identifier
     * @param customerId the ID of the customer placing the order
     */
    public Order(String orderNumber, Long customerId) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.orderDate = new Date();
        this.orderState = OrderState.NEW;
    }
    
    /**
     * Changes the order's state and records the change in history
     * 
     * @param newState the new order state
     * @param reason reason for the state change
     * @param changedBy user or system that initiated the change
     * @return the created history record
     */
    public OrderHistory changeState(OrderState newState, String reason, String changedBy) {
        OrderState oldState = this.orderState;
        this.orderState = newState;
        
        OrderHistory historyRecord = new OrderHistory(this, oldState, newState, reason, changedBy);
        this.history.add(historyRecord);
        
        return historyRecord;
    }
    
    /**
     * Adds an item to this order
     * 
     * @param item the item to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }
    
    /**
     * Removes an item from this order
     * 
     * @param item the item to remove
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotal();
    }
    
    /**
     * Recalculates the order total based on all items
     */
    private void recalculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        this.orderTotal = total;
    }
    
    /**
     * Checks if this order contains items in a specific category
     * 
     * @param category the category to check for
     * @return true if the order has items in that category
     */
    public boolean hasCategory(String category) {
        return categories.contains(category);
    }
    
    /**
     * Add a category to this order
     * 
     * @param category the category to add
     */
    public void addCategory(String category) {
        categories.add(category);
    }

    // Customer proxy methods for the OrderHistory class to use
    
    /**
     * Provides access to customer information
     */
    public Customer getCustomer() {
        // TODO: Implement customer retrieval or proxy
        return new Customer(customerId);
    }
    
    // Getters and Setters
    
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public List<OrderHistory> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void setHistory(List<OrderHistory> history) {
        this.history = history;
    }
    
    // Inner class to support OrderHistory functionality
    public static class Customer {
        private Long id;
        private boolean vip;
        
        public Customer(Long id) {
            this.id = id;
            // TODO: Implement actual VIP status determination
            this.vip = false;
        }
        
        public Long getId() {
            return id;
        }
        
        public boolean isVip() {
            return vip;
        }
    }
}