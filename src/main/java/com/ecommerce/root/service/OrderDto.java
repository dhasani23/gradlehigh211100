package com.ecommerce.root.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Order information.
 */
public class OrderDto {
    
    private Long orderId;
    private Long userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalAmount;
    private String currency;
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private List<OrderItemDto> items = new ArrayList<>();
    
    // Default constructor for deserialization
    public OrderDto() {
    }
    
    public OrderDto(Long orderId, Long userId, String status, LocalDateTime createdAt, 
                    BigDecimal totalAmount, String currency) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }
    
    // Getters and setters
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
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
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public List<OrderItemDto> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
    
    @Override
    public String toString() {
        return "OrderDto{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", items=" + items.size() +
                '}';
    }
}