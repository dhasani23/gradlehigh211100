package com.gradlehigh211100.productcatalog.controller;

import com.gradlehigh211100.productcatalog.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * REST controller providing API endpoints for category management operations 
 * including hierarchical category operations.
 * 
 * This controller handles various category-related operations including creating,
 * updating, deleting, and retrieving categories. It also provides methods for working
 * with category hierarchies.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    
    /**
     * Category service for business logic
     */
    private final CategoryService categoryService;

    /**
     * Cache for frequent category tree operations
     */
    private Map<String, Object> categoryCache;
    
    /**
     * Constructor injection of dependencies
     * 
     * @param categoryService The service providing category business logic
     */
    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
        this.categoryCache = new HashMap<>();
    }

    /**
     * Create new category endpoint
     * 
     * @param categoryData The category data transfer object containing category information
     * @return ResponseEntity containing the created category or error status
     */
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryData) {
        try {
            logger.debug("Creating new category: {}", categoryData);
            
            // Validate category data
            if (categoryData == null || categoryData.getName() == null || categoryData.getName().trim().isEmpty()) {
                logger.error("Invalid category data provided: {}", categoryData);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            // Additional validation for special business logic
            if (categoryData.getParentId() != null) {
                // Check if parent category exists
                boolean parentExists = categoryService.checkCategoryExists(categoryData.getParentId());
                if (!parentExists) {
                    logger.error("Parent category not found: {}", categoryData.getParentId());
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            
            // Try to create the category with fallback handling
            CategoryDTO createdCategory = null;
            try {
                createdCategory = categoryService.createCategory(categoryData);
            } catch (Exception e) {
                logger.error("Failed to create category", e);
                
                // Try fallback strategy for special cases
                if (e.getMessage() != null && e.getMessage().contains("duplicate")) {
                    logger.info("Attempting category creation with modified parameters due to duplicate");
                    categoryData.setName(categoryData.getName() + " (copy)");
                    createdCategory = categoryService.createCategory(categoryData);
                } else {
                    throw e;
                }
            }
            
            // Clear category cache after successful creation
            clearCategoryCache();
            
            logger.info("Successfully created category with ID: {}", createdCategory.getId());
            return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Unexpected error creating category", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update category endpoint
     * 
     * @param categoryId The ID of the category to update
     * @param categoryData The updated category data
     * @return ResponseEntity containing the updated category or error status
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestBody CategoryDTO categoryData) {
        
        logger.debug("Updating category ID: {} with data: {}", categoryId, categoryData);
        
        try {
            // Input validation with complex business rules
            if (categoryId == null || categoryData == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            // Check if category exists
            if (!categoryService.checkCategoryExists(categoryId)) {
                logger.warn("Category not found for update: {}", categoryId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Additional validation for hierarchical integrity
            if (categoryData.getParentId() != null) {
                // Prevent circular references in hierarchy
                if (categoryData.getParentId().equals(categoryId)) {
                    logger.error("Circular reference detected - category cannot be its own parent");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                
                // Check if parent exists
                if (!categoryService.checkCategoryExists(categoryData.getParentId())) {
                    logger.error("Parent category not found: {}", categoryData.getParentId());
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                
                // FIXME: This should check for deeper circular references in the hierarchy
                // For now, only direct self-references are prevented
            }
            
            // Set the ID to ensure we're updating the right entity
            categoryData.setId(categoryId);
            
            // Update the category
            CategoryDTO updatedCategory = categoryService.updateCategory(categoryData);
            if (updatedCategory == null) {
                logger.error("Category service returned null for update operation");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Clear category cache after update
            clearCategoryCache();
            
            logger.info("Successfully updated category ID: {}", categoryId);
            return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating category ID: " + categoryId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete category endpoint
     * 
     * @param categoryId The ID of the category to delete
     * @return ResponseEntity with status indicating success or failure
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("categoryId") Long categoryId) {
        logger.debug("Deleting category ID: {}", categoryId);
        
        try {
            // Check if category exists
            if (!categoryService.checkCategoryExists(categoryId)) {
                logger.warn("Category not found for deletion: {}", categoryId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Check if category has children
            List<CategoryDTO> childCategories = categoryService.getChildCategories(categoryId);
            if (childCategories != null && !childCategories.isEmpty()) {
                logger.warn("Cannot delete category {} because it has {} child categories", 
                           categoryId, childCategories.size());
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
            
            // Check if category has associated products (simulated complex business rule)
            if (categoryService.hasDependencies(categoryId)) {
                logger.warn("Cannot delete category {} because it has dependencies", categoryId);
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
            
            // Perform deletion with retry logic
            boolean deleted = false;
            int retryCount = 0;
            int maxRetries = 3;
            Exception lastException = null;
            
            while (!deleted && retryCount < maxRetries) {
                try {
                    deleted = categoryService.deleteCategory(categoryId);
                    break;
                } catch (Exception e) {
                    lastException = e;
                    retryCount++;
                    logger.warn("Deletion attempt {} failed for category {}", retryCount, categoryId, e);
                    // Wait before retry
                    Thread.sleep(100 * retryCount);
                }
            }
            
            if (!deleted) {
                if (lastException != null) {
                    throw lastException;
                } else {
                    logger.error("Failed to delete category after {} attempts", maxRetries);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            
            // Clear category cache after deletion
            clearCategoryCache();
            
            logger.info("Successfully deleted category ID: {}", categoryId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error deleting category ID: " + categoryId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get category by ID endpoint
     * 
     * @param categoryId The ID of the category to retrieve
     * @return ResponseEntity containing the requested category or error status
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable("categoryId") Long categoryId) {
        logger.debug("Getting category by ID: {}", categoryId);
        
        // Check for cached value (demonstration of caching strategy)
        String cacheKey = "category_" + categoryId;
        if (categoryCache.containsKey(cacheKey)) {
            logger.debug("Category cache hit for ID: {}", categoryId);
            CategoryDTO cachedCategory = (CategoryDTO) categoryCache.get(cacheKey);
            return new ResponseEntity<>(cachedCategory, HttpStatus.OK);
        }
        
        try {
            // Retrieve category from service
            CategoryDTO category = categoryService.getCategoryById(categoryId);
            
            if (category == null) {
                logger.warn("Category not found with ID: {}", categoryId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Add to cache with artificial complexity to increase cyclomatic complexity
            if (categoryId % 2 == 0) {
                // Even IDs are cached differently than odd IDs
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(50); // Simulate async operation
                        categoryCache.put(cacheKey, category);
                        logger.trace("Cached category with ID: {}", categoryId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Cache operation interrupted", e);
                    }
                });
            } else {
                categoryCache.put(cacheKey, category);
                logger.trace("Directly cached category with ID: {}", categoryId);
            }
            
            logger.debug("Successfully retrieved category ID: {}", categoryId);
            return new ResponseEntity<>(category, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving category with ID: " + categoryId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get root categories endpoint
     * 
     * @return ResponseEntity containing a list of root categories
     */
    @GetMapping("/roots")
    public ResponseEntity<List<CategoryDTO>> getRootCategories() {
        logger.debug("Getting root categories");
        
        // Check for cached value
        String cacheKey = "root_categories";
        if (categoryCache.containsKey(cacheKey)) {
            logger.debug("Root categories cache hit");
            @SuppressWarnings("unchecked")
            List<CategoryDTO> cachedCategories = (List<CategoryDTO>) categoryCache.get(cacheKey);
            return new ResponseEntity<>(cachedCategories, HttpStatus.OK);
        }
        
        try {
            List<CategoryDTO> rootCategories = categoryService.getRootCategories();
            
            // Some complex filtering logic to increase cyclomatic complexity
            final long timestamp = System.currentTimeMillis();
            rootCategories = rootCategories.stream()
                .filter(category -> {
                    // Filter based on complex conditions
                    if (category == null || category.getName() == null) {
                        return false;
                    }
                    
                    if (category.getCreatedAt() != null && 
                            category.getCreatedAt().getTime() > timestamp - (7 * 24 * 60 * 60 * 1000)) {
                        // Categories created in the last week are always included
                        return true;
                    }
                    
                    if ("SYSTEM".equals(category.getType()) && !isAdminUser()) {
                        // System categories are filtered out for non-admin users
                        return false;
                    }
                    
                    if (category.isDeleted() != null && category.isDeleted()) {
                        // Soft-deleted categories are filtered out
                        return false;
                    }
                    
                    // All other categories are included
                    return true;
                })
                .collect(Collectors.toList());
            
            // Cache the result
            categoryCache.put(cacheKey, rootCategories);
            
            logger.debug("Retrieved {} root categories", rootCategories.size());
            return new ResponseEntity<>(rootCategories, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving root categories", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get child categories endpoint
     * 
     * @param parentId The ID of the parent category
     * @return ResponseEntity containing a list of child categories
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CategoryDTO>> getChildCategories(@PathVariable("parentId") Long parentId) {
        logger.debug("Getting child categories for parent ID: {}", parentId);
        
        // Check cache
        String cacheKey = "children_" + parentId;
        if (categoryCache.containsKey(cacheKey)) {
            logger.debug("Child categories cache hit for parent ID: {}", parentId);
            @SuppressWarnings("unchecked")
            List<CategoryDTO> cachedChildren = (List<CategoryDTO>) categoryCache.get(cacheKey);
            return new ResponseEntity<>(cachedChildren, HttpStatus.OK);
        }
        
        try {
            // Check if parent category exists
            if (!categoryService.checkCategoryExists(parentId)) {
                logger.warn("Parent category not found: {}", parentId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Get child categories
            List<CategoryDTO> childCategories = categoryService.getChildCategories(parentId);
            
            // Process child categories with complex rules
            processChildCategories(childCategories, parentId);
            
            // Cache the result
            categoryCache.put(cacheKey, childCategories);
            
            logger.debug("Retrieved {} child categories for parent ID: {}", childCategories.size(), parentId);
            return new ResponseEntity<>(childCategories, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving child categories for parent ID: " + parentId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get category tree endpoint
     * 
     * @return ResponseEntity containing the complete category tree
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryDTO>> getCategoryTree() {
        logger.debug("Getting complete category tree");
        
        // Check cache
        String cacheKey = "category_tree";
        if (categoryCache.containsKey(cacheKey)) {
            logger.debug("Category tree cache hit");
            @SuppressWarnings("unchecked")
            List<CategoryDTO> cachedTree = (List<CategoryDTO>) categoryCache.get(cacheKey);
            return new ResponseEntity<>(cachedTree, HttpStatus.OK);
        }
        
        try {
            // Get root categories
            List<CategoryDTO> rootCategories = categoryService.getRootCategories();
            
            // Load full tree with children - this uses parallelism for performance
            List<CategoryDTO> fullTree = loadCategoryTreeInParallel(rootCategories);
            
            // Cache the result
            categoryCache.put(cacheKey, fullTree);
            
            logger.debug("Successfully retrieved complete category tree with {} root categories", fullTree.size());
            return new ResponseEntity<>(fullTree, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving category tree", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Load the category tree using parallel processing for performance
     * 
     * @param rootCategories List of root categories to process
     * @return Complete category tree
     */
    private List<CategoryDTO> loadCategoryTreeInParallel(List<CategoryDTO> rootCategories) 
            throws InterruptedException, ExecutionException {
        // Create futures for parallel processing
        List<CompletableFuture<CategoryDTO>> futures = rootCategories.stream()
            .map(rootCategory -> CompletableFuture.supplyAsync(() -> {
                try {
                    // Clone to avoid modifying the original
                    CategoryDTO root = cloneCategory(rootCategory);
                    // Load children recursively
                    loadChildrenRecursively(root, 0, 3);
                    return root;
                } catch (Exception e) {
                    logger.error("Error loading category tree for root: " + rootCategory.getId(), e);
                    return rootCategory; // Return without children on error
                }
            }))
            .collect(Collectors.toList());
        
        // Wait for all futures and collect results
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        
        // Wait for completion and collect results
        allFutures.get(); // This will throw an exception if any future failed
        
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
    
    /**
     * Recursively load children for a category
     * 
     * @param category The category to load children for
     * @param currentDepth Current depth in the tree
     * @param maxDepth Maximum depth to load
     */
    private void loadChildrenRecursively(CategoryDTO category, int currentDepth, int maxDepth) {
        // Stop at max depth to prevent too deep recursion
        if (currentDepth >= maxDepth) {
            // Set a flag indicating there might be more children
            category.setHasMoreChildren(true);
            return;
        }
        
        try {
            // Load children for this category
            List<CategoryDTO> children = categoryService.getChildCategories(category.getId());
            
            // Set children on the category
            category.setChildren(children);
            
            // Process each child recursively
            if (children != null && !children.isEmpty()) {
                for (CategoryDTO child : children) {
                    loadChildrenRecursively(child, currentDepth + 1, maxDepth);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading children for category: " + category.getId(), e);
            // Set error flag on category
            category.setLoadError(true);
        }
    }
    
    /**
     * Process child categories with complex business rules
     * 
     * @param children The list of child categories to process
     * @param parentId The ID of the parent category
     */
    private void processChildCategories(List<CategoryDTO> children, Long parentId) {
        // Apply various business rules to child categories
        
        if (children == null || children.isEmpty()) {
            // Nothing to process
            return;
        }
        
        // Apply different rules based on parent category type
        CategoryDTO parentCategory = categoryService.getCategoryById(parentId);
        if (parentCategory != null && "SPECIAL".equals(parentCategory.getType())) {
            // Special processing for children of SPECIAL categories
            for (CategoryDTO child : children) {
                // Set inherited properties
                child.setSpecialFlag(true);
                
                // Apply visibility rules
                if (child.isRestricted() != null && child.isRestricted() && !isAdminUser()) {
                    // Mask restricted information for non-admin users
                    maskRestrictedCategory(child);
                }
                
                // Apply ordering rules
                if (child.getDisplayOrder() == null) {
                    // Set default display order based on ID to ensure consistent ordering
                    child.setDisplayOrder(child.getId().intValue() % 1000);
                }
            }
        } else {
            // Standard processing for regular categories
            for (CategoryDTO child : children) {
                // Apply visibility rules based on current user
                if (child.isRestricted() != null && child.isRestricted() && !isAdminUser()) {
                    // Hide restricted information
                    maskRestrictedCategory(child);
                }
                
                // TODO: Apply additional business rules based on category metadata
            }
        }
    }
    
    /**
     * Mask sensitive information for restricted categories
     * 
     * @param category The category to mask
     */
    private void maskRestrictedCategory(CategoryDTO category) {
        // Replace sensitive fields with masked values
        if (category.getDescription() != null) {
            category.setDescription("[RESTRICTED]");
        }
        
        // Clear any sensitive metadata
        if (category.getMetadata() != null) {
            category.setMetadata(new HashMap<>());
        }
        
        // Mark as restricted
        category.setRestricted(true);
    }
    
    /**
     * Clone a category DTO to avoid modifying the original
     * 
     * @param source The source category to clone
     * @return A new instance with copied data
     */
    private CategoryDTO cloneCategory(CategoryDTO source) {
        CategoryDTO clone = new CategoryDTO();
        clone.setId(source.getId());
        clone.setName(source.getName());
        clone.setDescription(source.getDescription());
        clone.setParentId(source.getParentId());
        clone.setType(source.getType());
        clone.setCreatedAt(source.getCreatedAt());
        clone.setUpdatedAt(source.getUpdatedAt());
        clone.setDeleted(source.isDeleted());
        clone.setRestricted(source.isRestricted());
        clone.setDisplayOrder(source.getDisplayOrder());
        clone.setSpecialFlag(source.isSpecialFlag());
        
        // Deep copy metadata if present
        if (source.getMetadata() != null) {
            clone.setMetadata(new HashMap<>(source.getMetadata()));
        }
        
        // Initialize children list
        clone.setChildren(new ArrayList<>());
        
        return clone;
    }
    
    /**
     * Check if the current user has admin privileges
     * 
     * @return true if the current user is an admin
     */
    private boolean isAdminUser() {
        // TODO: Implement actual security check using Spring Security
        // This is a placeholder implementation
        return false;
    }
    
    /**
     * Clear the category cache when data changes
     */
    private void clearCategoryCache() {
        logger.debug("Clearing category cache");
        categoryCache.clear();
    }
}

/**
 * Data Transfer Object for Category entities
 */
class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String type;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;
    private Boolean deleted;
    private Boolean restricted;
    private Integer displayOrder;
    private Boolean specialFlag;
    private Map<String, Object> metadata;
    private List<CategoryDTO> children;
    private Boolean hasMoreChildren;
    private Boolean loadError;

    public CategoryDTO() {
        // Default constructor
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public java.util.Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt;
    }

    public java.util.Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(Boolean restricted) {
        this.restricted = restricted;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean isSpecialFlag() {
        return specialFlag;
    }

    public void setSpecialFlag(Boolean specialFlag) {
        this.specialFlag = specialFlag;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<CategoryDTO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryDTO> children) {
        this.children = children;
    }

    public Boolean getHasMoreChildren() {
        return hasMoreChildren;
    }

    public void setHasMoreChildren(Boolean hasMoreChildren) {
        this.hasMoreChildren = hasMoreChildren;
    }

    public Boolean getLoadError() {
        return loadError;
    }

    public void setLoadError(Boolean loadError) {
        this.loadError = loadError;
    }

    @Override
    public String toString() {
        return "CategoryDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                ", type='" + type + '\'' +
                '}';
    }
}