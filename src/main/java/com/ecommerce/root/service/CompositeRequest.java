package com.ecommerce.root.service;

import java.util.List;
import java.util.Map;

/**
 * CompositeRequest - Represents a request that requires data from multiple services.
 * This model is used for complex requests that involve orchestration across microservices.
 */
public class CompositeRequest {

    private String requestId;
    private String requestType;
    
    // User-related fields
    private Long userId;
    
    // Product-related fields
    private Long productId;
    private String category;
    private String similarityMethod;
    
    // Order-related fields
    private Long orderId;
    private List<Map<String, Object>> orderItems;
    
    // Pagination and sorting
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
    
    // Inclusion flags
    private Boolean includeOrders;
    private Boolean includePreferences;
    private Boolean includeRecommendations;
    private Boolean includeInventory;
    
    // Recommendation parameters
    private Integer recommendationLimit;

    /**
     * Gets the request ID.
     * 
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the request ID.
     * 
     * @param requestId the request ID to set
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Gets the request type.
     * 
     * @return the request type
     */
    public String getRequestType() {
        return requestType;
    }

    /**
     * Sets the request type.
     * 
     * @param requestType the request type to set
     */
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    /**
     * Gets the user ID.
     * 
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     * 
     * @param userId the user ID to set
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the product ID.
     * 
     * @return the product ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * Sets the product ID.
     * 
     * @param productId the product ID to set
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * Gets the category.
     * 
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category.
     * 
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the similarity method.
     * 
     * @return the similarity method
     */
    public String getSimilarityMethod() {
        return similarityMethod;
    }

    /**
     * Sets the similarity method.
     * 
     * @param similarityMethod the similarity method to set
     */
    public void setSimilarityMethod(String similarityMethod) {
        this.similarityMethod = similarityMethod;
    }

    /**
     * Gets the order ID.
     * 
     * @return the order ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * Sets the order ID.
     * 
     * @param orderId the order ID to set
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets the order items.
     * 
     * @return the order items
     */
    public List<Map<String, Object>> getOrderItems() {
        return orderItems;
    }

    /**
     * Sets the order items.
     * 
     * @param orderItems the order items to set
     */
    public void setOrderItems(List<Map<String, Object>> orderItems) {
        this.orderItems = orderItems;
    }

    /**
     * Gets the page number.
     * 
     * @return the page number
     */
    public Integer getPage() {
        return page;
    }

    /**
     * Sets the page number.
     * 
     * @param page the page number to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * Gets the page size.
     * 
     * @return the page size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * Sets the page size.
     * 
     * @param size the page size to set
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * Gets the sort by field.
     * 
     * @return the sort by field
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort by field.
     * 
     * @param sortBy the sort by field to set
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Gets the sort direction.
     * 
     * @return the sort direction
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets the sort direction.
     * 
     * @param sortDirection the sort direction to set
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Gets the include orders flag.
     * 
     * @return the include orders flag
     */
    public Boolean getIncludeOrders() {
        return includeOrders;
    }

    /**
     * Sets the include orders flag.
     * 
     * @param includeOrders the include orders flag to set
     */
    public void setIncludeOrders(Boolean includeOrders) {
        this.includeOrders = includeOrders;
    }

    /**
     * Gets the include preferences flag.
     * 
     * @return the include preferences flag
     */
    public Boolean getIncludePreferences() {
        return includePreferences;
    }

    /**
     * Sets the include preferences flag.
     * 
     * @param includePreferences the include preferences flag to set
     */
    public void setIncludePreferences(Boolean includePreferences) {
        this.includePreferences = includePreferences;
    }

    /**
     * Gets the include recommendations flag.
     * 
     * @return the include recommendations flag
     */
    public Boolean getIncludeRecommendations() {
        return includeRecommendations;
    }

    /**
     * Sets the include recommendations flag.
     * 
     * @param includeRecommendations the include recommendations flag to set
     */
    public void setIncludeRecommendations(Boolean includeRecommendations) {
        this.includeRecommendations = includeRecommendations;
    }

    /**
     * Gets the include inventory flag.
     * 
     * @return the include inventory flag
     */
    public Boolean getIncludeInventory() {
        return includeInventory;
    }

    /**
     * Sets the include inventory flag.
     * 
     * @param includeInventory the include inventory flag to set
     */
    public void setIncludeInventory(Boolean includeInventory) {
        this.includeInventory = includeInventory;
    }

    /**
     * Gets the recommendation limit.
     * 
     * @return the recommendation limit
     */
    public Integer getRecommendationLimit() {
        return recommendationLimit;
    }

    /**
     * Sets the recommendation limit.
     * 
     * @param recommendationLimit the recommendation limit to set
     */
    public void setRecommendationLimit(Integer recommendationLimit) {
        this.recommendationLimit = recommendationLimit;
    }
}