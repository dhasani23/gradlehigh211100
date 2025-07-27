package com.gradlehigh211100.productcatalog.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data transfer object for product variant information including size, color, style variations 
 * with pricing and inventory details.
 * 
 * This class represents different variations of a product with specific attributes like size,
 * color, style, etc. It contains pricing information and inventory-related data.
 */
public class ProductVariantDTO {
    
    // Unique identifier for the product variant
    private Long id;
    
    // ID of the parent product
    private Long productId;
    
    // Stock keeping unit for the variant
    private String sku;
    
    // Size specification of the variant
    private String size;
    
    // Color specification of the variant
    private String color;
    
    // Style specification of the variant
    private String style;
    
    // Price of the variant
    private BigDecimal price;
    
    // Weight of the variant in standard units
    private Double weight;
    
    // Physical dimensions of the variant (format: LxWxH)
    private String dimensions;
    
    /**
     * Default constructor for ProductVariantDTO
     */
    public ProductVariantDTO() {
        // Default constructor required for serialization/deserialization frameworks
    }
    
    /**
     * Parameterized constructor for creating a complete product variant
     *
     * @param id          Unique identifier for the variant
     * @param productId   Parent product ID
     * @param sku         Stock keeping unit
     * @param size        Size specification
     * @param color       Color specification
     * @param style       Style specification
     * @param price       Price of the variant
     * @param weight      Weight of the variant
     * @param dimensions  Physical dimensions
     */
    public ProductVariantDTO(Long id, Long productId, String sku, String size, String color, 
                           String style, BigDecimal price, Double weight, String dimensions) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.size = size;
        this.color = color;
        this.style = style;
        this.price = price;
        this.weight = weight;
        this.dimensions = dimensions;
    }

    /**
     * Get variant ID
     * 
     * @return the variant's unique identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Set variant ID
     * 
     * @param id the variant's unique identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Get parent product ID
     * 
     * @return the parent product's ID
     */
    public Long getProductId() {
        return productId;
    }
    
    /**
     * Set parent product ID
     * 
     * @param productId the parent product's ID to set
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    /**
     * Get stock keeping unit
     * 
     * @return the variant's SKU
     */
    public String getSku() {
        return sku;
    }
    
    /**
     * Set stock keeping unit
     * 
     * @param sku the variant's SKU to set
     */
    public void setSku(String sku) {
        // FIXME: Should validate SKU format before setting
        this.sku = sku;
    }
    
    /**
     * Get size specification
     * 
     * @return the variant's size
     */
    public String getSize() {
        return size;
    }
    
    /**
     * Set size specification
     * 
     * @param size the variant's size to set
     */
    public void setSize(String size) {
        this.size = size;
    }
    
    /**
     * Get color specification
     * 
     * @return the variant's color
     */
    public String getColor() {
        return color;
    }
    
    /**
     * Set color specification
     * 
     * @param color the variant's color to set
     */
    public void setColor(String color) {
        // Color validation logic could be added here
        if (color != null && color.trim().isEmpty()) {
            // Handle empty color string case
            this.color = null;
        } else {
            this.color = color;
        }
    }
    
    /**
     * Get style specification
     * 
     * @return the variant's style
     */
    public String getStyle() {
        return style;
    }
    
    /**
     * Set style specification
     * 
     * @param style the variant's style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }
    
    /**
     * Get variant price
     * 
     * @return the variant's price
     */
    public BigDecimal getPrice() {
        return price;
    }
    
    /**
     * Set variant price
     * 
     * @param price the variant's price to set
     */
    public void setPrice(BigDecimal price) {
        // Price validation logic
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            // TODO: Consider throwing an exception for negative prices
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.price = price;
    }
    
    /**
     * Get variant weight
     * 
     * @return the variant's weight
     */
    public Double getWeight() {
        return weight;
    }
    
    /**
     * Set variant weight
     * 
     * @param weight the variant's weight to set
     */
    public void setWeight(Double weight) {
        // Weight validation logic
        if (weight != null && weight < 0) {
            // FIXME: Need to decide if we should throw exception or set to 0
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        this.weight = weight;
    }
    
    /**
     * Get physical dimensions
     * 
     * @return the variant's dimensions
     */
    public String getDimensions() {
        return dimensions;
    }
    
    /**
     * Set physical dimensions
     * 
     * @param dimensions the variant's dimensions to set
     */
    public void setDimensions(String dimensions) {
        // TODO: Add dimension format validation (e.g., LxWxH)
        this.dimensions = dimensions;
    }

    /**
     * Creates a composite key for this variant using productId and variant attributes
     * 
     * @return a String that can be used as a composite key
     */
    public String createCompositeKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(productId)
                .append(":")
                .append(size == null ? "" : size)
                .append(":")
                .append(color == null ? "" : color)
                .append(":")
                .append(style == null ? "" : style);
        return keyBuilder.toString();
    }
    
    /**
     * Checks if this variant matches the specified criteria
     * 
     * @param size the size to match
     * @param color the color to match
     * @param style the style to match
     * @return true if this variant matches all the non-null criteria
     */
    public boolean matches(String size, String color, String style) {
        boolean sizeMatches = size == null || Objects.equals(this.size, size);
        boolean colorMatches = color == null || Objects.equals(this.color, color);
        boolean styleMatches = style == null || Objects.equals(this.style, style);
        
        return sizeMatches && colorMatches && styleMatches;
    }

    /**
     * Calculates if this variant is eligible for express shipping
     * 
     * @param maxExpressWeight the maximum weight allowed for express shipping
     * @return true if this variant is eligible for express shipping
     */
    public boolean isEligibleForExpressShipping(double maxExpressWeight) {
        // Complex business logic for express shipping eligibility
        if (this.weight == null) {
            return false;
        }
        
        // Basic weight check
        if (this.weight > maxExpressWeight) {
            return false;
        }
        
        // Dimension checks (assuming format "LxWxH" in cm)
        if (this.dimensions != null && !this.dimensions.isEmpty()) {
            try {
                String[] dims = this.dimensions.split("x");
                if (dims.length == 3) {
                    double length = Double.parseDouble(dims[0]);
                    double width = Double.parseDouble(dims[1]);
                    double height = Double.parseDouble(dims[2]);
                    
                    // Max dimension should not exceed 60cm for express shipping
                    double maxDimension = Math.max(length, Math.max(width, height));
                    if (maxDimension > 60) {
                        return false;
                    }
                    
                    // Sum of dimensions should not exceed 120cm
                    if ((length + width + height) > 120) {
                        return false;
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // In case of parsing errors or invalid format
                return false;
            }
        }
        
        return true;
    }

    /**
     * Calculates shipping cost based on weight and dimensions
     * 
     * @param baseRate the base shipping rate
     * @param weightMultiplier multiplier for weight-based cost
     * @return the calculated shipping cost
     */
    public BigDecimal calculateShippingCost(BigDecimal baseRate, BigDecimal weightMultiplier) {
        if (baseRate == null || weightMultiplier == null) {
            throw new IllegalArgumentException("Shipping rate parameters cannot be null");
        }
        
        BigDecimal shippingCost = baseRate;
        
        // Add weight-based cost if weight is available
        if (this.weight != null) {
            BigDecimal weightCost = weightMultiplier.multiply(BigDecimal.valueOf(this.weight));
            shippingCost = shippingCost.add(weightCost);
        }
        
        // Add dimension-based surcharge if applicable
        if (this.dimensions != null && !this.dimensions.isEmpty()) {
            try {
                String[] dims = this.dimensions.split("x");
                if (dims.length == 3) {
                    double length = Double.parseDouble(dims[0]);
                    double width = Double.parseDouble(dims[1]);
                    double height = Double.parseDouble(dims[2]);
                    
                    // Calculate dimensional weight (L*W*H/5000) in kg
                    double dimensionalWeight = (length * width * height) / 5000.0;
                    
                    // If dimensional weight is greater than actual weight, use it for calculation
                    if (this.weight != null && dimensionalWeight > this.weight) {
                        BigDecimal dimensionalCost = weightMultiplier.multiply(
                            BigDecimal.valueOf(dimensionalWeight - this.weight));
                        shippingCost = shippingCost.add(dimensionalCost);
                    }
                    
                    // Add surcharge for oversized items
                    double maxDimension = Math.max(length, Math.max(width, height));
                    if (maxDimension > 100) {
                        shippingCost = shippingCost.add(new BigDecimal("10.00"));
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // Fallback to base cost plus weight if dimension parsing fails
            }
        }
        
        return shippingCost;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProductVariantDTO that = (ProductVariantDTO) o;
        
        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(productId, that.productId)) return false;
        if (!Objects.equals(sku, that.sku)) return false;
        if (!Objects.equals(size, that.size)) return false;
        if (!Objects.equals(color, that.color)) return false;
        if (!Objects.equals(style, that.style)) return false;
        if (!Objects.equals(price, that.price)) return false;
        if (!Objects.equals(weight, that.weight)) return false;
        return Objects.equals(dimensions, that.dimensions);
    }
    
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        result = 31 * result + (sku != null ? sku.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (style != null ? style.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (weight != null ? weight.hashCode() : 0);
        result = 31 * result + (dimensions != null ? dimensions.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "ProductVariantDTO{" +
                "id=" + id +
                ", productId=" + productId +
                ", sku='" + sku + '\'' +
                ", size='" + size + '\'' +
                ", color='" + color + '\'' +
                ", style='" + style + '\'' +
                ", price=" + price +
                ", weight=" + weight +
                ", dimensions='" + dimensions + '\'' +
                '}';
    }
}