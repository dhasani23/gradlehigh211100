package com.gradlehigh211100.productcatalog.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data transfer object representing product information
 * Used for transferring product data between layers of the application
 */
public class ProductDTO {
    
    private Long id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Boolean onSale;
    private Integer stockQuantity;
    private List<String> categories;
    private Map<String, String> attributes;
    private String imageUrl;
    
    /**
     * Default constructor
     */
    public ProductDTO() {
        this.categories = new ArrayList<>();
        this.attributes = new HashMap<>();
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id The product ID
     * @param name The product name
     * @param price The product price
     */
    public ProductDTO(Long id, String name, BigDecimal price) {
        this();
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    /**
     * Full constructor
     * 
     * @param id The product ID
     * @param name The product name
     * @param description The product description
     * @param sku The product SKU
     * @param price The regular price
     * @param salePrice The sale price
     * @param onSale Whether the product is on sale
     * @param stockQuantity Available stock quantity
     * @param categories List of product categories
     * @param attributes Map of product attributes
     * @param imageUrl URL to product image
     */
    public ProductDTO(Long id, String name, String description, String sku, 
                    BigDecimal price, BigDecimal salePrice, Boolean onSale, 
                    Integer stockQuantity, List<String> categories, 
                    Map<String, String> attributes, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
        this.salePrice = salePrice;
        this.onSale = onSale;
        this.stockQuantity = stockQuantity;
        this.categories = categories != null ? categories : new ArrayList<>();
        this.attributes = attributes != null ? attributes : new HashMap<>();
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Boolean getOnSale() {
        return onSale;
    }

    public void setOnSale(Boolean onSale) {
        this.onSale = onSale;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    /**
     * Add a category to the product
     * 
     * @param category The category to add
     * @return True if the category was added, false if already exists
     */
    public boolean addCategory(String category) {
        if (category == null) {
            return false;
        }
        
        if (categories == null) {
            categories = new ArrayList<>();
        }
        
        if (!categories.contains(category)) {
            categories.add(category);
            return true;
        }
        return false;
    }
    
    /**
     * Add an attribute to the product
     * 
     * @param key Attribute key
     * @param value Attribute value
     * @return The previous value or null if none
     */
    public String addAttribute(String key, String value) {
        if (key == null) {
            return null;
        }
        
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        return attributes.put(key, value);
    }
    
    /**
     * Calculate the current price (sale price if on sale, regular price otherwise)
     * 
     * @return The current price
     */
    public BigDecimal getCurrentPrice() {
        if (Boolean.TRUE.equals(onSale) && salePrice != null) {
            return salePrice;
        }
        return price;
    }
    
    /**
     * Check if the product is in stock
     * 
     * @return True if the product is in stock, false otherwise
     */
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProductDTO that = (ProductDTO) o;
        
        return Objects.equals(id, that.id) &&
               Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sku);
    }

    @Override
    public String toString() {
        return "ProductDTO{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", sku='" + sku + '\'' +
               ", price=" + price +
               ", onSale=" + onSale +
               ", stockQuantity=" + stockQuantity +
               '}';
    }
}