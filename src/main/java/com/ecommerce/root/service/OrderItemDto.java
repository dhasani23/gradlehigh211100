package com.ecommerce.root.service;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Order Item information.
 */
public class OrderItemDto {
    
    private Long itemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    
    // Default constructor for deserialization
    public OrderItemDto() {
    }
    
    public OrderItemDto(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(new BigDecimal(quantity));
    }
    
    // Getters and setters
    
    public Long getItemId() {
        return itemId;
    }
    
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    /**
     * Recalculate the total price based on unit price and quantity.
     */
    public void recalculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(new BigDecimal(quantity));
        }
    }
    
    @Override
    public String toString() {
        return "OrderItemDto{" +
                "itemId=" + itemId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}