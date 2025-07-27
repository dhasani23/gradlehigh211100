package com.gradlehigh211100.productcatalog.model;

import com.gradlehigh211100.common.model.BaseEntity;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Product variant entity representing different variations of a product
 * such as size, color, style with specific pricing and inventory.
 * 
 * This class extends BaseEntity to inherit common entity attributes
 * and relates to a parent Product.
 */
public class ProductVariant extends BaseEntity {
    
    private Product product;
    private String sku;
    private String size;
    private String color;
    private String style;
    private BigDecimal price;
    private Double weight;
    private String dimensions;
    private Boolean isActive;
    
    // Constants for validation
    private static final int MAX_SKU_LENGTH = 50;
    private static final int MAX_COLOR_LENGTH = 30;
    private static final int MAX_SIZE_LENGTH = 20;
    private static final int MAX_STYLE_LENGTH = 50;
    private static final int MAX_DIMENSIONS_LENGTH = 100;
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9-_]{1,50}$");
    private static final BigDecimal MIN_PRICE = BigDecimal.ZERO;
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999.99");
    private static final Double MIN_WEIGHT = 0.0;
    private static final Double MAX_WEIGHT = 9999.99;
    
    /**
     * Default constructor
     */
    public ProductVariant() {
        super();
        this.isActive = true;
    }
    
    /**
     * Parameterized constructor for creating a complete variant
     * 
     * @param product Parent product reference
     * @param sku Stock keeping unit
     * @param size Size specification
     * @param color Color specification
     * @param style Style specification
     * @param price Variant price
     * @param weight Variant weight
     * @param dimensions Physical dimensions
     * @param isActive Active status flag
     */
    public ProductVariant(Product product, String sku, String size, String color, 
            String style, BigDecimal price, Double weight, String dimensions, Boolean isActive) {
        super();
        this.product = product;
        this.sku = validateSku(sku);
        this.size = validateSize(size);
        this.color = validateColor(color);
        this.style = validateStyle(style);
        this.price = validatePrice(price);
        this.weight = validateWeight(weight);
        this.dimensions = validateDimensions(dimensions);
        this.isActive = (isActive != null) ? isActive : true;
    }

    /**
     * Get the parent product
     * 
     * @return The parent product
     */
    public Product getProduct() {
        return product;
    }

    /**
     * Set the parent product
     * 
     * @param product The parent product to set
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * Get variant SKU
     * 
     * @return The variant SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Set variant SKU with validation
     * 
     * @param sku The SKU to set
     */
    public void setSku(String sku) {
        this.sku = validateSku(sku);
    }

    /**
     * Get variant size
     * 
     * @return The variant size
     */
    public String getSize() {
        return size;
    }

    /**
     * Set variant size with validation
     * 
     * @param size The size to set
     */
    public void setSize(String size) {
        this.size = validateSize(size);
    }

    /**
     * Get variant color
     * 
     * @return The variant color
     */
    public String getColor() {
        return color;
    }

    /**
     * Set variant color with validation
     * 
     * @param color The color to set
     */
    public void setColor(String color) {
        this.color = validateColor(color);
    }

    /**
     * Get variant style
     * 
     * @return The variant style
     */
    public String getStyle() {
        return style;
    }

    /**
     * Set variant style with validation
     * 
     * @param style The style to set
     */
    public void setStyle(String style) {
        this.style = validateStyle(style);
    }

    /**
     * Get variant price
     * 
     * @return The variant price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Set variant price with validation
     * 
     * @param price The price to set
     */
    public void setPrice(BigDecimal price) {
        this.price = validatePrice(price);
    }

    /**
     * Get variant weight
     * 
     * @return The variant weight
     */
    public Double getWeight() {
        return weight;
    }

    /**
     * Set variant weight with validation
     * 
     * @param weight The weight to set
     */
    public void setWeight(Double weight) {
        this.weight = validateWeight(weight);
    }

    /**
     * Get variant dimensions
     * 
     * @return The variant dimensions
     */
    public String getDimensions() {
        return dimensions;
    }

    /**
     * Set variant dimensions with validation
     * 
     * @param dimensions The dimensions to set
     */
    public void setDimensions(String dimensions) {
        this.dimensions = validateDimensions(dimensions);
    }

    /**
     * Get variant active status
     * 
     * @return The variant active status
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Set variant active status
     * 
     * @param isActive The active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    /**
     * Get formatted display name including size and color
     * 
     * @return A formatted display name for this variant
     */
    public String getDisplayName() {
        StringBuilder displayName = new StringBuilder();
        
        // Complex logic to demonstrate high cyclomatic complexity
        if (product != null && product.getName() != null) {
            displayName.append(product.getName());
            
            boolean hasAttributes = false;
            
            // Add variant attributes if present
            if (size != null && !size.trim().isEmpty()) {
                displayName.append(" - Size: ").append(size);
                hasAttributes = true;
            }
            
            if (color != null && !color.trim().isEmpty()) {
                if (hasAttributes) {
                    displayName.append(", ");
                } else {
                    displayName.append(" - ");
                }
                displayName.append("Color: ").append(color);
                hasAttributes = true;
            }
            
            if (style != null && !style.trim().isEmpty()) {
                if (hasAttributes) {
                    displayName.append(", ");
                } else {
                    displayName.append(" - ");
                }
                displayName.append("Style: ").append(style);
                hasAttributes = true;
            }
            
            if (!hasAttributes) {
                displayName.append(" - Standard");
            }
            
            if (sku != null && !sku.trim().isEmpty()) {
                displayName.append(" (").append(sku).append(")");
            }
            
        } else {
            // Fallback if product name is not available
            displayName.append("Variant");
            if (sku != null && !sku.trim().isEmpty()) {
                displayName.append(" ").append(sku);
            }
            
            boolean hasAttributes = false;
            
            if (size != null && !size.trim().isEmpty()) {
                displayName.append(" - Size: ").append(size);
                hasAttributes = true;
            }
            
            if (color != null && !color.trim().isEmpty()) {
                if (hasAttributes) {
                    displayName.append(", ");
                } else {
                    displayName.append(" - ");
                }
                displayName.append("Color: ").append(color);
            }
        }
        
        return displayName.toString();
    }
    
    /**
     * Check if variant is available based on active status and price
     * 
     * @return true if variant is available, false otherwise
     */
    public Boolean isAvailable() {
        // Complex availability check with high cyclomatic complexity
        if (isActive == null || !isActive) {
            return false;
        }
        
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (product == null) {
            return false;
        }
        
        if (sku == null || sku.trim().isEmpty()) {
            return false;
        }
        
        // Additional availability checks could be added here
        // This is placeholder for inventory check logic
        
        // FIXME: Add integration with inventory management system
        // TODO: Implement inventory threshold check
        
        return true;
    }
    
    /**
     * Validate SKU format and length
     * 
     * @param sku The SKU to validate
     * @return The validated SKU
     * @throws IllegalArgumentException if SKU is invalid
     */
    private String validateSku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        
        if (sku.length() > MAX_SKU_LENGTH) {
            throw new IllegalArgumentException("SKU length exceeds maximum limit of " + MAX_SKU_LENGTH);
        }
        
        if (!SKU_PATTERN.matcher(sku).matches()) {
            throw new IllegalArgumentException("SKU format is invalid. Only alphanumeric characters, hyphens and underscores are allowed.");
        }
        
        return sku;
    }
    
    /**
     * Validate size format and length
     * 
     * @param size The size to validate
     * @return The validated size
     */
    private String validateSize(String size) {
        if (size != null && size.length() > MAX_SIZE_LENGTH) {
            throw new IllegalArgumentException("Size length exceeds maximum limit of " + MAX_SIZE_LENGTH);
        }
        return size;
    }
    
    /**
     * Validate color format and length
     * 
     * @param color The color to validate
     * @return The validated color
     */
    private String validateColor(String color) {
        if (color != null && color.length() > MAX_COLOR_LENGTH) {
            throw new IllegalArgumentException("Color length exceeds maximum limit of " + MAX_COLOR_LENGTH);
        }
        return color;
    }
    
    /**
     * Validate style format and length
     * 
     * @param style The style to validate
     * @return The validated style
     */
    private String validateStyle(String style) {
        if (style != null && style.length() > MAX_STYLE_LENGTH) {
            throw new IllegalArgumentException("Style length exceeds maximum limit of " + MAX_STYLE_LENGTH);
        }
        return style;
    }
    
    /**
     * Validate price value range
     * 
     * @param price The price to validate
     * @return The validated price
     */
    private BigDecimal validatePrice(BigDecimal price) {
        if (price != null) {
            if (price.compareTo(MIN_PRICE) < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            if (price.compareTo(MAX_PRICE) > 0) {
                throw new IllegalArgumentException("Price exceeds maximum limit of " + MAX_PRICE);
            }
        }
        return price;
    }
    
    /**
     * Validate weight value range
     * 
     * @param weight The weight to validate
     * @return The validated weight
     */
    private Double validateWeight(Double weight) {
        if (weight != null) {
            if (weight < MIN_WEIGHT) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }
            if (weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("Weight exceeds maximum limit of " + MAX_WEIGHT);
            }
        }
        return weight;
    }
    
    /**
     * Validate dimensions format and length
     * 
     * @param dimensions The dimensions to validate
     * @return The validated dimensions
     */
    private String validateDimensions(String dimensions) {
        if (dimensions != null && dimensions.length() > MAX_DIMENSIONS_LENGTH) {
            throw new IllegalArgumentException("Dimensions length exceeds maximum limit of " + MAX_DIMENSIONS_LENGTH);
        }
        return dimensions;
    }

    /**
     * Calculate pricing based on various factors to demonstrate high cyclomatic complexity
     * 
     * @param basePrice Base price to use for calculation
     * @param discountPercent Discount percentage to apply
     * @param taxPercent Tax percentage to apply
     * @param includesTax Flag indicating if price includes tax
     * @param isPromotion Flag indicating if promotion is active
     * @return The calculated price
     */
    public BigDecimal calculatePricing(BigDecimal basePrice, BigDecimal discountPercent, 
                                     BigDecimal taxPercent, boolean includesTax, boolean isPromotion) {
        
        if (basePrice == null) {
            throw new IllegalArgumentException("Base price cannot be null");
        }
        
        BigDecimal calculatedPrice = basePrice;
        
        // Apply size-based adjustments
        if (size != null) {
            if (size.toLowerCase().contains("large") || size.toLowerCase().contains("xl")) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("1.15"));
            } else if (size.toLowerCase().contains("medium") || size.equalsIgnoreCase("m")) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("1.05"));
            } else if (size.toLowerCase().contains("small") || size.equalsIgnoreCase("s")) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("0.95"));
            }
        }
        
        // Apply weight-based adjustments
        if (weight != null) {
            if (weight > 10.0) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("1.2"));
            } else if (weight > 5.0) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("1.1"));
            } else if (weight > 2.0) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("1.05"));
            }
        }
        
        // Apply style-based adjustments
        if (style != null) {
            if (style.toLowerCase().contains("premium") || style.toLowerCase().contains("deluxe")) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("1.25"));
            } else if (style.toLowerCase().contains("standard")) {
                // No adjustment for standard
            } else if (style.toLowerCase().contains("basic") || style.toLowerCase().contains("economy")) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("0.9"));
            }
        }
        
        // Apply discount if applicable
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            if (discountPercent.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Discount percentage cannot exceed 100%");
            }
            
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountPercent.divide(new BigDecimal("100")));
            calculatedPrice = calculatedPrice.multiply(discountMultiplier);
        }
        
        // Apply promotional discount if applicable
        if (isPromotion) {
            // Additional 5% off for promotions
            calculatedPrice = calculatedPrice.multiply(new BigDecimal("0.95"));
        }
        
        // Apply tax if applicable
        if (taxPercent != null && taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            if (includesTax) {
                // Price already includes tax, so we need to extract it first
                BigDecimal taxMultiplier = BigDecimal.ONE.add(taxPercent.divide(new BigDecimal("100")));
                calculatedPrice = calculatedPrice.divide(taxMultiplier, 2, BigDecimal.ROUND_HALF_UP);
            }
            
            // Now apply the specified tax
            BigDecimal taxMultiplier = BigDecimal.ONE.add(taxPercent.divide(new BigDecimal("100")));
            calculatedPrice = calculatedPrice.multiply(taxMultiplier);
        }
        
        // Round to 2 decimal places
        calculatedPrice = calculatedPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        
        return calculatedPrice;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductVariant)) return false;
        if (!super.equals(o)) return false;
        
        ProductVariant that = (ProductVariant) o;
        
        if (!Objects.equals(sku, that.sku)) return false;
        if (product != null ? !product.getId().equals(that.product != null ? that.product.getId() : null) : that.product != null) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sku != null ? sku.hashCode() : 0);
        result = 31 * result + (product != null ? product.getId().hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "ProductVariant{" +
                "id=" + getId() +
                ", sku='" + sku + '\'' +
                ", size='" + size + '\'' +
                ", color='" + color + '\'' +
                ", style='" + style + '\'' +
                ", price=" + price +
                ", weight=" + weight +
                ", dimensions='" + dimensions + '\'' +
                ", isActive=" + isActive +
                ", product=" + (product != null ? product.getId() : "null") +
                '}';
    }
}