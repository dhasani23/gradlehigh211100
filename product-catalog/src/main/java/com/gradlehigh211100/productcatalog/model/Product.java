package com.gradlehigh211100.productcatalog.model;

import com.gradlehigh211100.common.model.BaseEntity;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core product entity representing a product with all its attributes including SKU,
 * name, description, category, pricing, and inventory information.
 * 
 * This class maintains complex relationships with variants and images, and provides 
 * methods to manage these associations efficiently.
 */
public class Product extends BaseEntity {
    
    // Core product attributes
    private String sku;
    private String name;
    private String description;
    private String brand;
    private Category category;
    private BigDecimal basePrice;
    
    // Collection attributes with complex relationship management
    private Set<ProductVariant> variants = new HashSet<>();
    private Set<ProductImage> images = new HashSet<>();
    
    // Status and metadata
    private Boolean isActive = true;
    private String tags;
    private String metaTitle;
    private String metaDescription;
    
    // Cache for performance optimization
    private transient ProductImage primaryImage;
    private transient boolean variantsCacheNeedsRefresh = true;
    private transient List<ProductVariant> activeVariantsCache;
    
    /**
     * Default constructor
     */
    public Product() {
        // Default constructor required by JPA
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param sku Product SKU
     * @param name Product name
     * @param basePrice Base price
     */
    public Product(String sku, String name, BigDecimal basePrice) {
        this.sku = sku;
        this.name = name;
        this.basePrice = basePrice;
    }
    
    /**
     * Complete constructor with all fields
     */
    public Product(String sku, String name, String description, String brand, 
                  Category category, BigDecimal basePrice, Boolean isActive,
                  String tags, String metaTitle, String metaDescription) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.basePrice = basePrice;
        this.isActive = isActive;
        this.tags = tags;
        this.metaTitle = metaTitle;
        this.metaDescription = metaDescription;
    }
    
    /**
     * Get product SKU
     * 
     * @return SKU of the product
     */
    public String getSku() {
        return sku;
    }
    
    /**
     * Set product SKU
     * 
     * @param sku SKU to set
     */
    public void setSku(String sku) {
        // Input validation with error handling for complex business rules
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be empty or null");
        }
        
        // Apply SKU formatting rules (uppercase, trimming)
        this.sku = sku.trim().toUpperCase();
        
        // Check for specific SKU patterns that might require special handling
        if (this.sku.startsWith("TEMP-")) {
            // FIXME: Temporary SKUs shouldn't be allowed in production
            this.isActive = false;
        } else if (this.sku.startsWith("DISC-")) {
            // Apply discount logic for products with DISC prefix
            if (this.basePrice != null && this.basePrice.compareTo(BigDecimal.ZERO) > 0) {
                // Apply standard 10% discount for discontinued items
                this.basePrice = this.basePrice.multiply(new BigDecimal("0.90")).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }
    }
    
    /**
     * Add product variant with bidirectional relationship management
     * 
     * @param variant ProductVariant to add
     */
    public void addVariant(ProductVariant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("Variant cannot be null");
        }
        
        // Complex business logic for variant validation
        if (variants.stream().anyMatch(v -> v.getVariantSku().equals(variant.getVariantSku()))) {
            throw new IllegalArgumentException("Duplicate variant SKU: " + variant.getVariantSku());
        }
        
        // Bidirectional relationship management
        variants.add(variant);
        variant.setProduct(this);
        
        // Invalidate cache
        variantsCacheNeedsRefresh = true;
        
        // Additional business logic
        if (variant.isDefaultVariant()) {
            // Ensure only one default variant
            variants.stream()
                .filter(v -> v != variant && v.isDefaultVariant())
                .forEach(v -> v.setDefaultVariant(false));
        }
        
        // Update base price if this is the first variant and base price is not set
        if (basePrice == null && variant.getPrice() != null) {
            basePrice = variant.getPrice();
        }
    }
    
    /**
     * Remove product variant with bidirectional relationship management
     * 
     * @param variant ProductVariant to remove
     */
    public void removeVariant(ProductVariant variant) {
        if (variant == null) {
            return;
        }
        
        // Complex business logic for variant removal
        boolean wasRemoved = variants.remove(variant);
        
        if (wasRemoved) {
            variant.setProduct(null);
            variantsCacheNeedsRefresh = true;
            
            // Additional business logic - if removed variant was the default one,
            // choose a new default variant if possible
            if (variant.isDefaultVariant() && !variants.isEmpty()) {
                variants.iterator().next().setDefaultVariant(true);
            }
        }
        
        // TODO: Add event notification for inventory systems when a variant is removed
    }
    
    /**
     * Add product image with bidirectional relationship management
     * 
     * @param image ProductImage to add
     */
    public void addImage(ProductImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        
        // Complex business logic
        boolean added = images.add(image);
        
        if (added) {
            image.setProduct(this);
            
            // Handle primary image logic
            if (image.isPrimary()) {
                // Only one primary image allowed - update others
                for (ProductImage existingImage : images) {
                    if (existingImage != image && existingImage.isPrimary()) {
                        existingImage.setPrimary(false);
                    }
                }
                primaryImage = image;
            } else if (images.size() == 1) {
                // If this is the first image, make it primary by default
                image.setPrimary(true);
                primaryImage = image;
            }
        }
    }
    
    /**
     * Get primary product image with caching for performance
     * 
     * @return The primary product image or null if no images exist
     */
    public ProductImage getPrimaryImage() {
        // Check cache first
        if (primaryImage != null && images.contains(primaryImage) && primaryImage.isPrimary()) {
            return primaryImage;
        }
        
        // Find primary image with complex logic
        primaryImage = images.stream()
            .filter(ProductImage::isPrimary)
            .findFirst()
            .orElse(null);
        
        // If no primary image is set but we have images, select the first one
        if (primaryImage == null && !images.isEmpty()) {
            primaryImage = images.iterator().next();
            
            // FIXME: This can cause data inconsistency if not persisted properly
            primaryImage.setPrimary(true);
        }
        
        return primaryImage;
    }
    
    /**
     * Get list of active variants with caching for performance
     * 
     * @return List of active product variants
     */
    public List<ProductVariant> getActiveVariants() {
        // Performance optimization using cache
        if (variantsCacheNeedsRefresh || activeVariantsCache == null) {
            // Complex filtering logic with multiple conditions
            activeVariantsCache = variants.stream()
                .filter(variant -> {
                    if (!variant.isActive()) {
                        return false;
                    }
                    
                    // Out of stock check (if inventory available is tracked)
                    if (variant.getInventoryCount() != null && variant.getInventoryCount() <= 0) {
                        return false;
                    }
                    
                    // Variant validity period check
                    Date now = new Date();
                    if (variant.getValidFrom() != null && now.before(variant.getValidFrom())) {
                        return false;
                    }
                    
                    if (variant.getValidUntil() != null && now.after(variant.getValidUntil())) {
                        return false;
                    }
                    
                    return true;
                })
                .sorted(Comparator
                    .comparing(ProductVariant::isDefaultVariant).reversed()
                    .thenComparing(ProductVariant::getPrice)
                )
                .collect(Collectors.toList());
            
            variantsCacheNeedsRefresh = false;
        }
        
        return new ArrayList<>(activeVariantsCache);
    }
    
    // Standard getters and setters with validation where appropriate
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        // Complex business logic for category validation
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        
        Category oldCategory = this.category;
        this.category = category;
        
        // Track category change for analytics purposes
        if (oldCategory != null && !oldCategory.equals(category)) {
            // TODO: Log category change for product analytics
        }
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        // Validation and business rules
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }
        this.basePrice = basePrice;
    }

    public Set<ProductVariant> getVariants() {
        // Return defensive copy to prevent collection manipulation
        return Collections.unmodifiableSet(variants);
    }

    public void setVariants(Set<ProductVariant> variants) {
        if (variants == null) {
            this.variants = new HashSet<>();
        } else {
            // Update bidirectional relationships and clear existing
            this.variants.clear();
            for (ProductVariant variant : variants) {
                addVariant(variant);
            }
        }
    }

    public Set<ProductImage> getImages() {
        // Return defensive copy to prevent collection manipulation
        return Collections.unmodifiableSet(images);
    }

    public void setImages(Set<ProductImage> images) {
        if (images == null) {
            this.images = new HashSet<>();
        } else {
            // Update bidirectional relationships and clear existing
            this.images.clear();
            for (ProductImage image : images) {
                addImage(image);
            }
        }
        // Reset primary image cache
        this.primaryImage = null;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        if (this.isActive != null && this.isActive.booleanValue() != active.booleanValue()) {
            // Product status is changing - invalidate caches
            variantsCacheNeedsRefresh = true;
        }
        this.isActive = active;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
    
    /**
     * Get product tags as list
     * 
     * @return List of tags
     */
    public List<String> getTagsList() {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toList());
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        Product product = (Product) o;
        
        // Use SKU as the main equality check since it should be unique
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sku);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + getId() +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", active=" + isActive +
                ", variants=" + variants.size() +
                '}';
    }
}