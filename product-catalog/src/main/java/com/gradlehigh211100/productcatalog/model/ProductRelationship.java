package com.gradlehigh211100.productcatalog.model;

import com.gradlehigh211100.common.model.BaseEntity;

/**
 * Entity representing relationships between products like cross-sell, upsell, and related products.
 * This class manages the different types of relationships that can exist between products
 * in the catalog, which is essential for recommendation systems and product navigation.
 */
public class ProductRelationship extends BaseEntity {

    // Constants for relationship types
    public static final String RELATIONSHIP_CROSS_SELL = "CROSS_SELL";
    public static final String RELATIONSHIP_UPSELL = "UPSELL";
    public static final String RELATIONSHIP_RELATED = "RELATED";
    
    private Product sourceProduct;
    private Product targetProduct;
    private String relationshipType;
    private Integer priority;
    private Boolean isActive;
    
    // Cache for complex relationship calculations
    private transient Boolean calculatedRelevanceFlag;
    
    /**
     * Default constructor
     */
    public ProductRelationship() {
        this.isActive = Boolean.TRUE;
        this.priority = 100; // Default priority
    }
    
    /**
     * Constructor with main fields
     * 
     * @param sourceProduct Source product in relationship
     * @param targetProduct Target product in relationship
     * @param relationshipType Type of relationship (cross-sell, upsell, related)
     */
    public ProductRelationship(Product sourceProduct, Product targetProduct, String relationshipType) {
        this();
        this.sourceProduct = sourceProduct;
        this.targetProduct = targetProduct;
        setRelationshipType(relationshipType);
    }
    
    /**
     * Full constructor with all fields
     * 
     * @param sourceProduct Source product in relationship
     * @param targetProduct Target product in relationship
     * @param relationshipType Type of relationship (cross-sell, upsell, related)
     * @param priority Priority for ordering recommendations
     * @param isActive Active status flag
     */
    public ProductRelationship(Product sourceProduct, Product targetProduct, 
                              String relationshipType, Integer priority, Boolean isActive) {
        this(sourceProduct, targetProduct, relationshipType);
        this.priority = priority;
        this.isActive = isActive;
    }

    /**
     * Get the source product in this relationship
     * 
     * @return The source product
     */
    public Product getSourceProduct() {
        return sourceProduct;
    }

    /**
     * Set the source product in this relationship
     * 
     * @param sourceProduct The source product to set
     */
    public void setSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
        // Reset any cached calculations when source changes
        resetCalculations();
    }

    /**
     * Get the target product in this relationship
     * 
     * @return The target product
     */
    public Product getTargetProduct() {
        return targetProduct;
    }

    /**
     * Set the target product in this relationship
     * 
     * @param targetProduct The target product to set
     */
    public void setTargetProduct(Product targetProduct) {
        this.targetProduct = targetProduct;
        // Reset any cached calculations when target changes
        resetCalculations();
    }

    /**
     * Get relationship type
     * 
     * @return The relationship type
     */
    public String getRelationshipType() {
        return relationshipType;
    }

    /**
     * Set relationship type
     * Validates that the provided type is one of the supported types.
     * 
     * @param type The relationship type to set
     */
    public void setRelationshipType(String type) {
        // Validate the relationship type
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Relationship type cannot be null or empty");
        }
        
        String upperType = type.toUpperCase();
        if (!RELATIONSHIP_CROSS_SELL.equals(upperType) &&
            !RELATIONSHIP_UPSELL.equals(upperType) &&
            !RELATIONSHIP_RELATED.equals(upperType)) {
            
            // FIXME: Consider making this more flexible by using an enum instead of string constants
            throw new IllegalArgumentException("Invalid relationship type: " + type +
                ". Must be one of: CROSS_SELL, UPSELL, RELATED");
        }
        
        this.relationshipType = upperType;
        // Reset any cached calculations when relationship type changes
        resetCalculations();
    }

    /**
     * Get priority for ordering recommendations
     * 
     * @return The priority value
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Set priority for ordering recommendations
     * 
     * @param priority The priority value to set
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Check if relationship is active
     * 
     * @return true if active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Set active status of relationship
     * 
     * @param isActive The active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    /**
     * Check if relationship is cross-sell
     * 
     * @return true if relationship is cross-sell, false otherwise
     */
    public Boolean isCrossSell() {
        return RELATIONSHIP_CROSS_SELL.equals(relationshipType);
    }

    /**
     * Check if relationship is upsell
     * 
     * @return true if relationship is upsell, false otherwise
     */
    public Boolean isUpsell() {
        return RELATIONSHIP_UPSELL.equals(relationshipType);
    }
    
    /**
     * Check if relationship is related
     * 
     * @return true if relationship is related, false otherwise
     */
    public Boolean isRelated() {
        return RELATIONSHIP_RELATED.equals(relationshipType);
    }
    
    /**
     * Determines if this relationship is relevant for the current business context.
     * This involves complex business logic considering product statuses, categories,
     * pricing relationships, and more.
     * 
     * @return true if the relationship is deemed relevant
     */
    public Boolean isRelevant() {
        // Use cached value if available
        if (calculatedRelevanceFlag != null) {
            return calculatedRelevanceFlag;
        }
        
        // Complex business logic to determine relevance
        Boolean relevance = calculateRelevance();
        
        // Cache the result
        calculatedRelevanceFlag = relevance;
        return relevance;
    }
    
    /**
     * Calculate the relevance of this relationship based on complex business rules
     * 
     * @return true if the relationship is relevant
     */
    private Boolean calculateRelevance() {
        // Skip inactive relationships
        if (!Boolean.TRUE.equals(isActive)) {
            return Boolean.FALSE;
        }
        
        // Check if both products exist and are valid
        if (sourceProduct == null || targetProduct == null) {
            return Boolean.FALSE;
        }
        
        // TODO: Add more complex business logic here based on product catalog rules
        
        // Implement different relevance calculations based on relationship type
        if (isCrossSell()) {
            return calculateCrossSellRelevance();
        } else if (isUpsell()) {
            return calculateUpsellRelevance();
        } else if (isRelated()) {
            return calculateRelatedRelevance();
        }
        
        return Boolean.TRUE; // Default to true for unknown relationship types
    }
    
    /**
     * Calculate cross-sell relationship relevance
     * 
     * @return true if the cross-sell relationship is relevant
     */
    private Boolean calculateCrossSellRelevance() {
        // FIXME: Implement proper cross-sell relevance logic
        
        // Complex logic path 1: Check for compatible categories
        Boolean categoriesCompatible = checkCategoriesCompatibility();
        
        // Complex logic path 2: Check for price ratio constraints
        Boolean priceRatioValid = checkPriceRatioValidity();
        
        // Complex logic path 3: Check for seasonal applicability
        Boolean seasonallyApplicable = checkSeasonalApplicability();
        
        // Combined relevance calculation
        return categoriesCompatible && priceRatioValid && seasonallyApplicable;
    }
    
    /**
     * Calculate upsell relationship relevance
     * 
     * @return true if the upsell relationship is relevant
     */
    private Boolean calculateUpsellRelevance() {
        // FIXME: Implement proper upsell relevance logic
        
        // Check if target product price is higher than source (basic upsell rule)
        if (targetProduct.getPrice() <= sourceProduct.getPrice()) {
            return Boolean.FALSE;
        }
        
        // Check if they're in the same product line
        Boolean sameLine = checkSameProductLine();
        
        // Check if target has premium features compared to source
        Boolean hasPremiumFeatures = checkPremiumFeatures();
        
        return sameLine && hasPremiumFeatures;
    }
    
    /**
     * Calculate related product relationship relevance
     * 
     * @return true if the related product relationship is relevant
     */
    private Boolean calculateRelatedRelevance() {
        // TODO: Implement proper related products relevance logic
        
        // Check if products share enough attributes to be considered related
        Boolean sharesSufficientAttributes = checkSharedAttributes();
        
        // Check if products are frequently purchased together
        Boolean frequentlyPurchasedTogether = checkPurchaseHistory();
        
        return sharesSufficientAttributes || frequentlyPurchasedTogether;
    }
    
    /**
     * Check if source and target products are compatible by category
     * 
     * @return true if categories are compatible
     */
    private Boolean checkCategoriesCompatibility() {
        // Placeholder for complex category compatibility logic
        try {
            // Simulated complex branching logic
            if (sourceProduct.getCategory() == null || targetProduct.getCategory() == null) {
                return Boolean.FALSE;
            }
            
            // More complex category relationship checking would go here
            
            return Boolean.TRUE;
        } catch (Exception e) {
            // Log error
            return Boolean.FALSE;
        }
    }
    
    /**
     * Check if the price ratio between products is valid for the relationship type
     * 
     * @return true if price ratio is valid
     */
    private Boolean checkPriceRatioValidity() {
        // Placeholder for complex price ratio validation
        try {
            double sourcePrice = sourceProduct.getPrice();
            double targetPrice = targetProduct.getPrice();
            
            if (sourcePrice <= 0) {
                return Boolean.FALSE;
            }
            
            double ratio = targetPrice / sourcePrice;
            
            // Different ratio validation based on relationship type
            if (isCrossSell()) {
                return ratio >= 0.2 && ratio <= 5.0;
            } else if (isUpsell()) {
                return ratio > 1.0 && ratio <= 10.0;
            } else {
                return ratio >= 0.5 && ratio <= 2.0;
            }
        } catch (Exception e) {
            // Log error
            return Boolean.FALSE;
        }
    }
    
    /**
     * Check if the relationship is applicable to the current season
     * 
     * @return true if the relationship is seasonally applicable
     */
    private Boolean checkSeasonalApplicability() {
        // TODO: Implement seasonal applicability check
        return Boolean.TRUE; // Default to true for now
    }
    
    /**
     * Check if source and target products are from the same product line
     * 
     * @return true if they're from the same line
     */
    private Boolean checkSameProductLine() {
        // Placeholder for product line check
        try {
            return sourceProduct.getProductLine() != null && 
                   sourceProduct.getProductLine().equals(targetProduct.getProductLine());
        } catch (Exception e) {
            // Log error
            return Boolean.FALSE;
        }
    }
    
    /**
     * Check if target product has premium features compared to source
     * 
     * @return true if target has premium features
     */
    private Boolean checkPremiumFeatures() {
        // Placeholder for premium features check
        // This would involve complex feature comparison logic
        return Boolean.TRUE; // Default for now
    }
    
    /**
     * Check if products share sufficient attributes to be considered related
     * 
     * @return true if they share enough attributes
     */
    private Boolean checkSharedAttributes() {
        // Placeholder for attribute comparison
        return Boolean.TRUE; // Default for now
    }
    
    /**
     * Check purchase history to see if products are frequently bought together
     * 
     * @return true if frequently purchased together
     */
    private Boolean checkPurchaseHistory() {
        // Placeholder for purchase history analysis
        return Boolean.TRUE; // Default for now
    }
    
    /**
     * Reset any cached calculations when underlying data changes
     */
    private void resetCalculations() {
        calculatedRelevanceFlag = null;
    }

    @Override
    public String toString() {
        return "ProductRelationship{" +
                "sourceProduct=" + (sourceProduct != null ? sourceProduct.getId() : "null") +
                ", targetProduct=" + (targetProduct != null ? targetProduct.getId() : "null") +
                ", relationshipType='" + relationshipType + '\'' +
                ", priority=" + priority +
                ", isActive=" + isActive +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRelationship)) return false;
        if (!super.equals(o)) return false;
        
        ProductRelationship that = (ProductRelationship) o;
        
        if (sourceProduct != null ? !sourceProduct.equals(that.sourceProduct) : that.sourceProduct != null)
            return false;
        if (targetProduct != null ? !targetProduct.equals(that.targetProduct) : that.targetProduct != null)
            return false;
        return relationshipType != null ? relationshipType.equals(that.relationshipType) : that.relationshipType == null;
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sourceProduct != null ? sourceProduct.hashCode() : 0);
        result = 31 * result + (targetProduct != null ? targetProduct.hashCode() : 0);
        result = 31 * result + (relationshipType != null ? relationshipType.hashCode() : 0);
        return result;
    }
}