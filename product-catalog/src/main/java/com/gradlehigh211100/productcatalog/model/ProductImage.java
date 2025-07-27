package com.gradlehigh211100.productcatalog.model;

import com.gradlehigh211100.common.model.BaseEntity;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Product image entity storing image URLs, alt text, and ordering information for product gallery.
 * This class represents various types of product images including thumbnails, detailed views, and
 * zoom-enabled images to enhance product visualization.
 * 
 * @since 1.0
 */
public class ProductImage extends BaseEntity {
    
    // Constants for validation
    private static final int MAX_ALT_TEXT_LENGTH = 255;
    private static final int MIN_ALT_TEXT_LENGTH = 5;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w.-]*)*/?$");
    private static final String[] VALID_IMAGE_TYPES = {
        "thumbnail", "detail", "zoom", "lifestyle", "360", "texture", "variant"
    };
    
    // Fields as per specifications
    private Product product;
    private String url;
    private String altText;
    private Integer sortOrder;
    private Boolean isPrimary;
    private String imageType;
    private Long fileSize;
    private String mimeType;
    
    /**
     * Default constructor for ProductImage.
     */
    public ProductImage() {
        this.sortOrder = 999; // Default to end of list
        this.isPrimary = false;
    }
    
    /**
     * Constructor with essential fields.
     * 
     * @param product  Associated product
     * @param url      URL of the image
     * @param altText  Alternative text for accessibility
     * @param imageType Type of image (thumbnail, detail, zoom)
     */
    public ProductImage(Product product, String url, String altText, String imageType) {
        this();
        this.product = product;
        this.url = url;
        this.altText = validateAndSanitizeAltText(altText);
        this.imageType = validateImageType(imageType);
    }
    
    /**
     * Full constructor for ProductImage.
     * 
     * @param product    Associated product
     * @param url        URL of the image
     * @param altText    Alternative text for accessibility
     * @param sortOrder  Order in product gallery
     * @param isPrimary  Flag indicating if this is primary product image
     * @param imageType  Type of image (thumbnail, detail, zoom)
     * @param fileSize   File size in bytes
     * @param mimeType   MIME type of the image
     */
    public ProductImage(Product product, String url, String altText, Integer sortOrder, 
                        Boolean isPrimary, String imageType, Long fileSize, String mimeType) {
        this.product = product;
        this.url = url;
        this.altText = validateAndSanitizeAltText(altText);
        this.sortOrder = sortOrder;
        this.isPrimary = isPrimary;
        this.imageType = validateImageType(imageType);
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        
        validateState();
    }
    
    /**
     * Get the associated product.
     * 
     * @return the product
     */
    public Product getProduct() {
        return product;
    }
    
    /**
     * Set the associated product.
     * 
     * @param product the product to set
     */
    public void setProduct(Product product) {
        this.product = product;
    }
    
    /**
     * Get the image URL.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the image URL.
     * URL validation is performed to ensure format correctness.
     * 
     * @param url the url to set
     */
    public void setUrl(String url) {
        // Validate URL
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }
        
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            // FIXME: URL validation is too strict and may reject valid URLs in some edge cases
            // Consider using a more robust URL validation library
            
            // For now, log warning but accept the URL
            System.out.println("Warning: URL format may be invalid: " + url);
        }
        
        this.url = url;
    }
    
    /**
     * Get the alternative text.
     * 
     * @return the altText
     */
    public String getAltText() {
        return altText;
    }
    
    /**
     * Set the alternative text for accessibility.
     * 
     * @param altText the altText to set
     */
    public void setAltText(String altText) {
        this.altText = validateAndSanitizeAltText(altText);
    }
    
    /**
     * Get the sort order in product gallery.
     * 
     * @return the sortOrder
     */
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    /**
     * Set the sort order in product gallery.
     * 
     * @param sortOrder the sortOrder to set
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    /**
     * Check if this is the primary image.
     * 
     * @return true if this is the primary image, false otherwise
     */
    public Boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimary);
    }
    
    /**
     * Set primary image flag.
     * 
     * @param primary the primary flag to set
     */
    public void setPrimary(Boolean primary) {
        this.isPrimary = primary;
        
        // If this becomes primary, update product's primary image if needed
        if (Boolean.TRUE.equals(primary) && product != null) {
            // TODO: Implement logic to ensure only one image is primary for a given product
            // This requires access to product's image collection or a dedicated service
        }
    }
    
    /**
     * Get the image type.
     * 
     * @return the imageType
     */
    public String getImageType() {
        return imageType;
    }
    
    /**
     * Set the image type.
     * 
     * @param imageType the imageType to set
     */
    public void setImageType(String imageType) {
        this.imageType = validateImageType(imageType);
    }
    
    /**
     * Get the file size in bytes.
     * 
     * @return the fileSize
     */
    public Long getFileSize() {
        return fileSize;
    }
    
    /**
     * Set the file size in bytes.
     * 
     * @param fileSize the fileSize to set
     */
    public void setFileSize(Long fileSize) {
        if (fileSize != null && fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        this.fileSize = fileSize;
    }
    
    /**
     * Get the MIME type of the image.
     * 
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Set the MIME type of the image.
     * 
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        // Basic validation for common image MIME types
        if (mimeType != null && !mimeType.isEmpty() && 
                !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException(
                "Invalid MIME type: " + mimeType + ". MIME type should start with 'image/'");
        }
        this.mimeType = mimeType;
    }
    
    /**
     * Validates and sanitizes the alternative text.
     * 
     * @param text the alt text to validate
     * @return sanitized alt text
     */
    private String validateAndSanitizeAltText(String text) {
        if (text == null) {
            return "";
        }
        
        // Trim and clean the text
        String cleaned = text.trim();
        
        // Check length constraints
        if (cleaned.length() > MAX_ALT_TEXT_LENGTH) {
            // FIXME: Truncating may not be the best approach for all cases
            cleaned = cleaned.substring(0, MAX_ALT_TEXT_LENGTH);
        }
        
        // Check for very short alt text that might not be descriptive enough
        if (!cleaned.isEmpty() && cleaned.length() < MIN_ALT_TEXT_LENGTH) {
            // Log warning but accept the text
            System.out.println("Warning: Alt text may be too short for adequate description: " + cleaned);
        }
        
        return cleaned;
    }
    
    /**
     * Validates the image type.
     * 
     * @param type the image type to validate
     * @return validated image type
     */
    private String validateImageType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "detail"; // Default type
        }
        
        String normalizedType = type.trim().toLowerCase();
        
        // Check against valid types
        for (String validType : VALID_IMAGE_TYPES) {
            if (validType.equals(normalizedType)) {
                return normalizedType;
            }
        }
        
        // If not found, return default with a warning
        System.out.println("Warning: Invalid image type: " + type + 
                ". Using default type 'detail' instead.");
        return "detail";
    }
    
    /**
     * Validates the overall state of the object.
     * This performs complex validation involving multiple fields.
     */
    private void validateState() {
        // At least URL must be present
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("Product image must have a URL");
        }
        
        // Validate relationships
        if (product == null) {
            throw new IllegalStateException("Product image must be associated with a product");
        }
        
        // Perform complex validation involving multiple fields
        if (Boolean.TRUE.equals(isPrimary) && !"detail".equals(imageType)) {
            // Primary images should typically be detail images
            System.out.println("Warning: Primary image is not of type 'detail'. This may affect display quality.");
        }
        
        // Check for consistency between file size and MIME type
        if (fileSize != null && fileSize > 0 && (mimeType == null || mimeType.trim().isEmpty())) {
            System.out.println("Warning: File size is specified but MIME type is missing");
        }
    }

    /**
     * Utility method to generate a hash code based on image content.
     * This can be used for detecting duplicate images.
     * 
     * @return a hash code that represents the image content
     */
    public String generateContentHash() {
        // This is a simplified mock implementation
        // In a real system, this would analyze the actual image content
        
        if (url == null) {
            return "unknown";
        }
        
        // FIXME: This is not a real content hash - just for demonstration
        // A real implementation would download and hash the image
        StringBuilder hashBuilder = new StringBuilder();
        hashBuilder.append(url.hashCode());
        
        if (fileSize != null) {
            hashBuilder.append("-").append(fileSize);
        }
        
        // Add some randomness to simulate content differences
        Random random = new Random(url.hashCode());
        hashBuilder.append("-").append(Math.abs(random.nextInt(1000)));
        
        return hashBuilder.toString();
    }
    
    /**
     * Estimates the download time in seconds based on file size.
     * 
     * @param connectionSpeedKbps connection speed in Kbps
     * @return estimated download time in seconds, or -1 if file size unknown
     */
    public double estimateDownloadTime(int connectionSpeedKbps) {
        if (fileSize == null || fileSize <= 0 || connectionSpeedKbps <= 0) {
            return -1;
        }
        
        // Convert bytes to bits and calculate
        double fileSizeBits = fileSize * 8;
        double speedBps = connectionSpeedKbps * 1024; // Convert to bps
        
        return fileSizeBits / speedBps;
    }
    
    /**
     * Creates a variant of this image with different dimensions.
     * 
     * @param width the width of the variant
     * @param height the height of the variant
     * @return a new ProductImage with the modified URL
     */
    public ProductImage createVariant(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        
        // Create a new variant with modified URL
        ProductImage variant = new ProductImage();
        variant.setProduct(this.product);
        
        // Modify URL to include dimensions (this is a simplistic approach)
        // FIXME: This assumes a specific URL structure that may not be valid for all systems
        String variantUrl = this.url;
        if (variantUrl.contains("?")) {
            variantUrl += "&width=" + width + "&height=" + height;
        } else {
            variantUrl += "?width=" + width + "&height=" + height;
        }
        variant.setUrl(variantUrl);
        
        // Copy other properties
        variant.setAltText(this.altText);
        variant.setImageType("variant");
        variant.setSortOrder(this.sortOrder != null ? this.sortOrder + 1 : 1000);
        variant.setPrimary(false);
        variant.setMimeType(this.mimeType);
        
        // TODO: Calculate new file size based on dimensions
        
        return variant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        ProductImage that = (ProductImage) o;
        
        // Two product images are considered equal if they have the same URL and product
        return Objects.equals(url, that.url) &&
               (product != null && that.product != null && 
                Objects.equals(product.getId(), that.product.getId()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, 
                (product != null ? product.getId() : 0));
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "id=" + getId() +
                ", product=" + (product != null ? product.getId() : "null") +
                ", url='" + url + '\'' +
                ", imageType='" + imageType + '\'' +
                ", isPrimary=" + isPrimary +
                ", sortOrder=" + sortOrder +
                '}';
    }
}