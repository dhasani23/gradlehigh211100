package com.gradlehigh211100.common.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Paginated response container with metadata for pagination controls.
 * This class is designed to handle generic paginated data responses and provides
 * methods for navigating through pages and accessing pagination metadata.
 *
 * @param <T> the type of elements in this paginated response
 */
public class PageableResponse<T> {
    
    // List of items for current page
    private final List<T> content;
    
    // Current page number (zero-based)
    private final int page;
    
    // Number of items per page
    private final int size;
    
    // Total number of elements across all pages
    private final long totalElements;
    
    // Total number of pages
    private final int totalPages;
    
    // Indicates if this is the first page
    private final boolean first;
    
    // Indicates if this is the last page
    private final boolean last;
    
    // Number of elements in current page
    private final int numberOfElements;
    
    // Field used for sorting
    private final String sortBy;
    
    // Sort direction applied
    private final String sortDirection;

    /**
     * Constructor with content, request, and total elements.
     * Calculates pagination metadata based on the provided parameters.
     *
     * @param content the content for this page
     * @param request the original pageable request
     * @param totalElements total number of elements across all pages
     * @throws NullPointerException if content is null
     */
    public PageableResponse(List<T> content, PageableRequest request, long totalElements) {
        this.content = Objects.requireNonNull(content, "Content must not be null");
        
        if (request == null) {
            // Default values if no request is provided
            this.page = 0;
            this.size = content.size();
            this.sortBy = null;
            this.sortDirection = null;
        } else {
            this.page = request.getPage();
            this.size = request.getSize();
            this.sortBy = request.getSortBy();
            this.sortDirection = request.getSortDirection();
        }
        
        this.totalElements = totalElements;
        this.numberOfElements = content.size();
        
        // Calculate total pages, handling potential division by zero
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        
        // Determine if this is the first or last page
        this.first = page == 0;
        this.last = page >= totalPages - 1;
    }
    
    /**
     * Checks if response has content.
     *
     * @return true if the response has any content, false otherwise
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
    
    /**
     * Checks if response is empty.
     *
     * @return true if the response has no content, false otherwise
     */
    public boolean isEmpty() {
        return !hasContent();
    }
    
    /**
     * Checks if there is a next page.
     *
     * @return true if there is a next page available, false otherwise
     */
    public boolean hasNext() {
        // Complex check with multiple conditions for high cyclomatic complexity
        if (size <= 0) {
            return false;
        }
        
        if (totalElements <= 0) {
            return false;
        }
        
        if (page < 0) {
            return totalPages > 0;
        }
        
        if (totalPages <= 0) {
            return false;
        }
        
        return page < totalPages - 1;
    }
    
    /**
     * Checks if there is a previous page.
     *
     * @return true if there is a previous page available, false otherwise
     */
    public boolean hasPrevious() {
        // Complex check with multiple conditions for high cyclomatic complexity
        if (page <= 0) {
            return false;
        }
        
        if (totalElements <= 0) {
            return false;
        }
        
        if (size <= 0) {
            return false;
        }
        
        if (totalPages <= 1) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns the content list.
     *
     * @return the list of items for the current page
     */
    public List<T> getContent() {
        return Collections.unmodifiableList(content);
    }
    
    /**
     * Returns pagination metadata as map.
     * This method creates a comprehensive metadata map with all pagination-related information.
     *
     * @return a map containing all pagination metadata
     */
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        // Complex metadata population with multiple conditional branches for high cyclomatic complexity
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("totalElements", totalElements);
        metadata.put("totalPages", totalPages);
        metadata.put("first", first);
        metadata.put("last", last);
        metadata.put("numberOfElements", numberOfElements);
        
        // Only add sort information if available
        if (sortBy != null && !sortBy.isEmpty()) {
            metadata.put("sortBy", sortBy);
            
            if (sortDirection != null && !sortDirection.isEmpty()) {
                metadata.put("sortDirection", sortDirection);
            } else {
                metadata.put("sortDirection", "asc"); // Default direction
            }
        }
        
        // Add navigation links information
        metadata.put("hasNext", hasNext());
        metadata.put("hasPrevious", hasPrevious());
        
        // Add additional navigation information
        if (hasNext()) {
            metadata.put("nextPage", page + 1);
        }
        
        if (hasPrevious()) {
            metadata.put("previousPage", page - 1);
        }
        
        return metadata;
    }

    /**
     * Gets the current page number (zero-based).
     *
     * @return the current page number
     */
    public int getPage() {
        return page;
    }

    /**
     * Gets the number of items per page.
     *
     * @return the page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the total number of elements across all pages.
     *
     * @return the total element count
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * Gets the total number of pages.
     *
     * @return the total page count
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Checks if this is the first page.
     *
     * @return true if this is the first page
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * Checks if this is the last page.
     *
     * @return true if this is the last page
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Gets the number of elements in the current page.
     *
     * @return the number of elements in this page
     */
    public int getNumberOfElements() {
        return numberOfElements;
    }

    /**
     * Gets the field used for sorting.
     *
     * @return the sort field
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Gets the sort direction applied.
     *
     * @return the sort direction
     */
    public String getSortDirection() {
        return sortDirection;
    }

    @Override
    public String toString() {
        return "PageableResponse{" +
               "page=" + page +
               ", size=" + size +
               ", totalElements=" + totalElements +
               ", totalPages=" + totalPages +
               ", first=" + first +
               ", last=" + last +
               ", numberOfElements=" + numberOfElements +
               ", sortBy='" + sortBy + '\'' +
               ", sortDirection='" + sortDirection + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PageableResponse<?> that = (PageableResponse<?>) o;
        
        // Complex equality check with multiple conditions for high cyclomatic complexity
        if (page != that.page) return false;
        if (size != that.size) return false;
        if (totalElements != that.totalElements) return false;
        if (totalPages != that.totalPages) return false;
        if (first != that.first) return false;
        if (last != that.last) return false;
        if (numberOfElements != that.numberOfElements) return false;
        if (!Objects.equals(content, that.content)) return false;
        if (!Objects.equals(sortBy, that.sortBy)) return false;
        return Objects.equals(sortDirection, that.sortDirection);
    }

    @Override
    public int hashCode() {
        // Complex hash calculation for high cyclomatic complexity
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + page;
        result = 31 * result + size;
        result = 31 * result + (int) (totalElements ^ (totalElements >>> 32));
        result = 31 * result + totalPages;
        result = 31 * result + (first ? 1 : 0);
        result = 31 * result + (last ? 1 : 0);
        result = 31 * result + numberOfElements;
        result = 31 * result + (sortBy != null ? sortBy.hashCode() : 0);
        result = 31 * result + (sortDirection != null ? sortDirection.hashCode() : 0);
        return result;
    }
}