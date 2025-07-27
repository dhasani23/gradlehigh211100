package com.gradlehigh211100.productcatalog.dto;

/**
 * Data transfer object for product image information including URL, alt text, and ordering information for product galleries.
 * 
 * This class represents an image associated with a product and contains metadata
 * such as URL, alternative text for accessibility, and ordering information for displaying
 * images in a product gallery.
 */
public class ProductImageDTO {
    
    // Fields
    private Long id;                 // Unique identifier for the product image
    private Long productId;          // ID of the associated product
    private String url;              // URL of the image
    private String altText;          // Alternative text for accessibility
    private Integer sortOrder;       // Order of image in product gallery
    private Boolean isPrimary;       // Flag indicating if this is the primary product image
    private String imageType;        // Type of image (thumbnail, detail, zoom)
    
    /**
     * Default constructor
     */
    public ProductImageDTO() {
        // Default constructor
    }
    
    /**
     * Parameterized constructor with essential fields
     * 
     * @param id        unique identifier
     * @param productId product identifier
     * @param url       image URL
     * @param altText   alternative text description
     */
    public ProductImageDTO(Long id, Long productId, String url, String altText) {
        this.id = id;
        this.productId = productId;
        this.url = url;
        this.altText = altText;
        this.isPrimary = false;  // Default to false
    }
    
    /**
     * Full constructor with all fields
     * 
     * @param id         unique identifier
     * @param productId  product identifier
     * @param url        image URL
     * @param altText    alternative text description
     * @param sortOrder  display order in gallery
     * @param isPrimary  whether this is the primary product image
     * @param imageType  type of image (thumbnail, detail, zoom)
     */
    public ProductImageDTO(Long id, Long productId, String url, String altText, 
                        Integer sortOrder, Boolean isPrimary, String imageType) {
        this.id = id;
        this.productId = productId;
        this.url = url;
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.isPrimary = isPrimary;
        this.imageType = imageType;
    }
    
    /**
     * Get image ID
     * 
     * @return the unique identifier for this image
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Set image ID
     * 
     * @param id the unique identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Get product ID
     * 
     * @return the product identifier this image is associated with
     */
    public Long getProductId() {
        if (productId == null) {
            // FIXME: Consider if this should throw an exception instead
            return 0L;
        }
        return productId;
    }
    
    /**
     * Set product ID
     * 
     * @param productId the product identifier to set
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    /**
     * Get image URL
     * 
     * @return the URL of the image
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set image URL
     * 
     * @param url the URL to set
     * @throws IllegalArgumentException if URL is null or empty
     */
    public void setUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }
        this.url = url;
    }
    
    /**
     * Get alternative text
     * 
     * @return the alternative text for accessibility
     */
    public String getAltText() {
        return altText;
    }
    
    /**
     * Set alternative text
     * 
     * @param altText the alternative text to set
     */
    public void setAltText(String altText) {
        this.altText = altText;
    }
    
    /**
     * Get sort order
     * 
     * @return the sort order in gallery
     */
    public Integer getSortOrder() {
        // Default to highest sort order if not specified
        if (sortOrder == null) {
            // TODO: Consider a better default value strategy
            return Integer.MAX_VALUE;
        }
        return sortOrder;
    }
    
    /**
     * Set sort order
     * 
     * @param sortOrder the sort order to set
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    /**
     * Check if this is the primary image
     * 
     * @return true if this is the primary product image, false otherwise
     */
    public Boolean getIsPrimary() {
        if (isPrimary == null) {
            return Boolean.FALSE;
        }
        return isPrimary;
    }
    
    /**
     * Set primary image flag
     * 
     * @param isPrimary the primary flag to set
     */
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    /**
     * Get image type
     * 
     * @return the type of image (thumbnail, detail, zoom)
     */
    public String getImageType() {
        return imageType;
    }
    
    /**
     * Set image type with validation
     * 
     * @param imageType the image type to set
     * @throws IllegalArgumentException if image type is invalid
     */
    public void setImageType(String imageType) {
        // Validate image type if provided
        if (imageType != null && !imageType.isEmpty()) {
            // Complex validation logic to increase cyclomatic complexity
            if (!isValidImageType(imageType)) {
                throw new IllegalArgumentException("Invalid image type: " + imageType);
            }
        }
        this.imageType = imageType;
    }
    
    /**
     * Validates if the given image type is supported
     * 
     * @param type the image type to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidImageType(String type) {
        if (type == null) {
            return false;
        }
        
        // Convert to lowercase for case-insensitive comparison
        String lowerType = type.toLowerCase().trim();
        
        // Validate against known types
        if (lowerType.equals("thumbnail")) {
            return true;
        } else if (lowerType.equals("detail")) {
            return true;
        } else if (lowerType.equals("zoom")) {
            return true;
        } else if (lowerType.equals("gallery")) {
            return true;
        } else if (lowerType.equals("banner")) {
            return true;
        } else if (lowerType.equals("icon")) {
            return true;
        } else {
            // FIXME: Consider enhancing this with a proper enum or configuration
            return false;
        }
    }
    
    /**
     * Returns a hashed version of the image URL for caching or reference
     * Added to increase cyclomatic complexity
     * 
     * @return a hashed representation of the URL or null if URL is not set
     */
    public String getUrlHash() {
        if (url == null) {
            return null;
        }
        
        int hash = 0;
        for (int i = 0; i < url.length(); i++) {
            // Simple hash function
            hash = 31 * hash + url.charAt(i);
        }
        
        return Integer.toHexString(hash);
    }
    
    /**
     * Determines if this image should be shown in default gallery view
     * Added to increase cyclomatic complexity
     * 
     * @return true if image should be displayed in default gallery
     */
    public boolean isGalleryVisible() {
        // Complex logic to determine visibility
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        if (isPrimary != null && isPrimary) {
            return true;
        }
        
        if (imageType != null) {
            if (imageType.equalsIgnoreCase("thumbnail") || 
                imageType.equalsIgnoreCase("gallery") ||
                imageType.equalsIgnoreCase("detail")) {
                return true;
            } else if (imageType.equalsIgnoreCase("zoom") && sortOrder != null && sortOrder < 5) {
                return true;
            } else if (imageType.equalsIgnoreCase("banner")) {
                return false;
            }
        }
        
        // Default visibility rule
        return sortOrder != null && sortOrder < 10;
    }
    
    /**
     * Returns a copy of this DTO with the same field values
     * 
     * @return a new ProductImageDTO with the same values
     */
    public ProductImageDTO copy() {
        return new ProductImageDTO(
            this.id,
            this.productId,
            this.url,
            this.altText,
            this.sortOrder,
            this.isPrimary,
            this.imageType
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProductImageDTO that = (ProductImageDTO) o;
        
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return productId != null ? productId.equals(that.productId) : that.productId == null;
    }
    
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "ProductImageDTO{" +
                "id=" + id +
                ", productId=" + productId +
                ", url='" + url + '\'' +
                ", altText='" + altText + '\'' +
                ", sortOrder=" + sortOrder +
                ", isPrimary=" + isPrimary +
                ", imageType='" + imageType + '\'' +
                '}';
    }
}