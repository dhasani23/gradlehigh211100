package com.gradlehigh211100.productcatalog.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data transfer object for category information used in API responses and requests.
 * Contains category hierarchy information and metadata.
 * 
 * This DTO represents a product category within the catalog system and includes
 * hierarchical relationship data to maintain the category tree structure.
 */
public class CategoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Primary identifier for the category
    private Long id;
    
    // Category display name
    private String name;
    
    // Detailed category description
    private String description;
    
    // Reference to parent category for hierarchical structure
    private Long parentId;
    
    // Level in the category tree (0 for root categories)
    private Integer level;
    
    // Flag to enable/disable categories without deletion
    private Boolean isActive;
    
    // Custom sort order for display purposes
    private Integer sortOrder;

    /**
     * Default constructor
     */
    public CategoryDTO() {
        // Initialize with default values
        this.isActive = true;
        this.level = 0;
        this.sortOrder = 0;
    }

    /**
     * Constructor with required fields
     * 
     * @param id category identifier
     * @param name category name
     */
    public CategoryDTO(Long id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    /**
     * Full constructor
     * 
     * @param id category identifier
     * @param name category name
     * @param description category description
     * @param parentId parent category identifier
     * @param level hierarchy level
     * @param isActive activity status
     * @param sortOrder display order
     */
    public CategoryDTO(Long id, String name, String description, Long parentId, 
                     Integer level, Boolean isActive, Integer sortOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.level = level;
        this.isActive = isActive;
        this.sortOrder = sortOrder;
    }

    /**
     * Get category ID
     * 
     * @return the category identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Set category ID
     * 
     * @param id the category identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get category name
     * 
     * @return the category name
     */
    public String getName() {
        return name;
    }

    /**
     * Set category name
     * 
     * @param name the category name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get category description
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set category description
     * 
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get parent category ID
     * 
     * @return the parent category ID
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * Set parent category ID
     * 
     * @param parentId the parent category ID to set
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * Get hierarchy level
     * 
     * @return the level in category hierarchy
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * Set hierarchy level
     * 
     * @param level the hierarchy level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * Check if category is active
     * 
     * @return true if category is active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Set category active status
     * 
     * @param isActive the active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Get sort order
     * 
     * @return the sort order
     */
    public Integer getSortOrder() {
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
     * Determines if this category is a root category
     * 
     * @return true if this is a root category, false otherwise
     */
    public boolean isRootCategory() {
        return parentId == null || parentId <= 0;
    }

    /**
     * Helper method to check if this category can contain products directly
     * This complex logic helps determine product placement rules
     * 
     * @param maxProductLevel the maximum level at which products can be assigned
     * @return true if products can be assigned to this category
     */
    public boolean canContainProducts(int maxProductLevel) {
        // FIXME: This logic might need adjustment based on business requirements
        if (level == null) {
            return false;
        }
        
        if (!isActive) {
            return false;
        }
        
        // Complex logic to determine if category can hold products
        boolean isLeafCategory = true;
        
        // This is a placeholder for what would normally be a database check
        // In real implementation, we would check if this category has children
        
        // For now, we'll assume categories at or below maxProductLevel can have products
        if (level <= maxProductLevel) {
            // Additional business rules could be applied here
            if (isLeafCategory || level == maxProductLevel) {
                return true;
            } else {
                // Some additional complex condition
                int complexityFactor = calculateComplexityFactor();
                return complexityFactor > 5;
            }
        }
        
        return false;
    }
    
    /**
     * Calculate a complexity factor based on category attributes
     * This is a placeholder method to demonstrate cyclomatic complexity
     * 
     * @return complexity factor value
     */
    private int calculateComplexityFactor() {
        int factor = 0;
        
        // Add complexity based on level
        if (level != null) {
            if (level == 0) {
                factor += 1;
            } else if (level == 1) {
                factor += 2;
            } else if (level == 2) {
                factor += 3;
            } else {
                factor += 4;
            }
        }
        
        // Add complexity based on name length
        if (name != null) {
            if (name.length() < 5) {
                factor += 1;
            } else if (name.length() < 10) {
                factor += 2;
            } else if (name.length() < 20) {
                factor += 3;
            } else {
                factor += 4;
            }
        }
        
        // Add complexity based on description
        if (description != null) {
            if (description.contains("special")) {
                factor += 2;
            }
            
            if (description.length() > 100) {
                factor += 2;
            }
            
            // Count words in description
            String[] words = description.split("\\s+");
            if (words.length > 20) {
                factor += 2;
            } else if (words.length > 10) {
                factor += 1;
            }
        }
        
        // Special case for active status
        if (Boolean.TRUE.equals(isActive)) {
            factor += 1;
        } else {
            factor -= 1;
        }
        
        return factor;
    }
    
    /**
     * Validate if the category data is complete enough for creation
     * 
     * @return true if category has all required data
     */
    public boolean validateForCreation() {
        // TODO: Add more comprehensive validation rules
        
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (level == null) {
            return false;
        }
        
        // Parent is required except for root categories
        if (level > 0 && (parentId == null || parentId <= 0)) {
            return false;
        }
        
        // Complex validation with nested conditions
        if (isActive != null && isActive) {
            if (name != null && !name.isEmpty()) {
                if (level != null) {
                    if (level == 0) {
                        // Root category checks
                        return true;
                    } else if (level > 0) {
                        // Non-root category checks
                        if (parentId != null && parentId > 0) {
                            // Valid non-root category
                            return true;
                        } else {
                            // Invalid non-root (no parent)
                            return false;
                        }
                    } else {
                        // Negative level is invalid
                        return false;
                    }
                } else {
                    // No level specified
                    return false;
                }
            } else {
                // No name specified
                return false;
            }
        } else {
            // Inactive categories require special validation
            return name != null && !name.isEmpty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        CategoryDTO that = (CategoryDTO) o;
        
        return Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(parentId, that.parentId) &&
               Objects.equals(level, that.level) &&
               Objects.equals(isActive, that.isActive) &&
               Objects.equals(sortOrder, that.sortOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, parentId, level, isActive, sortOrder);
    }

    @Override
    public String toString() {
        return "CategoryDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 20)) + "..." : null) + '\'' +
                ", parentId=" + parentId +
                ", level=" + level +
                ", isActive=" + isActive +
                ", sortOrder=" + sortOrder +
                '}';
    }
}