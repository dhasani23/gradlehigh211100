package com.gradlehigh211100.productcatalog.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for Inventory information
 */
public class InventoryDTO {
    private Long id;
    private Long productId;
    private Long variantId;
    private Integer quantity;
    private Integer lowStockThreshold;
    private String inventoryType;
    private LocalDateTime expirationDate;
    private String hazardLevel;
    private String location;
    private String batchNumber;
    private LocalDateTime lastUpdated;

    public InventoryDTO() {
    }

    public InventoryDTO(Long id, Long productId, Long variantId, Integer quantity, 
                      Integer lowStockThreshold, String inventoryType, 
                      LocalDateTime expirationDate, String hazardLevel, 
                      String location, String batchNumber, LocalDateTime lastUpdated) {
        this.id = id;
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.inventoryType = inventoryType;
        this.expirationDate = expirationDate;
        this.hazardLevel = hazardLevel;
        this.location = location;
        this.batchNumber = batchNumber;
        this.lastUpdated = lastUpdated;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getInventoryType() {
        return inventoryType;
    }

    public void setInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getHazardLevel() {
        return hazardLevel;
    }

    public void setHazardLevel(String hazardLevel) {
        this.hazardLevel = hazardLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryDTO that = (InventoryDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(productId, that.productId) &&
                Objects.equals(variantId, that.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId, variantId);
    }

    @Override
    public String toString() {
        return "InventoryDTO{" +
                "id=" + id +
                ", productId=" + productId +
                ", variantId=" + variantId +
                ", quantity=" + quantity +
                ", lowStockThreshold=" + lowStockThreshold +
                ", inventoryType='" + inventoryType + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}