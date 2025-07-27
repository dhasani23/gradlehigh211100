package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entity representing a line item in an order.
 * Contains information about a product, its quantity, and pricing.
 */
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_code", nullable = false)
    private String productCode;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;
    
    /**
     * Default constructor required by JPA
     */
    public OrderItem() {
        // Required by JPA
    }
    
    /**
     * Creates a new order item with the specified details
     * 
     * @param productId the ID of the product
     * @param productName the name of the product
     * @param productCode the product code/SKU
     * @param quantity the quantity ordered
     * @param unitPrice the price per unit
     */
    public OrderItem(Long productId, String productName, String productCode, 
                    Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }
    
    /**
     * Calculates the subtotal for this item based on quantity and unit price
     */
    private void calculateSubtotal() {
        if (quantity != null && unitPrice != null) {
            this.subtotal = unitPrice.multiply(new BigDecimal(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }
    
    /**
     * Updates the quantity of this item and recalculates the subtotal
     * 
     * @param quantity the new quantity
     */
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    // Getters and Setters
    
    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    protected void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        OrderItem orderItem = (OrderItem) o;
        
        return Objects.equals(getId(), orderItem.getId()) &&
               Objects.equals(order, orderItem.order) &&
               Objects.equals(productId, orderItem.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), order, productId);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + getId() +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", subtotal=" + subtotal +
                '}';
    }
}