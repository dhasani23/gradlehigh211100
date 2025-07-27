package com.gradlehigh211100.common.dto;

/**
 * Represents a pageable request with pagination and sorting parameters.
 * This class is used to request a specific page of data with sorting options.
 */
public class PageableRequest {

    // Default values
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String DEFAULT_SORT_DIRECTION = "asc";

    // Current page number (zero-based)
    private int page = DEFAULT_PAGE;
    
    // Number of items per page
    private int size = DEFAULT_SIZE;
    
    // Field used for sorting
    private String sortBy;
    
    // Sort direction (asc/desc)
    private String sortDirection = DEFAULT_SORT_DIRECTION;

    /**
     * Default constructor
     */
    public PageableRequest() {
        // Use default values
    }

    /**
     * Constructor with page and size parameters
     *
     * @param page the page number (zero-based)
     * @param size the page size
     */
    public PageableRequest(int page, int size) {
        this.page = Math.max(0, page);
        this.size = Math.max(1, size);
    }

    /**
     * Full constructor with all parameters
     *
     * @param page the page number (zero-based)
     * @param size the page size
     * @param sortBy the field to sort by
     * @param sortDirection the sort direction
     */
    public PageableRequest(int page, int size, String sortBy, String sortDirection) {
        this(page, size);
        this.sortBy = sortBy;
        
        if (sortDirection != null && 
            (sortDirection.equalsIgnoreCase("asc") || sortDirection.equalsIgnoreCase("desc"))) {
            this.sortDirection = sortDirection.toLowerCase();
        }
    }

    /**
     * Gets the page number
     *
     * @return the page number
     */
    public int getPage() {
        return page;
    }

    /**
     * Sets the page number
     *
     * @param page the page number to set
     * @return this request for chaining
     */
    public PageableRequest setPage(int page) {
        this.page = Math.max(0, page);
        return this;
    }

    /**
     * Gets the page size
     *
     * @return the page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the page size
     *
     * @param size the page size to set
     * @return this request for chaining
     */
    public PageableRequest setSize(int size) {
        this.size = Math.max(1, size);
        return this;
    }

    /**
     * Gets the sort field
     *
     * @return the sort field
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort field
     *
     * @param sortBy the sort field to set
     * @return this request for chaining
     */
    public PageableRequest setSortBy(String sortBy) {
        this.sortBy = sortBy;
        return this;
    }

    /**
     * Gets the sort direction
     *
     * @return the sort direction
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets the sort direction
     *
     * @param sortDirection the sort direction to set
     * @return this request for chaining
     */
    public PageableRequest setSortDirection(String sortDirection) {
        if (sortDirection != null && 
            (sortDirection.equalsIgnoreCase("asc") || sortDirection.equalsIgnoreCase("desc"))) {
            this.sortDirection = sortDirection.toLowerCase();
        }
        return this;
    }
    
    /**
     * Creates a new PageableRequest for the next page with the same size and sorting
     *
     * @return a new PageableRequest for the next page
     */
    public PageableRequest next() {
        return new PageableRequest(page + 1, size, sortBy, sortDirection);
    }
    
    /**
     * Creates a new PageableRequest for the previous page with the same size and sorting
     *
     * @return a new PageableRequest for the previous page
     */
    public PageableRequest previous() {
        return new PageableRequest(Math.max(0, page - 1), size, sortBy, sortDirection);
    }

    /**
     * Creates a new PageableRequest for the first page with the same size and sorting
     *
     * @return a new PageableRequest for the first page
     */
    public PageableRequest first() {
        return new PageableRequest(0, size, sortBy, sortDirection);
    }

    @Override
    public String toString() {
        return "PageableRequest{" +
               "page=" + page +
               ", size=" + size +
               ", sortBy='" + sortBy + '\'' +
               ", sortDirection='" + sortDirection + '\'' +
               '}';
    }
}