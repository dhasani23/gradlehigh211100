package com.gradlehigh211100.productcatalog.model;

import com.gradlehigh211100.common.model.BaseEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Category entity representing hierarchical product categories with parent-child relationships 
 * and category-specific attributes.
 * 
 * This class implements a tree structure where each category can have a parent and multiple children,
 * allowing for a hierarchical organization of product categories.
 */
public class Category extends BaseEntity {
    
    private String name;
    private String description;
    private Category parent;
    private Set<Category> children = new HashSet<>();
    private Integer level;
    private Boolean isActive;
    private Integer sortOrder;
    private String metaTitle;
    private String metaDescription;
    
    /**
     * Default constructor for Category.
     */
    public Category() {
        this.isActive = true;
        this.level = 0;
        this.sortOrder = 0;
    }
    
    /**
     * Constructor with name parameter.
     * 
     * @param name The category name
     */
    public Category(String name) {
        this();
        this.name = name;
    }
    
    /**
     * Constructor with name and parent parameters.
     * 
     * @param name The category name
     * @param parent The parent category
     */
    public Category(String name, Category parent) {
        this(name);
        this.parent = parent;
        
        // Calculate level based on parent
        if (parent != null) {
            this.level = parent.getLevel() + 1;
            // Add this category as a child to the parent
            parent.addChild(this);
        }
    }

    /**
     * Get the category name.
     * 
     * @return The category name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the category name.
     * 
     * @param name The category name to set
     */
    public void setName(String name) {
        // FIXME: Implement name validation
        this.name = name;
    }

    /**
     * Get the category description.
     * 
     * @return The category description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the category description.
     * 
     * @param description The category description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the parent category.
     * 
     * @return The parent category
     */
    public Category getParent() {
        return parent;
    }

    /**
     * Set the parent category and recalculate level.
     * 
     * @param parent The parent category to set
     */
    public void setParent(Category parent) {
        // Remove this category from previous parent's children if exists
        if (this.parent != null) {
            this.parent.removeChild(this);
        }
        
        this.parent = parent;
        
        // Add this category to new parent's children if exists
        if (parent != null) {
            parent.addChild(this);
            // Recalculate level based on parent
            this.level = parent.getLevel() + 1;
            
            // Recursively update the level of all children
            updateChildrenLevels();
        } else {
            // Root category has level 0
            this.level = 0;
            updateChildrenLevels();
        }
    }

    /**
     * Get the child categories.
     * 
     * @return Set of child categories
     */
    public Set<Category> getChildren() {
        return children;
    }

    /**
     * Set the child categories.
     * 
     * @param children Set of child categories to set
     */
    public void setChildren(Set<Category> children) {
        // Clear existing children
        this.children.clear();
        
        // Add all new children and update their parent references
        if (children != null) {
            for (Category child : children) {
                addChild(child);
            }
        }
    }

    /**
     * Add a child category and set this category as its parent.
     * 
     * @param child The child category to add
     */
    public void addChild(Category child) {
        if (child == null) {
            return;
        }
        
        // Prevent circular reference
        if (isDescendantOf(child)) {
            throw new IllegalArgumentException("Cannot add category as child: circular reference detected");
        }
        
        // Only add if it's not already a child
        if (!children.contains(child)) {
            children.add(child);
            
            // Update child's parent reference if necessary
            if (child.getParent() != this) {
                // Using a temporary variable to avoid recursive calls
                Category oldParent = child.getParent();
                child.parent = this;
                
                // If the child had a different parent, remove from that parent
                if (oldParent != null) {
                    oldParent.children.remove(child);
                }
                
                // Update the child's level
                child.level = this.level + 1;
                
                // Recursively update the levels of child's children
                child.updateChildrenLevels();
            }
        }
    }

    /**
     * Remove a child category from this category's children.
     * 
     * @param child The child category to remove
     */
    public void removeChild(Category child) {
        if (child != null && children.contains(child)) {
            children.remove(child);
            
            // Update child's parent reference if it points to this category
            if (child.getParent() == this) {
                child.parent = null;
                child.level = 0;
                child.updateChildrenLevels();
            }
        }
    }

    /**
     * Get the hierarchical level.
     * 
     * @return The level
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * Set the hierarchical level.
     * 
     * @param level The level to set
     */
    public void setLevel(Integer level) {
        // TODO: Consider if manual level setting should be allowed
        this.level = level;
    }

    /**
     * Check if the category is active.
     * 
     * @return True if active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Set the active status.
     * 
     * @param isActive Active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Get the sort order.
     * 
     * @return The sort order
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * Set the sort order.
     * 
     * @param sortOrder The sort order to set
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Get the SEO meta title.
     * 
     * @return The meta title
     */
    public String getMetaTitle() {
        return metaTitle;
    }

    /**
     * Set the SEO meta title.
     * 
     * @param metaTitle The meta title to set
     */
    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    /**
     * Get the SEO meta description.
     * 
     * @return The meta description
     */
    public String getMetaDescription() {
        return metaDescription;
    }

    /**
     * Set the SEO meta description.
     * 
     * @param metaDescription The meta description to set
     */
    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    /**
     * Check if this is a root category (has no parent).
     * 
     * @return True if this is a root category, false otherwise
     */
    public Boolean isRootCategory() {
        return parent == null;
    }

    /**
     * Get the full hierarchical path from root to this category.
     * 
     * @return String representation of the full path
     */
    public String getFullPath() {
        if (isRootCategory()) {
            return name;
        }
        
        // Recursively build path from root to this category
        StringJoiner pathJoiner = new StringJoiner(" > ");
        Category current = this;
        
        // Use a set to detect circular references
        Set<Category> visited = new HashSet<>();
        
        while (current != null) {
            // Check for circular reference
            if (visited.contains(current)) {
                return "ERROR: Circular reference detected";
            }
            visited.add(current);
            
            // Add current category name to the beginning of the path
            pathJoiner.add(current.getName());
            current = current.getParent();
        }
        
        // Reverse the path to get root-to-leaf order
        String[] parts = pathJoiner.toString().split(" > ");
        StringBuilder reversedPath = new StringBuilder();
        
        for (int i = parts.length - 1; i >= 0; i--) {
            reversedPath.append(parts[i]);
            if (i > 0) {
                reversedPath.append(" > ");
            }
        }
        
        return reversedPath.toString();
    }
    
    /**
     * Check if this category is a descendant of the specified category.
     * Used to prevent circular references when adding children.
     * 
     * @param potentialAncestor The potential ancestor category
     * @return True if this category is a descendant of the specified category, false otherwise
     */
    private boolean isDescendantOf(Category potentialAncestor) {
        if (potentialAncestor == null) {
            return false;
        }
        
        // Check if this category is the same as the potential ancestor
        if (this.equals(potentialAncestor)) {
            return true;
        }
        
        // Check if any of this category's ancestors is the potential ancestor
        Category current = this.parent;
        // Use a set to track visited nodes to detect circular references
        Set<Category> visited = new HashSet<>();
        
        while (current != null) {
            if (visited.contains(current)) {
                // Circular reference detected
                // FIXME: Consider better handling of circular references in existing data
                return true;
            }
            
            visited.add(current);
            
            if (current.equals(potentialAncestor)) {
                return true;
            }
            
            current = current.parent;
        }
        
        return false;
    }
    
    /**
     * Recursively update the levels of all children based on this category's level.
     */
    private void updateChildrenLevels() {
        for (Category child : children) {
            child.level = this.level + 1;
            child.updateChildrenLevels();
        }
    }
    
    /**
     * Get all descendant categories (children, grandchildren, etc.).
     * 
     * @return Set of all descendant categories
     */
    public Set<Category> getAllDescendants() {
        Set<Category> allDescendants = new HashSet<>();
        
        // Use a set to track visited nodes to handle potential circular references
        collectDescendants(allDescendants, new HashSet<>());
        
        return allDescendants;
    }
    
    /**
     * Helper method to recursively collect all descendants.
     * 
     * @param descendants Set to collect descendants into
     * @param visited Set to track visited categories
     */
    private void collectDescendants(Set<Category> descendants, Set<Category> visited) {
        // Check for circular references
        if (visited.contains(this)) {
            return;
        }
        
        visited.add(this);
        
        for (Category child : children) {
            descendants.add(child);
            child.collectDescendants(descendants, visited);
        }
    }
    
    /**
     * Get active child categories.
     * 
     * @return Set of active child categories
     */
    public Set<Category> getActiveChildren() {
        return children.stream()
            .filter(Category::getIsActive)
            .collect(Collectors.toSet());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        if (!super.equals(o)) return false;
        
        Category category = (Category) o;
        
        // Compare only the name and ID for equality
        // Using getId() from BaseEntity
        return getId() != null && getId().equals(category.getId());
    }
    
    @Override
    public int hashCode() {
        // Use ID for hash code if available, otherwise use name
        return getId() != null ? getId().hashCode() : (name != null ? name.hashCode() : 0);
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", isActive=" + isActive +
                ", childrenCount=" + children.size() +
                '}';
    }
}