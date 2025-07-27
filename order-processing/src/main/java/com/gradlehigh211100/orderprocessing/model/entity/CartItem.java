package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing an item in a user's shopping cart.
 * This class stores information about a product, its quantity, and price in the cart context.
 */
public class CartItem extends BaseEntity {
    
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private String productName;
    private String productImageUrl;
    private Date addedDate;
    
    public CartItem() {
        this.addedDate = new Date();
        this.quantity = 1;
    }
    
    public CartItem(Long productId, Integer quantity, BigDecimal price) {
        this();
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.calculateTotalPrice();
    }

    /**
     * Calculates the total price based on item quantity and unit price
     */
    public void calculateTotalPrice() {
        if (price != null && quantity != null) {
            this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    // Getters and setters
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateTotalPrice();
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
        calculateTotalPrice();
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductImageUrl() {
        return productImageUrl;
    }
    
    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }
    
    public Date getAddedDate() {
        return addedDate != null ? new Date(addedDate.getTime()) : null;
    }
    
    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate != null ? new Date(addedDate.getTime()) : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem)) return false;
        if (!super.equals(o)) return false;
        
        CartItem cartItem = (CartItem) o;
        return Objects.equals(productId, cartItem.productId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), productId);
    }
    
    @Override
    public String toString() {
        return "CartItem{" +
               "id=" + getId() +
               ", productId=" + productId +
               ", quantity=" + quantity +
               ", price=" + price +
               ", totalPrice=" + totalPrice +
               ", productName='" + productName + '\'' +
               '}';
    }
}