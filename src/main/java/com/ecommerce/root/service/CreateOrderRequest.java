package com.ecommerce.root.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request object for creating a new order.
 */
public class CreateOrderRequest {
    
    private Long userId;
    private List<OrderItemDto> items = new ArrayList<>();
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private String currency = "USD"; // Default currency
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private String couponCode;
    private String notes;
    
    // Constructors
    
    public CreateOrderRequest() {
    }
    
    public CreateOrderRequest(Long userId, List<OrderItemDto> items, String shippingAddress) {
        this.userId = userId;
        this.items = items;
        this.shippingAddress = shippingAddress;
    }
    
    // Getters and Setters
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public List<OrderItemDto> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
    
    /**
     * Add an item to the order.
     * 
     * @param item The item to add
     * @return This request object for method chaining
     */
    public CreateOrderRequest addItem(OrderItemDto item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        return this;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getBillingAddress() {
        return billingAddress;
    }
    
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
    
    /**
     * Use shipping address as billing address if billing address is not provided.
     */
    public void useSameAddressForShippingAndBilling() {
        this.billingAddress = this.shippingAddress;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getShippingCost() {
        return shippingCost;
    }
    
    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }
    
    public String getCouponCode() {
        return couponCode;
    }
    
    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Calculate the total price of all items in the order.
     * 
     * @return Total price
     */
    public BigDecimal calculateTotalItemsPrice() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return items.stream()
                .map(item -> {
                    if (item.getTotalPrice() != null) {
                        return item.getTotalPrice();
                    } else if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        return item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
                    } else {
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate total order amount including shipping.
     * 
     * @return Total amount
     */
    public BigDecimal calculateTotalAmount() {
        return calculateTotalItemsPrice().add(shippingCost != null ? shippingCost : BigDecimal.ZERO);
    }
    
    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "userId=" + userId +
                ", items=" + (items != null ? items.size() : 0) +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", currency='" + currency + '\'' +
                ", totalAmount=" + calculateTotalAmount() +
                '}';
    }
}