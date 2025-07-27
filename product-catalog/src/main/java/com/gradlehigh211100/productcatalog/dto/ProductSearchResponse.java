package com.gradlehigh211100.productcatalog.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data transfer object for product search responses containing results, pagination, 
 * and faceted search information.
 * 
 * This class encapsulates all information returned from a product search operation,
 * including the matching products, pagination details, and faceted search information
 * for further filtering of results.
 */
public class ProductSearchResponse {
    
    // List of products matching search criteria
    private List<ProductDTO> products;
    
    // Total number of matching products across all pages
    private Long totalResults;
    
    // Current page number in the search results
    private Integer currentPage;
    
    // Total number of available pages
    private Integer totalPages;
    
    // Number of products per page
    private Integer pageSize;
    
    // Faceted search results (category -> list of values)
    private Map<String, List<String>> facets;
    
    // Search term suggestions for query improvement
    private List<String> suggestedTerms;

    /**
     * Default constructor initializing empty collections
     */
    public ProductSearchResponse() {
        this.products = new ArrayList<>();
        this.facets = new HashMap<>();
        this.suggestedTerms = new ArrayList<>();
        this.currentPage = 0;
        this.totalPages = 0;
        this.pageSize = 0;
        this.totalResults = 0L;
    }

    /**
     * Constructor with essential pagination parameters
     * 
     * @param currentPage The current page number
     * @param pageSize The number of items per page
     * @param totalResults The total number of results
     */
    public ProductSearchResponse(Integer currentPage, Integer pageSize, Long totalResults) {
        this();
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalResults = totalResults;
        
        // Calculate total pages based on page size and total results
        calculateTotalPages();
    }
    
    /**
     * Full constructor with all parameters
     * 
     * @param products List of products for this page
     * @param totalResults Total number of matching products
     * @param currentPage Current page number
     * @param totalPages Total number of pages
     * @param pageSize Size of each page
     * @param facets Map of faceted search results
     * @param suggestedTerms List of suggested search terms
     */
    public ProductSearchResponse(List<ProductDTO> products, Long totalResults, 
                               Integer currentPage, Integer totalPages, 
                               Integer pageSize, Map<String, List<String>> facets,
                               List<String> suggestedTerms) {
        this.products = products != null ? products : new ArrayList<>();
        this.totalResults = totalResults;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
        this.facets = facets != null ? facets : new HashMap<>();
        this.suggestedTerms = suggestedTerms != null ? suggestedTerms : new ArrayList<>();
    }
    
    /**
     * Get the list of products in this page of search results
     * 
     * @return List of product DTOs
     */
    public List<ProductDTO> getProducts() {
        return products;
    }
    
    /**
     * Set the list of products in this page of search results
     * 
     * @param products List of product DTOs to set
     */
    public void setProducts(List<ProductDTO> products) {
        this.products = products != null ? products : new ArrayList<>();
    }
    
    /**
     * Get the total number of results matching the search criteria
     * 
     * @return Total number of matching products
     */
    public Long getTotalResults() {
        return totalResults;
    }
    
    /**
     * Set the total number of results matching the search criteria
     * 
     * @param totalResults Total number of matching products
     */
    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
        calculateTotalPages();
    }
    
    /**
     * Get the current page number
     * 
     * @return Current page number
     */
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Set the current page number
     * 
     * @param currentPage Current page number to set
     */
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
    
    /**
     * Get the total number of pages
     * 
     * @return Total number of pages
     */
    public Integer getTotalPages() {
        return totalPages;
    }
    
    /**
     * Set the total number of pages
     * 
     * @param totalPages Total number of pages to set
     */
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    /**
     * Get the page size
     * 
     * @return Number of items per page
     */
    public Integer getPageSize() {
        return pageSize;
    }
    
    /**
     * Set the page size
     * 
     * @param pageSize Number of items per page to set
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        calculateTotalPages();
    }
    
    /**
     * Get faceted search results
     * 
     * @return Map of facet categories to lists of facet values
     */
    public Map<String, List<String>> getFacets() {
        return facets;
    }
    
    /**
     * Set faceted search results
     * 
     * @param facets Map of facet categories to lists of facet values
     */
    public void setFacets(Map<String, List<String>> facets) {
        this.facets = facets != null ? facets : new HashMap<>();
    }
    
    /**
     * Get suggested search terms
     * 
     * @return List of suggested search terms
     */
    public List<String> getSuggestedTerms() {
        return suggestedTerms;
    }
    
    /**
     * Set suggested search terms
     * 
     * @param suggestedTerms List of suggested search terms
     */
    public void setSuggestedTerms(List<String> suggestedTerms) {
        this.suggestedTerms = suggestedTerms != null ? suggestedTerms : new ArrayList<>();
    }
    
    /**
     * Add a facet to search response
     * 
     * @param facetName Name of the facet category
     * @param facetValues List of values for this facet
     */
    public void addFacet(String facetName, List<String> facetValues) {
        // Check for null inputs
        if (facetName == null) {
            throw new IllegalArgumentException("Facet name cannot be null");
        }
        
        // Create facets map if it doesn't exist
        if (facets == null) {
            facets = new HashMap<>();
        }
        
        // Complex branching logic to demonstrate high cyclomatic complexity
        if (facets.containsKey(facetName)) {
            List<String> existingValues = facets.get(facetName);
            
            // Handle the case when facetValues is null
            if (facetValues == null) {
                // Remove this facet if the new values are null
                facets.remove(facetName);
                return;
            }
            
            // Merge with existing values without duplicates
            for (String value : facetValues) {
                if (value != null && !existingValues.contains(value)) {
                    existingValues.add(value);
                }
            }
        } else {
            // Handle null values for new facet
            if (facetValues == null) {
                facets.put(facetName, new ArrayList<>());
            } else {
                // Filter out null values
                List<String> nonNullValues = new ArrayList<>();
                for (String value : facetValues) {
                    if (value != null) {
                        nonNullValues.add(value);
                    }
                }
                facets.put(facetName, nonNullValues);
            }
        }
        
        // FIXME: This method has high cyclomatic complexity and should be refactored
        // for better maintainability
    }
    
    /**
     * Check if there is a next page of results
     * 
     * @return True if there is a next page, false otherwise
     */
    public Boolean hasNextPage() {
        // Implement complex logic to determine if a next page exists
        if (totalPages == null || currentPage == null) {
            return false;
        }
        
        if (totalPages <= 0 || currentPage < 0) {
            return false;
        }
        
        // Handle edge case where totalPages is 0 but we're on page 0
        if (totalPages == 0) {
            return currentPage < 0;
        }
        
        // Check if we're on the last page
        if (currentPage >= totalPages - 1) {
            return false;
        }
        
        // Check if we have any results
        if (totalResults == null || totalResults <= 0) {
            return false;
        }
        
        // Check if we have all results on current page
        if (pageSize != null && 
            pageSize > 0 && 
            currentPage * pageSize >= totalResults) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if there is a previous page of results
     * 
     * @return True if there is a previous page, false otherwise
     */
    public Boolean hasPreviousPage() {
        if (currentPage == null) {
            return false;
        }
        
        return currentPage > 0;
    }
    
    /**
     * Get the next page number if one exists
     * 
     * @return Next page number or null if no next page
     */
    public Integer getNextPageNumber() {
        if (hasNextPage()) {
            return currentPage + 1;
        }
        return null;
    }
    
    /**
     * Get the previous page number if one exists
     * 
     * @return Previous page number or null if no previous page
     */
    public Integer getPreviousPageNumber() {
        if (hasPreviousPage()) {
            return currentPage - 1;
        }
        return null;
    }
    
    /**
     * Calculate the first item index in the current page (0-based)
     * 
     * @return The index of the first item
     */
    public Long getFirstItemIndex() {
        if (currentPage == null || pageSize == null) {
            return 0L;
        }
        return (currentPage * pageSize.longValue());
    }
    
    /**
     * Calculate the last item index in the current page (0-based)
     * 
     * @return The index of the last item
     */
    public Long getLastItemIndex() {
        if (currentPage == null || pageSize == null || totalResults == null) {
            return 0L;
        }
        
        Long lastPossibleIndex = (currentPage + 1) * pageSize.longValue() - 1;
        return Math.min(lastPossibleIndex, totalResults - 1);
    }
    
    /**
     * Add a suggested term to the list of suggested search terms
     * 
     * @param term The term to add
     * @return True if the term was added, false if it already exists
     */
    public boolean addSuggestedTerm(String term) {
        if (term == null) {
            return false;
        }
        
        if (suggestedTerms == null) {
            suggestedTerms = new ArrayList<>();
        }
        
        if (!suggestedTerms.contains(term)) {
            suggestedTerms.add(term);
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculate the total number of pages based on total results and page size
     */
    private void calculateTotalPages() {
        if (totalResults == null || pageSize == null || pageSize <= 0) {
            totalPages = 0;
            return;
        }
        
        // Calculate total pages with ceiling division
        totalPages = (int)Math.ceil((double)totalResults / pageSize);
        
        // TODO: Consider optimizing this calculation for very large result sets
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProductSearchResponse that = (ProductSearchResponse) o;
        
        return Objects.equals(products, that.products) &&
               Objects.equals(totalResults, that.totalResults) &&
               Objects.equals(currentPage, that.currentPage) &&
               Objects.equals(totalPages, that.totalPages) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(facets, that.facets) &&
               Objects.equals(suggestedTerms, that.suggestedTerms);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(products, totalResults, currentPage, totalPages, 
                           pageSize, facets, suggestedTerms);
    }
    
    @Override
    public String toString() {
        return "ProductSearchResponse{" +
               "products=" + (products == null ? "null" : products.size() + " items") +
               ", totalResults=" + totalResults +
               ", currentPage=" + currentPage +
               ", totalPages=" + totalPages +
               ", pageSize=" + pageSize +
               ", facets=" + (facets == null ? "null" : facets.size() + " categories") +
               ", suggestedTerms=" + (suggestedTerms == null ? "null" : suggestedTerms) +
               '}';
    }
}