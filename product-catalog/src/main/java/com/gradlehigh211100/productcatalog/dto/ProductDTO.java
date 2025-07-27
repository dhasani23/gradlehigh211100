package com.gradlehigh211100.productcatalog.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Data transfer object for product information containing core product details,
 * category, variants, and images.
 * 
 * This class represents the complete product information needed for display and
 * manipulation in the product catalog system.
 */
public class ProductDTO {
    
    // Core product attributes
    private Long id;
    private String sku;
    private String name;
    private String description;
    private String brand;
    private BigDecimal basePrice;
    
    // Related product information
    private CategoryDTO category;
    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;
    
    // Status fields
    private Boolean isActive;
    private Date createdDate;
    
    /**
     * Default constructor initializes collections
     */
    public ProductDTO() {
        this.variants = new ArrayList<>();
        this.images = new ArrayList<>();
        this.isActive = true;
        this.createdDate = new Date();
    }
    
    /**
     * Constructor with essential product attributes
     * 
     * @param id The product ID
     * @param sku The stock keeping unit
     * @param name The product name
     * @param basePrice The base price
     */
    public ProductDTO(Long id, String sku, String name, BigDecimal basePrice) {
        this();
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.basePrice = basePrice;
    }
    
    /**
     * Full constructor with all product attributes
     * 
     * @param id The product ID
     * @param sku The stock keeping unit
     * @param name The product name
     * @param description The product description
     * @param brand The product brand
     * @param basePrice The base price
     * @param category The product category
     * @param isActive Whether the product is active
     * @param createdDate The creation date
     */
    public ProductDTO(Long id, String sku, String name, String description, String brand,
                    BigDecimal basePrice, CategoryDTO category, Boolean isActive, Date createdDate) {
        this();
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.basePrice = basePrice;
        this.category = category;
        
        if (isActive != null) {
            this.isActive = isActive;
        }
        
        if (createdDate != null) {
            this.createdDate = createdDate;
        }
    }

    /**
     * Get the product ID
     * 
     * @return The product ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the product ID
     * 
     * @param id The product ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the stock keeping unit
     * 
     * @return The SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Set the stock keeping unit
     * 
     * @param sku The SKU to set
     */
    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * Get the product name
     * 
     * @return The product name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the product name
     * 
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the product description
     * 
     * @return The product description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the product description
     * 
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the product brand
     * 
     * @return The brand
     */
    public String getBrand() {
        return brand;
    }

    /**
     * Set the product brand
     * 
     * @param brand The brand to set
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /**
     * Get the base price
     * 
     * @return The base price
     */
    public BigDecimal getBasePrice() {
        return basePrice;
    }

    /**
     * Set the base price
     * 
     * @param basePrice The base price to set
     */
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    /**
     * Get the product category
     * 
     * @return The category
     */
    public CategoryDTO getCategory() {
        return category;
    }

    /**
     * Set the product category
     * 
     * @param category The category to set
     */
    public void setCategory(CategoryDTO category) {
        this.category = category;
    }

    /**
     * Get all product variants
     * 
     * @return List of product variants
     */
    public List<ProductVariantDTO> getVariants() {
        return variants;
    }

    /**
     * Set the product variants
     * 
     * @param variants The variants to set
     */
    public void setVariants(List<ProductVariantDTO> variants) {
        this.variants = variants != null ? variants : new ArrayList<>();
    }
    
    /**
     * Add a product variant
     * 
     * @param variant The variant to add
     */
    public void addVariant(ProductVariantDTO variant) {
        if (variant == null) {
            throw new IllegalArgumentException("Variant cannot be null");
        }
        
        if (this.variants == null) {
            this.variants = new ArrayList<>();
        }
        
        // Check for duplicate SKU
        boolean duplicateSku = this.variants.stream()
                .anyMatch(v -> Objects.equals(v.getSku(), variant.getSku()));
        
        if (duplicateSku) {
            // FIXME: Better duplicate handling strategy needed - currently just ignoring duplicates
            return;
        }
        
        this.variants.add(variant);
    }
    
    /**
     * Get only active variants
     * 
     * @return List of active product variants
     */
    public List<ProductVariantDTO> getActiveVariants() {
        if (this.variants == null) {
            return new ArrayList<>();
        }
        
        // Filter variants where isActive is true
        return this.variants.stream()
                .filter(variant -> variant.getIsActive() != null && variant.getIsActive())
                .collect(Collectors.toList());
    }

    /**
     * Get all product images
     * 
     * @return List of product images
     */
    public List<ProductImageDTO> getImages() {
        return images;
    }

    /**
     * Set the product images
     * 
     * @param images The images to set
     */
    public void setImages(List<ProductImageDTO> images) {
        this.images = images != null ? images : new ArrayList<>();
    }
    
    /**
     * Add a product image
     * 
     * @param image The image to add
     */
    public void addImage(ProductImageDTO image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        
        // Check if we already have an image with the same URL
        boolean duplicateImage = this.images.stream()
                .anyMatch(img -> Objects.equals(img.getUrl(), image.getUrl()));
        
        if (duplicateImage) {
            // TODO: Implement proper duplicate image handling strategy
            return;
        }
        
        this.images.add(image);
        
        // Sort images based on priority if available
        this.images.sort((img1, img2) -> {
            if (img1.getPriority() == null && img2.getPriority() == null) {
                return 0;
            } else if (img1.getPriority() == null) {
                return 1;
            } else if (img2.getPriority() == null) {
                return -1;
            }
            return img1.getPriority().compareTo(img2.getPriority());
        });
    }

    /**
     * Check if product is active
     * 
     * @return True if active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Set product active status
     * 
     * @param isActive The active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Get product creation date
     * 
     * @return The creation date
     */
    public Date getCreatedDate() {
        return createdDate;
    }

    /**
     * Set product creation date
     * 
     * @param createdDate The creation date to set
     */
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    
    /**
     * Validates if the product is ready for publication
     * 
     * @return true if the product is valid for publication
     */
    public boolean isValidForPublication() {
        // Complex validation logic with high cyclomatic complexity
        if (id == null || id <= 0) {
            return false;
        }
        
        if (sku == null || sku.trim().isEmpty()) {
            return false;
        }
        
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (category == null || category.getId() == null) {
            return false;
        }
        
        // Must have at least one active variant
        boolean hasActiveVariant = false;
        if (variants != null && !variants.isEmpty()) {
            for (ProductVariantDTO variant : variants) {
                if (variant.getIsActive() != null && variant.getIsActive()) {
                    if (variant.getSku() != null && !variant.getSku().trim().isEmpty()) {
                        if (variant.getPrice() != null && variant.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                            hasActiveVariant = true;
                            break;
                        }
                    }
                }
            }
        }
        
        if (!hasActiveVariant) {
            return false;
        }
        
        // Must have at least one image
        if (images == null || images.isEmpty()) {
            return false;
        }
        
        // Check that there's at least one primary image
        boolean hasPrimaryImage = false;
        for (ProductImageDTO image : images) {
            if (image.getUrl() != null && !image.getUrl().trim().isEmpty()) {
                if (image.getPriority() != null && image.getPriority() == 1) {
                    hasPrimaryImage = true;
                    break;
                }
            }
        }
        
        if (!hasPrimaryImage) {
            return false;
        }
        
        // Product must be active
        if (isActive == null || !isActive) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate the minimum price across all variants
     * 
     * @return The minimum price or base price if no variants
     */
    public BigDecimal getMinimumPrice() {
        if (variants == null || variants.isEmpty()) {
            return basePrice;
        }
        
        BigDecimal minPrice = basePrice;
        boolean foundActiveVariant = false;
        
        for (ProductVariantDTO variant : variants) {
            // Skip inactive variants
            if (variant.getIsActive() == null || !variant.getIsActive()) {
                continue;
            }
            
            // Skip variants without price
            if (variant.getPrice() == null) {
                continue;
            }
            
            // Initialize minPrice with first active variant price
            if (!foundActiveVariant) {
                minPrice = variant.getPrice();
                foundActiveVariant = true;
                continue;
            }
            
            // Compare with current minimum
            if (variant.getPrice().compareTo(minPrice) < 0) {
                minPrice = variant.getPrice();
            }
        }
        
        return minPrice;
    }
    
    /**
     * Calculate the maximum price across all variants
     * 
     * @return The maximum price or base price if no variants
     */
    public BigDecimal getMaximumPrice() {
        if (variants == null || variants.isEmpty()) {
            return basePrice;
        }
        
        BigDecimal maxPrice = basePrice;
        boolean foundActiveVariant = false;
        
        for (ProductVariantDTO variant : variants) {
            // Skip inactive variants
            if (variant.getIsActive() == null || !variant.getIsActive()) {
                continue;
            }
            
            // Skip variants without price
            if (variant.getPrice() == null) {
                continue;
            }
            
            // Initialize maxPrice with first active variant price
            if (!foundActiveVariant) {
                maxPrice = variant.getPrice();
                foundActiveVariant = true;
                continue;
            }
            
            // Compare with current maximum
            if (variant.getPrice().compareTo(maxPrice) > 0) {
                maxPrice = variant.getPrice();
            }
        }
        
        return maxPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProductDTO that = (ProductDTO) o;
        
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return sku != null ? sku.equals(that.sku) : that.sku == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (sku != null ? sku.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProductDTO{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", basePrice=" + basePrice +
                ", category=" + (category != null ? category.getName() : null) +
                ", variants=" + (variants != null ? variants.size() : 0) +
                ", images=" + (images != null ? images.size() : 0) +
                ", isActive=" + isActive +
                '}';
    }
}