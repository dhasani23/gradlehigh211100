package com.gradlehigh211100.productcatalog.model;

import java.util.Date;
import java.util.Map;

/**
 * Data Transfer Object for inventory information
 */
public class InventoryDTO {
    
    private Long productId;
    private Long variantId;
    private Integer quantity;
    private Integer reservedQuantity;
    private String productName;
    private String category;
    private Date expirationDate; // For perishable items
    private Map<String, String> safetyInformation; // For hazardous items
    private boolean lowStock;
    
    public InventoryDTO() {
        // Default constructor
    }
    
    public InventoryDTO(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
    
    public InventoryDTO(Long productId, Long variantId, Integer quantity) {
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
    }
    
    // Getters and setters
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Long getVariantId() {
        return variantId;
    }
    
    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Date getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public Map<String, String> getSafetyInformation() {
        return safetyInformation;
    }
    
    public void setSafetyInformation(Map<String, String> safetyInformation) {
        this.safetyInformation = safetyInformation;
    }
    
    public boolean isLowStock() {
        return lowStock;
    }
    
    public void setLowStock(boolean lowStock) {
        this.lowStock = lowStock;
    }
    
    @Override
    public String toString() {
        return "InventoryDTO{" +
                "productId=" + productId +
                ", variantId=" + variantId +
                ", quantity=" + quantity +
                ", reservedQuantity=" + reservedQuantity +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", lowStock=" + lowStock +
                '}';
    }
}