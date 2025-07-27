package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an individual item within an order.
 * Contains product details, quantity, price, and options.
 */
public class OrderItem extends BaseEntity {
    
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private Order order;
    private boolean digital;
    private Map<String, String> options;
    
    /**
     * Default constructor
     */
    public OrderItem() {
        this.quantity = 0;
        this.price = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.options = new HashMap<>();
        this.digital = false;
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param productId the ID of the product
     * @param productName the name of the product
     * @param quantity the quantity of this item
     * @param price the price per unit
     */
    public OrderItem(Long productId, String productName, Integer quantity, BigDecimal price) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }
    
    /**
     * Full constructor
     * 
     * @param productId the ID of the product
     * @param productName the name of the product
     * @param productSku the SKU of the product
     * @param quantity the quantity of this item
     * @param price the price per unit
     * @param discount the discount amount per unit
     * @param order the parent order
     * @param digital whether this item is digital
     * @param options additional options/attributes for this item
     */
    public OrderItem(Long productId, String productName, String productSku, Integer quantity, 
            BigDecimal price, BigDecimal discount, Order order, boolean digital, 
            Map<String, String> options) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.price = price;
        this.discount = discount;
        this.order = order;
        this.digital = digital;
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
    }
    
    /**
     * Determines if this item can be combined with another item
     * (same product, same options)
     * 
     * @param other the other order item to check
     * @return true if the items can be combined, false otherwise
     */
    public boolean canCombineWith(OrderItem other) {
        if (other == null) {
            return false;
        }
        
        return Objects.equals(this.productId, other.productId) &&
                hasSameOptions(other) &&
                Objects.equals(this.price, other.price) &&
                Objects.equals(this.discount, other.discount);
    }
    
    /**
     * Checks if this item has the same options as another item
     * 
     * @param other the other item to compare with
     * @return true if options are the same, false otherwise
     */
    public boolean hasSameOptions(OrderItem other) {
        if (other == null) {
            return false;
        }
        
        if (this.options == null && other.options == null) {
            return true;
        }
        
        if (this.options == null || other.options == null) {
            return false;
        }
        
        return this.options.equals(other.options);
    }

    // Getters and setters
    
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

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public boolean isDigital() {
        return digital;
    }

    public void setDigital(boolean digital) {
        this.digital = digital;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
    }
    
    /**
     * Adds a single option to the item
     * 
     * @param key the option key
     * @param value the option value
     */
    public void addOption(String key, String value) {
        if (this.options == null) {
            this.options = new HashMap<>();
        }
        this.options.put(key, value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        if (!super.equals(o)) return false;
        
        OrderItem orderItem = (OrderItem) o;
        
        if (!Objects.equals(productId, orderItem.productId)) return false;
        if (!Objects.equals(options, orderItem.options)) return false;
        if (order != null && orderItem.order != null && 
                !Objects.equals(order.getOrderNumber(), orderItem.order.getOrderNumber())) {
            return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + getId() +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}