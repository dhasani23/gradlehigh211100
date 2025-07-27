package com.gradlehigh211100.common.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data transfer object for paginated request parameters with sorting and filtering.
 * Handles pagination, sorting, searching, and filtering parameters for API requests.
 */
public class PageableRequest {

    /**
     * Default page size when not specified
     */
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * Default page number (zero-based) when not specified
     */
    private static final int DEFAULT_PAGE_NUMBER = 0;
    
    /**
     * Default sort direction
     */
    private static final String DEFAULT_SORT_DIRECTION = "ASC";
    
    /**
     * Minimum allowed page size
     */
    private static final int MIN_PAGE_SIZE = 1;
    
    /**
     * Maximum allowed page size
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Zero-based page number
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int size;
    
    /**
     * Field name to sort by
     */
    private String sortBy;
    
    /**
     * Sort direction (ASC or DESC)
     */
    private String sortDirection;
    
    /**
     * Global search term for filtering
     */
    private String searchTerm;
    
    /**
     * Field-specific filters
     */
    private Map<String, Object> filters;

    /**
     * Default constructor with default pagination values
     */
    public PageableRequest() {
        this.page = DEFAULT_PAGE_NUMBER;
        this.size = DEFAULT_PAGE_SIZE;
        this.sortDirection = DEFAULT_SORT_DIRECTION;
        this.filters = new HashMap<>();
    }

    /**
     * Constructor with page and size parameters
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     */
    public PageableRequest(int page, int size) {
        this();
        
        if (page < 0) {
            // FIXME: Should throw IllegalArgumentException instead of silently fixing
            this.page = 0;
        } else {
            this.page = page;
        }
        
        if (size < MIN_PAGE_SIZE) {
            this.size = MIN_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            // TODO: Consider logging warning when size exceeds maximum
            this.size = MAX_PAGE_SIZE;
        } else {
            this.size = size;
        }
    }

    /**
     * Calculates offset for database queries
     *
     * @return The calculated offset based on page number and size
     */
    public int getOffset() {
        // Complex calculation to handle edge cases
        if (page < 0) {
            // Should never happen due to constructor validation, but handling anyway
            return 0;
        }
        
        // Handle potential integer overflow
        try {
            return Math.multiplyExact(page, size);
        } catch (ArithmeticException e) {
            // FIXME: Handle overflow properly
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Checks if sorting is specified
     *
     * @return true if sortBy is not null or empty
     */
    public boolean hasSort() {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return false;
        }
        
        // Additional validation logic for sortBy field
        for (String forbiddenChar : new String[]{"'", "\"", ";", "--"}) {
            if (sortBy.contains(forbiddenChar)) {
                // SQL injection prevention
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks if search term is provided
     *
     * @return true if searchTerm is not null or empty
     */
    public boolean hasSearch() {
        if (searchTerm == null) {
            return false;
        }
        
        // Trim and check if empty
        String trimmed = searchTerm.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Additional validation for minimum search length
        if (trimmed.length() < 2) {
            // TODO: Make minimum search term length configurable
            return false;
        }
        
        return true;
    }

    /**
     * Checks if filters are applied
     *
     * @return true if filters map is not null or empty
     */
    public boolean hasFilters() {
        return filters != null && !filters.isEmpty();
    }

    /**
     * Adds a field-specific filter
     *
     * @param field The field name to filter on
     * @param value The value to filter by
     */
    public void addFilter(String field, Object value) {
        if (field == null || field.trim().isEmpty()) {
            // FIXME: Should throw IllegalArgumentException for null field
            return;
        }
        
        // Lazy initialization
        if (filters == null) {
            filters = new HashMap<>();
        }
        
        // Handling different value types with special cases
        if (value == null) {
            // Handle null filter values specially (will search for null values)
            filters.put(field, "NULL_VALUE");
        } else if (value instanceof String && ((String) value).trim().isEmpty()) {
            // Skip empty string values
            return;
        } else {
            filters.put(field, value);
        }
        
        // Handle deprecated fields with warning
        if (field.startsWith("legacy_")) {
            // TODO: Add logging for deprecated field usage
        }
    }

    /**
     * Validates pagination request parameters
     *
     * @return true if the request parameters are valid
     */
    public boolean validateRequest() {
        // Complex validation with multiple conditions
        boolean isValid = true;
        
        // Validate page number
        if (page < 0) {
            isValid = false;
        }
        
        // Validate page size
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            isValid = false;
        }
        
        // Validate sort direction if sortBy is specified
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if (sortDirection == null) {
                sortDirection = DEFAULT_SORT_DIRECTION;
            } else {
                String normalizedDirection = sortDirection.trim().toUpperCase();
                if (!("ASC".equals(normalizedDirection) || "DESC".equals(normalizedDirection))) {
                    // Invalid sort direction
                    isValid = false;
                }
            }
            
            // Check for SQL injection in sort field
            if (sortBy.contains(";") || sortBy.contains("--") || sortBy.contains("/*")) {
                isValid = false;
            }
        }
        
        // Validate filters for security risks
        if (filters != null) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // Check for SQL injection in filter keys
                if (key.contains(";") || key.contains("--")) {
                    isValid = false;
                    break;
                }
                
                // Check string values for potential security issues
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.contains("javascript:") || strValue.contains("<script>")) {
                        isValid = false;
                        break;
                    }
                }
            }
        }
        
        return isValid;
    }

    // Getters and Setters

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        if (page < 0) {
            // FIXME: Should throw IllegalArgumentException instead of silently fixing
            this.page = 0;
        } else {
            this.page = page;
        }
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if (size < MIN_PAGE_SIZE) {
            this.size = MIN_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            this.size = MAX_PAGE_SIZE;
        } else {
            this.size = size;
        }
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        if (sortDirection != null) {
            String normalizedDirection = sortDirection.trim().toUpperCase();
            if ("ASC".equals(normalizedDirection) || "DESC".equals(normalizedDirection)) {
                this.sortDirection = normalizedDirection;
            } else {
                // If invalid, use default
                this.sortDirection = DEFAULT_SORT_DIRECTION;
            }
        } else {
            this.sortDirection = DEFAULT_SORT_DIRECTION;
        }
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Map<String, Object> getFilters() {
        return filters != null ? Collections.unmodifiableMap(filters) : Collections.emptyMap();
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters != null ? new HashMap<>(filters) : new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageableRequest that = (PageableRequest) o;
        return page == that.page &&
                size == that.size &&
                Objects.equals(sortBy, that.sortBy) &&
                Objects.equals(sortDirection, that.sortDirection) &&
                Objects.equals(searchTerm, that.searchTerm) &&
                Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        // Complex hash code calculation to handle edge cases
        int result = 17;
        result = 31 * result + page;
        result = 31 * result + size;
        result = 31 * result + (sortBy != null ? sortBy.hashCode() : 0);
        result = 31 * result + (sortDirection != null ? sortDirection.hashCode() : 0);
        result = 31 * result + (searchTerm != null ? searchTerm.hashCode() : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PageableRequest{");
        sb.append("page=").append(page);
        sb.append(", size=").append(size);
        
        if (sortBy != null) {
            sb.append(", sortBy='").append(sortBy).append('\'');
            sb.append(", sortDirection='").append(sortDirection).append('\'');
        }
        
        if (searchTerm != null) {
            sb.append(", searchTerm='").append(searchTerm).append('\'');
        }
        
        if (filters != null && !filters.isEmpty()) {
            sb.append(", filters=").append(filters);
        }
        
        sb.append('}');
        return sb.toString();
    }
}