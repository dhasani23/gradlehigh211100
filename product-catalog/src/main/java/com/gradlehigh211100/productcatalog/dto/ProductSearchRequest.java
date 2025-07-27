package com.gradlehigh211100.productcatalog.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data transfer object for product search requests containing search criteria,
 * filters, and pagination parameters.
 * 
 * This class encapsulates all parameters needed for product search operations,
 * including text search, category filtering, price ranges, product attributes, 
 * sorting, and pagination.
 */
public class ProductSearchRequest {
    
    // Search query fields
    private String query;
    private Long categoryId;
    
    // Price range filters
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    // Product attribute filters
    private List<String> brands;
    private List<String> sizes;
    private List<String> colors;
    
    // Sorting parameters
    private String sortBy;
    private String sortOrder;
    
    // Pagination parameters
    private Integer page;
    private Integer size;
    
    /**
     * Default constructor initializing collections and default values
     */
    public ProductSearchRequest() {
        this.brands = new ArrayList<>();
        this.sizes = new ArrayList<>();
        this.colors = new ArrayList<>();
        this.page = 0;
        this.size = 20;
        this.sortOrder = "ASC";
    }
    
    /**
     * Constructor with search query parameter
     * 
     * @param query the search query text
     */
    public ProductSearchRequest(String query) {
        this();
        this.query = query;
    }
    
    /**
     * Constructor with search query and category parameters
     * 
     * @param query the search query text
     * @param categoryId the category ID filter
     */
    public ProductSearchRequest(String query, Long categoryId) {
        this(query);
        this.categoryId = categoryId;
    }
    
    /**
     * Full constructor with all parameters
     * 
     * @param query search query text
     * @param categoryId category ID filter
     * @param minPrice minimum price filter
     * @param maxPrice maximum price filter
     * @param brands list of brand filters
     * @param sizes list of size filters
     * @param colors list of color filters
     * @param sortBy field to sort by
     * @param sortOrder sort direction (ASC/DESC)
     * @param page page number
     * @param size page size
     */
    public ProductSearchRequest(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                               List<String> brands, List<String> sizes, List<String> colors, 
                               String sortBy, String sortOrder, Integer page, Integer size) {
        this.query = query;
        this.categoryId = categoryId;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.brands = brands != null ? brands : new ArrayList<>();
        this.sizes = sizes != null ? sizes : new ArrayList<>();
        this.colors = colors != null ? colors : new ArrayList<>();
        this.sortBy = sortBy;
        this.sortOrder = sortOrder != null ? sortOrder : "ASC";
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 20;
    }
    
    /**
     * Get search query
     * 
     * @return the search query text
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * Set search query
     * 
     * @param query the search query text to set
     */
    public void setQuery(String query) {
        this.query = query;
    }
    
    /**
     * Get category ID
     * 
     * @return the category ID filter
     */
    public Long getCategoryId() {
        return categoryId;
    }
    
    /**
     * Set category ID
     * 
     * @param categoryId the category ID to set
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    /**
     * Get minimum price filter
     * 
     * @return the minimum price
     */
    public BigDecimal getMinPrice() {
        return minPrice;
    }
    
    /**
     * Set minimum price filter
     * 
     * @param minPrice the minimum price to set
     */
    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }
    
    /**
     * Get maximum price filter
     * 
     * @return the maximum price
     */
    public BigDecimal getMaxPrice() {
        return maxPrice;
    }
    
    /**
     * Set maximum price filter
     * 
     * @param maxPrice the maximum price to set
     */
    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }
    
    /**
     * Get brand filters
     * 
     * @return list of brand filters
     */
    public List<String> getBrands() {
        return brands;
    }
    
    /**
     * Set brand filters
     * 
     * @param brands the list of brands to filter by
     */
    public void setBrands(List<String> brands) {
        this.brands = brands != null ? brands : new ArrayList<>();
    }
    
    /**
     * Add a brand to the brand filter list
     * 
     * @param brand the brand name to add to filters
     */
    public void addBrandFilter(String brand) {
        if (brand != null && !brand.trim().isEmpty()) {
            if (this.brands == null) {
                this.brands = new ArrayList<>();
            }
            
            // Check for duplicates using case-insensitive comparison
            boolean isDuplicate = false;
            for (String existingBrand : this.brands) {
                if (existingBrand.equalsIgnoreCase(brand)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                this.brands.add(brand);
            }
        }
    }
    
    /**
     * Get size filters
     * 
     * @return list of size filters
     */
    public List<String> getSizes() {
        return sizes;
    }
    
    /**
     * Set size filters
     * 
     * @param sizes the list of sizes to filter by
     */
    public void setSizes(List<String> sizes) {
        this.sizes = sizes != null ? sizes : new ArrayList<>();
    }
    
    /**
     * Add a size to the size filter list
     * 
     * @param size the size to add to filters
     */
    public void addSizeFilter(String size) {
        if (size != null && !size.trim().isEmpty()) {
            if (this.sizes == null) {
                this.sizes = new ArrayList<>();
            }
            
            if (!this.sizes.contains(size)) {
                this.sizes.add(size);
            }
        }
    }
    
    /**
     * Get color filters
     * 
     * @return list of color filters
     */
    public List<String> getColors() {
        return colors;
    }
    
    /**
     * Set color filters
     * 
     * @param colors the list of colors to filter by
     */
    public void setColors(List<String> colors) {
        this.colors = colors != null ? colors : new ArrayList<>();
    }
    
    /**
     * Add a color to the color filter list
     * 
     * @param color the color to add to filters
     */
    public void addColorFilter(String color) {
        if (color != null && !color.trim().isEmpty()) {
            if (this.colors == null) {
                this.colors = new ArrayList<>();
            }
            
            // Normalize color name and check for duplicates
            color = color.toLowerCase().trim();
            if (!this.colors.contains(color)) {
                this.colors.add(color);
            }
        }
    }
    
    /**
     * Get sort field
     * 
     * @return the field to sort by
     */
    public String getSortBy() {
        return sortBy;
    }
    
    /**
     * Set sort field
     * 
     * @param sortBy the field to sort by
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    /**
     * Get sort order
     * 
     * @return the sort order (ASC/DESC)
     */
    public String getSortOrder() {
        return sortOrder;
    }
    
    /**
     * Set sort order
     * 
     * @param sortOrder the sort order (ASC/DESC)
     */
    public void setSortOrder(String sortOrder) {
        // Validate and normalize sort order
        if (sortOrder != null) {
            if (sortOrder.equalsIgnoreCase("ASC") || 
                sortOrder.equalsIgnoreCase("ascending")) {
                this.sortOrder = "ASC";
            } else if (sortOrder.equalsIgnoreCase("DESC") || 
                       sortOrder.equalsIgnoreCase("descending")) {
                this.sortOrder = "DESC";
            } else {
                // FIXME: Consider throwing IllegalArgumentException for invalid sort order
                this.sortOrder = "ASC"; // Default to ascending for invalid values
            }
        } else {
            this.sortOrder = "ASC"; // Default value
        }
    }
    
    /**
     * Get page number
     * 
     * @return the page number for pagination
     */
    public Integer getPage() {
        return page;
    }
    
    /**
     * Set page number
     * 
     * @param page the page number to set
     */
    public void setPage(Integer page) {
        // Ensure page is never negative
        this.page = (page != null && page >= 0) ? page : 0;
    }
    
    /**
     * Get page size
     * 
     * @return the page size for pagination
     */
    public Integer getSize() {
        return size;
    }
    
    /**
     * Set page size
     * 
     * @param size the page size to set
     */
    public void setSize(Integer size) {
        // Ensure size is always positive and within reasonable bounds
        if (size != null && size > 0) {
            // TODO: Consider implementing a max page size check based on system capacity
            this.size = Math.min(size, 1000); // Set arbitrary upper limit to prevent resource exhaustion
        } else {
            this.size = 20; // Default value
        }
    }
    
    /**
     * Clear all search filters but maintain search query and pagination settings
     */
    public void clearFilters() {
        this.categoryId = null;
        this.minPrice = null;
        this.maxPrice = null;
        
        if (this.brands != null) {
            this.brands.clear();
        } else {
            this.brands = new ArrayList<>();
        }
        
        if (this.sizes != null) {
            this.sizes.clear();
        } else {
            this.sizes = new ArrayList<>();
        }
        
        if (this.colors != null) {
            this.colors.clear();
        } else {
            this.colors = new ArrayList<>();
        }
    }
    
    /**
     * Check if any filters are applied
     * 
     * @return true if any filter is set, false otherwise
     */
    public boolean hasFilters() {
        return categoryId != null
            || minPrice != null
            || maxPrice != null
            || (brands != null && !brands.isEmpty())
            || (sizes != null && !sizes.isEmpty())
            || (colors != null && !colors.isEmpty());
    }
    
    /**
     * Calculate approximate memory footprint of this request object
     * 
     * @return approximate memory size in bytes
     */
    public int calculateMemoryFootprint() {
        int size = 0;
        
        // Base object overhead (approx. 16 bytes)
        size += 16;
        
        // String references (8 bytes each)
        size += 8 * 3; // query, sortBy, sortOrder
        
        // Object references (8 bytes each)
        size += 8 * 8; // categoryId, minPrice, maxPrice, brands, sizes, colors, page, size
        
        // String content (2 bytes per char + 24 bytes overhead per String)
        if (query != null) size += 24 + (query.length() * 2);
        if (sortBy != null) size += 24 + (sortBy.length() * 2);
        if (sortOrder != null) size += 24 + (sortOrder.length() * 2);
        
        // Collections (24 bytes overhead + 8 bytes per element)
        if (brands != null) {
            size += 24 + (8 * brands.size());
            // Add string content size
            for (String brand : brands) {
                if (brand != null) {
                    size += 24 + (brand.length() * 2);
                }
            }
        }
        
        if (sizes != null) {
            size += 24 + (8 * sizes.size());
            // Add string content size
            for (String sizeValue : sizes) {
                if (sizeValue != null) {
                    size += 24 + (sizeValue.length() * 2);
                }
            }
        }
        
        if (colors != null) {
            size += 24 + (8 * colors.size());
            // Add string content size
            for (String color : colors) {
                if (color != null) {
                    size += 24 + (color.length() * 2);
                }
            }
        }
        
        return size;
    }
    
    /**
     * Validates if the request parameters are within acceptable ranges
     * 
     * @return true if request parameters are valid, false otherwise
     */
    public boolean validateRequest() {
        boolean isValid = true;
        
        // Check price range validity
        if (minPrice != null && maxPrice != null) {
            isValid = minPrice.compareTo(maxPrice) <= 0;
        }
        
        // Check pagination parameters
        if (page != null && page < 0) {
            isValid = false;
        }
        
        if (size != null && (size <= 0 || size > 1000)) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Helper method to normalize string filters before applying
     * 
     * @param value the string to normalize
     * @return normalized string or null if input is null/empty
     */
    private String normalizeFilterString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        // Perform custom normalization logic (e.g., lowercase, trim whitespace)
        String normalized = value.trim().toLowerCase();
        
        // Additional processing could be done based on specific filtering needs
        // FIXME: Consider additional sanitization to prevent SQL injection in filter values
        
        return normalized;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProductSearchRequest that = (ProductSearchRequest) o;
        
        return Objects.equals(query, that.query) &&
               Objects.equals(categoryId, that.categoryId) &&
               Objects.equals(minPrice, that.minPrice) &&
               Objects.equals(maxPrice, that.maxPrice) &&
               Objects.equals(brands, that.brands) &&
               Objects.equals(sizes, that.sizes) &&
               Objects.equals(colors, that.colors) &&
               Objects.equals(sortBy, that.sortBy) &&
               Objects.equals(sortOrder, that.sortOrder) &&
               Objects.equals(page, that.page) &&
               Objects.equals(size, that.size);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(query, categoryId, minPrice, maxPrice, brands, sizes, 
                           colors, sortBy, sortOrder, page, size);
    }
    
    @Override
    public String toString() {
        return "ProductSearchRequest{" +
               "query='" + query + '\'' +
               ", categoryId=" + categoryId +
               ", minPrice=" + minPrice +
               ", maxPrice=" + maxPrice +
               ", brands=" + brands +
               ", sizes=" + sizes +
               ", colors=" + colors +
               ", sortBy='" + sortBy + '\'' +
               ", sortOrder='" + sortOrder + '\'' +
               ", page=" + page +
               ", size=" + size +
               '}';
    }
}