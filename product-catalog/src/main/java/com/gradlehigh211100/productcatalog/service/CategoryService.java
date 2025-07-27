package com.gradlehigh211100.productcatalog.service;

import com.gradlehigh211100.productcatalog.repository.CategoryRepository;
import com.gradlehigh211100.productcatalog.dto.CategoryDTO;
import com.gradlehigh211100.productcatalog.entity.Category;
import com.gradlehigh211100.productcatalog.exception.CategoryNotFoundException;
import com.gradlehigh211100.productcatalog.exception.CategoryOperationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service class that provides business logic for category management.
 * Handles operations like creating, updating, deleting, and retrieving categories.
 * Also manages hierarchical operations and category tree structures.
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    
    private static final String CACHE_KEY_ALL_CATEGORIES = "all_categories";
    private static final String CACHE_KEY_ROOT_CATEGORIES = "root_categories";
    private static final String CACHE_KEY_CATEGORY_TREE = "category_tree";
    private static final int MAX_HIERARCHY_DEPTH = 10;
    private static final int MAX_CHILDREN_PER_CATEGORY = 100;

    private final CategoryRepository categoryRepository;
    private final CacheService cacheService;
    
    // Maps to track category relationships for improved performance
    private final Map<Long, Set<Long>> categoryChildrenMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> categoryParentMap = new ConcurrentHashMap<>();

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, CacheService cacheService) {
        this.categoryRepository = categoryRepository;
        this.cacheService = cacheService;
        initializeCategoryMaps();
    }

    /**
     * Initializes internal category relationship maps for faster access
     * This adds complexity but improves performance for repeated operations
     */
    private void initializeCategoryMaps() {
        logger.info("Initializing category relationship maps");
        List<Category> allCategories = categoryRepository.findAll();
        
        for (Category category : allCategories) {
            Long categoryId = category.getId();
            Long parentId = category.getParentId();
            
            // Build parent-child relationship maps
            if (parentId != null) {
                categoryParentMap.put(categoryId, parentId);
                categoryChildrenMap.computeIfAbsent(parentId, k -> new HashSet<>()).add(categoryId);
            } else {
                // Root categories have null parent
                categoryChildrenMap.computeIfAbsent(0L, k -> new HashSet<>()).add(categoryId);
            }
        }
        
        logger.info("Category maps initialized with {} categories", allCategories.size());
    }

    /**
     * Create a new category
     *
     * @param categoryData DTO containing category information
     * @return Created category as DTO
     * @throws CategoryOperationException if the category couldn't be created
     */
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryData) {
        logger.info("Creating new category: {}", categoryData.getName());
        
        // Validation logic with high cyclomatic complexity
        if (categoryData == null) {
            logger.error("Cannot create category: Data is null");
            throw new IllegalArgumentException("Category data cannot be null");
        }
        
        if (categoryData.getName() == null || categoryData.getName().trim().isEmpty()) {
            logger.error("Cannot create category: Name is required");
            throw new CategoryOperationException("Category name is required");
        }
        
        // Check name length and format
        if (categoryData.getName().length() < 2 || categoryData.getName().length() > 50) {
            logger.error("Cannot create category: Name length invalid");
            throw new CategoryOperationException("Category name must be between 2 and 50 characters");
        }
        
        // Check if parent category exists if parentId is provided
        if (categoryData.getParentId() != null && categoryData.getParentId() > 0) {
            Optional<Category> parentCategory = categoryRepository.findById(categoryData.getParentId());
            if (!parentCategory.isPresent()) {
                logger.error("Cannot create category: Parent category {} not found", categoryData.getParentId());
                throw new CategoryNotFoundException("Parent category not found with ID: " + categoryData.getParentId());
            }
            
            // Check hierarchy depth to prevent too deep nesting
            int depth = calculateHierarchyDepth(categoryData.getParentId());
            if (depth >= MAX_HIERARCHY_DEPTH) {
                logger.error("Cannot create category: Maximum hierarchy depth reached");
                throw new CategoryOperationException("Maximum category hierarchy depth reached");
            }
            
            // Check number of children for parent
            int childrenCount = getChildCategoriesCount(categoryData.getParentId());
            if (childrenCount >= MAX_CHILDREN_PER_CATEGORY) {
                logger.error("Cannot create category: Parent already has maximum children");
                throw new CategoryOperationException("Parent category already has maximum number of children");
            }
        }
        
        try {
            // Convert DTO to entity
            Category category = new Category();
            category.setName(categoryData.getName());
            category.setDescription(categoryData.getDescription());
            category.setParentId(categoryData.getParentId());
            category.setActive(categoryData.isActive() != null ? categoryData.isActive() : true);
            category.setDisplayOrder(categoryData.getDisplayOrder() != null ? 
                    categoryData.getDisplayOrder() : calculateNextDisplayOrder(categoryData.getParentId()));
            category.setMetadata(categoryData.getMetadata());
            category.setCreatedAt(new Date());
            category.setUpdatedAt(new Date());
            
            // Save category
            Category savedCategory = categoryRepository.save(category);
            
            // Update internal maps
            Long categoryId = savedCategory.getId();
            Long parentId = savedCategory.getParentId();
            
            if (parentId != null) {
                categoryParentMap.put(categoryId, parentId);
                categoryChildrenMap.computeIfAbsent(parentId, k -> new HashSet<>()).add(categoryId);
            } else {
                // Root category
                categoryChildrenMap.computeIfAbsent(0L, k -> new HashSet<>()).add(categoryId);
            }
            
            // Invalidate caches
            invalidateCategoryCaches();
            
            logger.info("Category created successfully with ID: {}", savedCategory.getId());
            return convertToDTO(savedCategory);
            
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage(), e);
            throw new CategoryOperationException("Failed to create category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate the next display order for a category based on its siblings
     * @param parentId The parent category ID
     * @return The next display order value
     */
    private int calculateNextDisplayOrder(Long parentId) {
        // Find max display order among siblings
        List<Category> siblings;
        if (parentId == null) {
            siblings = categoryRepository.findByParentIdIsNull();
        } else {
            siblings = categoryRepository.findByParentId(parentId);
        }
        
        return siblings.stream()
                .mapToInt(Category::getDisplayOrder)
                .max()
                .orElse(0) + 10; // Leave gaps between display orders for easier future insertions
    }

    /**
     * Calculate the hierarchy depth of a category
     * @param categoryId The category ID
     * @return The depth in the hierarchy
     */
    private int calculateHierarchyDepth(Long categoryId) {
        int depth = 0;
        Long currentId = categoryId;
        
        // Traverse up the tree to find depth
        while (currentId != null && depth < MAX_HIERARCHY_DEPTH) {
            depth++;
            currentId = categoryParentMap.get(currentId);
        }
        
        return depth;
    }
    
    /**
     * Get the number of direct children for a category
     * @param categoryId The category ID
     * @return Number of children
     */
    private int getChildCategoriesCount(Long categoryId) {
        Set<Long> children = categoryChildrenMap.get(categoryId);
        return children != null ? children.size() : 0;
    }

    /**
     * Update an existing category
     *
     * @param categoryId ID of the category to update
     * @param categoryData Updated category data
     * @return Updated category as DTO
     * @throws CategoryNotFoundException if category doesn't exist
     * @throws CategoryOperationException if update operation fails
     */
    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryData) {
        logger.info("Updating category with ID: {}", categoryId);
        
        if (categoryId == null || categoryId <= 0) {
            logger.error("Invalid category ID for update: {}", categoryId);
            throw new IllegalArgumentException("Valid category ID is required");
        }
        
        if (categoryData == null) {
            logger.error("Cannot update category: Data is null");
            throw new IllegalArgumentException("Category data cannot be null");
        }
        
        // Fetch existing category
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            logger.error("Cannot update category: Category not found with ID: {}", categoryId);
            throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
        }
        
        Category existingCategory = categoryOpt.get();
        Long oldParentId = existingCategory.getParentId();
        Long newParentId = categoryData.getParentId();
        
        // Check if parent is changing
        boolean isChangingParent = (newParentId != null && !newParentId.equals(oldParentId)) || 
                                   (newParentId == null && oldParentId != null);
        
        // Special validations for parent change
        if (isChangingParent) {
            // Check for circular reference
            if (newParentId != null && (newParentId.equals(categoryId) || isDescendantOf(newParentId, categoryId))) {
                logger.error("Cannot update category: Would create circular reference");
                throw new CategoryOperationException("Cannot move category to its own descendant");
            }
            
            // Check hierarchy depth
            if (newParentId != null) {
                int depth = calculateHierarchyDepth(newParentId);
                if (depth >= MAX_HIERARCHY_DEPTH - 1) { // -1 because we're adding one more level
                    logger.error("Cannot update category: Maximum hierarchy depth would be exceeded");
                    throw new CategoryOperationException("Maximum category hierarchy depth would be exceeded");
                }
                
                // Check number of children for new parent
                int childrenCount = getChildCategoriesCount(newParentId);
                if (childrenCount >= MAX_CHILDREN_PER_CATEGORY) {
                    logger.error("Cannot update category: New parent already has maximum children");
                    throw new CategoryOperationException("New parent category already has maximum number of children");
                }
            }
        }
        
        try {
            // Update fields from DTO
            if (categoryData.getName() != null && !categoryData.getName().trim().isEmpty()) {
                existingCategory.setName(categoryData.getName());
            }
            
            if (categoryData.getDescription() != null) {
                existingCategory.setDescription(categoryData.getDescription());
            }
            
            if (categoryData.isActive() != null) {
                existingCategory.setActive(categoryData.isActive());
            }
            
            if (categoryData.getDisplayOrder() != null) {
                existingCategory.setDisplayOrder(categoryData.getDisplayOrder());
            }
            
            if (categoryData.getMetadata() != null) {
                existingCategory.setMetadata(categoryData.getMetadata());
            }
            
            // Handle parent change
            if (isChangingParent) {
                existingCategory.setParentId(newParentId);
                
                // Update internal maps
                // Remove from old parent's children
                if (oldParentId != null) {
                    Set<Long> oldSiblings = categoryChildrenMap.get(oldParentId);
                    if (oldSiblings != null) {
                        oldSiblings.remove(categoryId);
                    }
                } else {
                    Set<Long> rootCategories = categoryChildrenMap.get(0L);
                    if (rootCategories != null) {
                        rootCategories.remove(categoryId);
                    }
                }
                
                // Add to new parent's children
                if (newParentId != null) {
                    categoryParentMap.put(categoryId, newParentId);
                    categoryChildrenMap.computeIfAbsent(newParentId, k -> new HashSet<>()).add(categoryId);
                } else {
                    categoryParentMap.remove(categoryId);
                    categoryChildrenMap.computeIfAbsent(0L, k -> new HashSet<>()).add(categoryId);
                }
            }
            
            existingCategory.setUpdatedAt(new Date());
            
            // Save updated category
            Category updatedCategory = categoryRepository.save(existingCategory);
            
            // Invalidate caches
            invalidateCategoryCaches();
            
            logger.info("Category updated successfully: {}", updatedCategory.getId());
            return convertToDTO(updatedCategory);
            
        } catch (Exception e) {
            logger.error("Error updating category: {}", e.getMessage(), e);
            throw new CategoryOperationException("Failed to update category: " + e.getMessage(), e);
        }
    }

    /**
     * Check if potentialAncestor is a descendant of categoryId
     * @param potentialDescendant The category to check
     * @param ancestorId The potential ancestor
     * @return true if potentialDescendant is a descendant of ancestorId
     */
    private boolean isDescendantOf(Long potentialDescendant, Long ancestorId) {
        Long currentId = potentialDescendant;
        Set<Long> visited = new HashSet<>();
        
        while (currentId != null) {
            // Prevent infinite loops in case of corrupted data
            if (visited.contains(currentId)) {
                logger.warn("Circular reference detected in category hierarchy for ID: {}", currentId);
                return false;
            }
            visited.add(currentId);
            
            if (currentId.equals(ancestorId)) {
                return true;
            }
            
            currentId = categoryParentMap.get(currentId);
        }
        
        return false;
    }

    /**
     * Delete a category
     *
     * @param categoryId ID of the category to delete
     * @throws CategoryNotFoundException if category doesn't exist
     * @throws CategoryOperationException if deletion fails or category has children
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        logger.info("Deleting category with ID: {}", categoryId);
        
        if (categoryId == null || categoryId <= 0) {
            logger.error("Invalid category ID for deletion: {}", categoryId);
            throw new IllegalArgumentException("Valid category ID is required");
        }
        
        // Check if category exists
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            logger.error("Cannot delete category: Category not found with ID: {}", categoryId);
            throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
        }
        
        // Check if category has children
        Set<Long> children = categoryChildrenMap.get(categoryId);
        if (children != null && !children.isEmpty()) {
            logger.error("Cannot delete category: Has {} child categories", children.size());
            throw new CategoryOperationException(
                "Cannot delete category with ID: " + categoryId + " because it has " + children.size() + " child categories");
        }
        
        try {
            // Update internal maps
            Long parentId = categoryParentMap.get(categoryId);
            if (parentId != null) {
                Set<Long> siblings = categoryChildrenMap.get(parentId);
                if (siblings != null) {
                    siblings.remove(categoryId);
                }
                categoryParentMap.remove(categoryId);
            } else {
                // Was a root category
                Set<Long> rootCategories = categoryChildrenMap.get(0L);
                if (rootCategories != null) {
                    rootCategories.remove(categoryId);
                }
            }
            
            categoryChildrenMap.remove(categoryId);
            
            // Delete from database
            categoryRepository.deleteById(categoryId);
            
            // Invalidate caches
            invalidateCategoryCaches();
            
            logger.info("Category deleted successfully: {}", categoryId);
            
        } catch (Exception e) {
            logger.error("Error deleting category: {}", e.getMessage(), e);
            throw new CategoryOperationException("Failed to delete category: " + e.getMessage(), e);
        }
    }

    /**
     * Get category by ID
     *
     * @param categoryId ID of the category to retrieve
     * @return Category data as DTO
     * @throws CategoryNotFoundException if category doesn't exist
     */
    public CategoryDTO getCategoryById(Long categoryId) {
        logger.debug("Getting category by ID: {}", categoryId);
        
        if (categoryId == null || categoryId <= 0) {
            logger.error("Invalid category ID: {}", categoryId);
            throw new IllegalArgumentException("Valid category ID is required");
        }
        
        // Try to get from cache first
        String cacheKey = "category_" + categoryId;
        CategoryDTO cachedCategory = cacheService.get(cacheKey, CategoryDTO.class);
        if (cachedCategory != null) {
            logger.debug("Category found in cache: {}", categoryId);
            return cachedCategory;
        }
        
        // Fetch from database
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            logger.error("Category not found with ID: {}", categoryId);
            throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
        }
        
        CategoryDTO categoryDTO = convertToDTO(categoryOpt.get());
        
        // Cache the result
        cacheService.put(cacheKey, categoryDTO);
        
        return categoryDTO;
    }

    /**
     * Get all root categories (categories without a parent)
     *
     * @return List of root categories as DTOs
     */
    public List<CategoryDTO> getRootCategories() {
        logger.debug("Getting root categories");
        
        // Try cache first
        @SuppressWarnings("unchecked")
        List<CategoryDTO> cachedRootCategories = cacheService.get(CACHE_KEY_ROOT_CATEGORIES, List.class);
        if (cachedRootCategories != null) {
            logger.debug("Root categories found in cache");
            return cachedRootCategories;
        }
        
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();
        List<CategoryDTO> rootCategoryDTOs = rootCategories.stream()
                .filter(Category::isActive)  // Only return active categories
                .sorted(Comparator.comparing(Category::getDisplayOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // Cache the result
        cacheService.put(CACHE_KEY_ROOT_CATEGORIES, rootCategoryDTOs);
        
        logger.debug("Found {} root categories", rootCategoryDTOs.size());
        return rootCategoryDTOs;
    }

    /**
     * Get child categories of a specific parent
     *
     * @param parentId ID of the parent category
     * @return List of child categories as DTOs
     */
    public List<CategoryDTO> getChildCategories(Long parentId) {
        logger.debug("Getting child categories for parent ID: {}", parentId);
        
        if (parentId == null || parentId <= 0) {
            logger.error("Invalid parent category ID: {}", parentId);
            throw new IllegalArgumentException("Valid parent category ID is required");
        }
        
        // Check if parent exists
        Optional<Category> parentOpt = categoryRepository.findById(parentId);
        if (!parentOpt.isPresent()) {
            logger.error("Parent category not found with ID: {}", parentId);
            throw new CategoryNotFoundException("Parent category not found with ID: " + parentId);
        }
        
        // Try cache first
        String cacheKey = "children_" + parentId;
        @SuppressWarnings("unchecked")
        List<CategoryDTO> cachedChildren = cacheService.get(cacheKey, List.class);
        if (cachedChildren != null) {
            logger.debug("Child categories found in cache for parent: {}", parentId);
            return cachedChildren;
        }
        
        List<Category> childCategories = categoryRepository.findByParentId(parentId);
        List<CategoryDTO> childCategoryDTOs = childCategories.stream()
                .filter(Category::isActive)  // Only return active categories
                .sorted(Comparator.comparing(Category::getDisplayOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // Cache the result
        cacheService.put(cacheKey, childCategoryDTOs);
        
        logger.debug("Found {} child categories for parent: {}", childCategoryDTOs.size(), parentId);
        return childCategoryDTOs;
    }

    /**
     * Get complete category tree structure
     *
     * @return List of root categories with nested children as DTOs
     */
    public List<CategoryDTO> getCategoryTree() {
        logger.debug("Getting complete category tree");
        
        // Try cache first
        @SuppressWarnings("unchecked")
        List<CategoryDTO> cachedTree = cacheService.get(CACHE_KEY_CATEGORY_TREE, List.class);
        if (cachedTree != null) {
            logger.debug("Category tree found in cache");
            return cachedTree;
        }
        
        // Get root categories
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();
        
        // Convert to DTOs with children
        List<CategoryDTO> rootCategoryDTOs = rootCategories.stream()
                .filter(Category::isActive)  // Only include active categories
                .sorted(Comparator.comparing(Category::getDisplayOrder))
                .map(root -> convertToDTOWithChildren(root, new HashSet<>()))
                .collect(Collectors.toList());
        
        // Cache the result
        cacheService.put(CACHE_KEY_CATEGORY_TREE, rootCategoryDTOs);
        
        logger.debug("Category tree built with {} root categories", rootCategoryDTOs.size());
        return rootCategoryDTOs;
    }

    /**
     * Recursively convert category to DTO including its children
     * @param category The category to convert
     * @param processedIds Set of already processed IDs to prevent infinite recursion
     * @return Category DTO with children
     */
    private CategoryDTO convertToDTOWithChildren(Category category, Set<Long> processedIds) {
        CategoryDTO dto = convertToDTO(category);
        Long categoryId = category.getId();
        
        // Prevent infinite recursion
        if (processedIds.contains(categoryId)) {
            logger.warn("Circular reference detected in category hierarchy for ID: {}", categoryId);
            return dto;
        }
        
        processedIds.add(categoryId);
        
        // Get children if any
        Set<Long> childIds = categoryChildrenMap.get(categoryId);
        if (childIds != null && !childIds.isEmpty()) {
            List<Category> childCategories = categoryRepository.findAllById(childIds);
            List<CategoryDTO> childDTOs = childCategories.stream()
                    .filter(Category::isActive)  // Only include active categories
                    .sorted(Comparator.comparing(Category::getDisplayOrder))
                    .map(child -> convertToDTOWithChildren(child, new HashSet<>(processedIds)))
                    .collect(Collectors.toList());
            
            dto.setChildren(childDTOs);
        } else {
            dto.setChildren(Collections.emptyList());
        }
        
        return dto;
    }

    /**
     * Move category to a new parent
     *
     * @param categoryId ID of the category to move
     * @param newParentId ID of the new parent category (can be null for root)
     * @throws CategoryNotFoundException if category or parent doesn't exist
     * @throws CategoryOperationException if move operation fails
     */
    @Transactional
    public void moveCategory(Long categoryId, Long newParentId) {
        logger.info("Moving category {} to new parent {}", categoryId, newParentId);
        
        if (categoryId == null || categoryId <= 0) {
            logger.error("Invalid category ID for move: {}", categoryId);
            throw new IllegalArgumentException("Valid category ID is required");
        }
        
        // Check if category exists
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            logger.error("Cannot move category: Category not found with ID: {}", categoryId);
            throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
        }
        
        Category category = categoryOpt.get();
        Long oldParentId = category.getParentId();
        
        // Don't do anything if parent is not changing
        if ((oldParentId == null && newParentId == null) || 
            (oldParentId != null && oldParentId.equals(newParentId))) {
            logger.info("Category {} already has parent {}, no move needed", categoryId, newParentId);
            return;
        }
        
        // Check if new parent exists (if not null)
        if (newParentId != null) {
            Optional<Category> newParentOpt = categoryRepository.findById(newParentId);
            if (!newParentOpt.isPresent()) {
                logger.error("Cannot move category: New parent not found with ID: {}", newParentId);
                throw new CategoryNotFoundException("New parent category not found with ID: " + newParentId);
            }
            
            // Check for circular reference
            if (newParentId.equals(categoryId) || isDescendantOf(newParentId, categoryId)) {
                logger.error("Cannot move category: Would create circular reference");
                throw new CategoryOperationException("Cannot move category to its own descendant");
            }
            
            // Check hierarchy depth
            int depth = calculateHierarchyDepth(newParentId);
            if (depth >= MAX_HIERARCHY_DEPTH - 1) { // -1 because we're adding one more level
                logger.error("Cannot move category: Maximum hierarchy depth would be exceeded");
                throw new CategoryOperationException("Maximum category hierarchy depth would be exceeded");
            }
            
            // Check number of children for new parent
            int childrenCount = getChildCategoriesCount(newParentId);
            if (childrenCount >= MAX_CHILDREN_PER_CATEGORY) {
                logger.error("Cannot move category: New parent already has maximum children");
                throw new CategoryOperationException("New parent category already has maximum number of children");
            }
        }
        
        try {
            // Update category with new parent
            category.setParentId(newParentId);
            category.setUpdatedAt(new Date());
            
            // Recalculate display order based on new siblings
            category.setDisplayOrder(calculateNextDisplayOrder(newParentId));
            
            categoryRepository.save(category);
            
            // Update internal maps
            // Remove from old parent's children
            if (oldParentId != null) {
                Set<Long> oldSiblings = categoryChildrenMap.get(oldParentId);
                if (oldSiblings != null) {
                    oldSiblings.remove(categoryId);
                }
            } else {
                Set<Long> rootCategories = categoryChildrenMap.get(0L);
                if (rootCategories != null) {
                    rootCategories.remove(categoryId);
                }
            }
            
            // Add to new parent's children
            if (newParentId != null) {
                categoryParentMap.put(categoryId, newParentId);
                categoryChildrenMap.computeIfAbsent(newParentId, k -> new HashSet<>()).add(categoryId);
            } else {
                categoryParentMap.remove(categoryId);
                categoryChildrenMap.computeIfAbsent(0L, k -> new HashSet<>()).add(categoryId);
            }
            
            // Invalidate caches
            invalidateCategoryCaches();
            
            logger.info("Category {} successfully moved to parent {}", categoryId, newParentId);
            
        } catch (Exception e) {
            logger.error("Error moving category: {}", e.getMessage(), e);
            throw new CategoryOperationException("Failed to move category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert Category entity to CategoryDTO
     * @param category The Category entity
     * @return The corresponding DTO
     */
    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setParentId(category.getParentId());
        dto.setActive(category.isActive());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setMetadata(category.getMetadata());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Invalidate all category-related caches
     */
    private void invalidateCategoryCaches() {
        logger.debug("Invalidating category caches");
        cacheService.remove(CACHE_KEY_ALL_CATEGORIES);
        cacheService.remove(CACHE_KEY_ROOT_CATEGORIES);
        cacheService.remove(CACHE_KEY_CATEGORY_TREE);
        
        // Also invalidate individual category caches
        // This is a simplistic approach; in a real system you might want to be more selective
        // TODO: Implement more targeted cache invalidation
    }
    
    // FIXME: The category move operation doesn't update products that reference the category
    // FIXME: Need to handle cache invalidation more selectively for better performance
}