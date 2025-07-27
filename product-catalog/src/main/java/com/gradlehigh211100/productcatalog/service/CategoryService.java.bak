package com.gradlehigh211100.productcatalog.service;

import com.gradlehigh211100.productcatalog.dto.CategoryDTO;
import com.gradlehigh211100.productcatalog.entity.Category;
import com.gradlehigh211100.productcatalog.exception.CategoryNotFoundException;
import com.gradlehigh211100.productcatalog.exception.CategoryOperationException;
import com.gradlehigh211100.productcatalog.repository.CategoryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing category operations
 */
@Service
public class CategoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private static final int MAX_HIERARCHY_DEPTH = 5;
    private static final int MAX_CHILDREN_PER_CATEGORY = 100;
    private static final String CACHE_KEY_ROOT_CATEGORIES = "root_categories";
    
    private final CategoryRepository categoryRepository;
    private final CacheService cacheService;
    
    @Autowired
    public CategoryService(CategoryRepository categoryRepository, CacheService cacheService) {
        this.categoryRepository = categoryRepository;
        this.cacheService = cacheService;
    }
    
    /**
     * Check if a category exists
     */
    public boolean checkCategoryExists(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        return categoryRepository.existsById(categoryId);
    }
    
    /**
     * Check if a category has dependencies
     */
    public boolean hasDependencies(Long categoryId) {
        // This is a placeholder - in a real app this would check for products or other entities
        return false;
    }
    
    /**
     * Get all categories in the system
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        List<CategoryDTO> categoryDTOs = new ArrayList<>();
        
        for (Category category : allCategories) {
            categoryDTOs.add(convertToDTO(category));
        }
        
        return categoryDTOs;
    }
    
    /**
     * Create a new category
     */
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryData) {
        try {
            validateCategoryData(categoryData);
            
            Category category = new Category();
            category.setName(categoryData.getName());
            category.setDescription(categoryData.getDescription());
            category.setParentId(categoryData.getParentId());
            category.setActive(categoryData.getActive() != null ? categoryData.getActive() : true);
            category.setDisplayOrder(categoryData.getDisplayOrder() != null ? 
                    categoryData.getDisplayOrder() : calculateNextDisplayOrder(categoryData.getParentId()));
            category.setMetadata(categoryData.getMetadata());
            category.setCreatedAt(LocalDateTime.now());
            
            // Save category to database
            Category savedCategory = categoryRepository.save(category);
            
            // Convert and return as DTO
            return convertToDTO(savedCategory);
        } catch (Exception e) {
            logger.error("Error creating category", e);
            throw new CategoryOperationException("Failed to create category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate next display order for a new category
     */
    private int calculateNextDisplayOrder(Long parentId) {
        List<Category> siblings;
        
        if (parentId == null) {
            siblings = categoryRepository.findByParentIdIsNull();
        } else {
            siblings = categoryRepository.findByParentId(parentId);
        }
        
        if (siblings.isEmpty()) {
            return 1;
        }
        
        return siblings.stream()
                .mapToInt(Category::getDisplayOrder)
                .max()
                .orElse(0) + 1;
    }
    
    /**
     * Validate category data before creation or update
     */
    private void validateCategoryData(CategoryDTO categoryData) {
        if (categoryData.getName() == null || categoryData.getName().trim().isEmpty()) {
            throw new CategoryOperationException("Category name is required");
        }
        
        if (categoryData.getName().length() < 2 || categoryData.getName().length() > 50) {
            throw new CategoryOperationException("Category name must be between 2 and 50 characters");
        }
        
        if (categoryData.getParentId() != null) {
            Optional<Category> parentCategory = categoryRepository.findById(categoryData.getParentId());
            
            if (!parentCategory.isPresent()) {
                throw new CategoryNotFoundException("Parent category not found with ID: " + categoryData.getParentId());
            }
            
            // Check hierarchy depth to prevent too deep nesting
            int depth = calculateHierarchyDepth(parentCategory.get());
            if (depth >= MAX_HIERARCHY_DEPTH) {
                throw new CategoryOperationException("Maximum category hierarchy depth reached");
            }
            
            // Check number of children
            List<Category> siblings = categoryRepository.findByParentId(categoryData.getParentId());
            if (siblings.size() >= MAX_CHILDREN_PER_CATEGORY) {
                throw new CategoryOperationException("Parent category already has maximum number of children");
            }
        }
    }
    
    /**
     * Calculate hierarchy depth of a category
     */
    private int calculateHierarchyDepth(Category category) {
        int depth = 0;
        Long parentId = category.getParentId();
        
        while (parentId != null) {
            depth++;
            Optional<Category> parent = categoryRepository.findById(parentId);
            if (!parent.isPresent()) {
                break;
            }
            parentId = parent.get().getParentId();
        }
        
        return depth;
    }
    
    /**
     * Update an existing category
     */
    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryData) {
        try {
            // Find existing category
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            
            if (!categoryOpt.isPresent()) {
                throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
            }
            
            Category existingCategory = categoryOpt.get();
            
            // Check for circular references
            if (categoryData.getParentId() != null && !categoryData.getParentId().equals(existingCategory.getParentId())) {
                if (categoryData.getParentId().equals(existingCategory.getId())) {
                    throw new CategoryOperationException("Cannot move category to its own descendant");
                }
                
                // Check if new parent would create circular reference
                if (isDescendant(categoryId, categoryData.getParentId())) {
                    throw new CategoryOperationException("Cannot move category to its own descendant");
                }
                
                // Check hierarchy depth
                Optional<Category> newParentOpt = categoryRepository.findById(categoryData.getParentId());
                if (newParentOpt.isPresent()) {
                    int depth = calculateHierarchyDepth(newParentOpt.get());
                    if (depth >= MAX_HIERARCHY_DEPTH - 1) {
                        throw new CategoryOperationException("Maximum category hierarchy depth would be exceeded");
                    }
                    
                    // Check children count
                    List<Category> siblings = categoryRepository.findByParentId(categoryData.getParentId());
                    if (siblings.size() >= MAX_CHILDREN_PER_CATEGORY) {
                        throw new CategoryOperationException("New parent category already has maximum number of children");
                    }
                }
                
                // Update parent ID
                existingCategory.setParentId(categoryData.getParentId());
            }
            
            // Update basic properties
            if (categoryData.getName() != null && !categoryData.getName().isEmpty()) {
                existingCategory.setName(categoryData.getName());
            }
            
            if (categoryData.getDescription() != null) {
                existingCategory.setDescription(categoryData.getDescription());
            }
            
            if (categoryData.getActive() != null) {
                existingCategory.setActive(categoryData.getActive());
            }
            
            if (categoryData.getDisplayOrder() != null) {
                existingCategory.setDisplayOrder(categoryData.getDisplayOrder());
            }
            
            if (categoryData.getMetadata() != null) {
                existingCategory.setMetadata(categoryData.getMetadata());
            }
            
            // Update timestamp
            existingCategory.setUpdatedAt(LocalDateTime.now());
            
            // Save updated category
            Category updatedCategory = categoryRepository.save(existingCategory);
            
            // Clear cache for this category
            String cacheKey = "category_" + categoryId;
            cacheService.remove(cacheKey);
            
            // Also clear cache for parent categories and root categories
            cacheService.remove(CACHE_KEY_ROOT_CATEGORIES);
            
            return convertToDTO(updatedCategory);
        } catch (CategoryNotFoundException | CategoryOperationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating category", e);
            throw new CategoryOperationException("Failed to update category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if one category is a descendant of another
     */
    private boolean isDescendant(Long ancestorId, Long descendantId) {
        if (descendantId == null) {
            return false;
        }
        
        Optional<Category> category = categoryRepository.findById(descendantId);
        if (!category.isPresent()) {
            return false;
        }
        
        Long parentId = category.get().getParentId();
        
        if (parentId == null) {
            return false;
        }
        
        if (parentId.equals(ancestorId)) {
            return true;
        }
        
        return isDescendant(ancestorId, parentId);
    }
    
    /**
     * Delete a category
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        try {
            // Verify category exists
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            
            if (!categoryOpt.isPresent()) {
                throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
            }
            
            // Check if category has children
            List<Category> children = categoryRepository.findByParentId(categoryId);
            if (!children.isEmpty()) {
                throw new CategoryOperationException(
                    "Cannot delete category with children. Found " + children.size() + " child categories.");
            }
            
            // Perform the deletion
            categoryRepository.deleteById(categoryId);
            
            // Clear cache for this category and related collections
            String cacheKey = "category_" + categoryId;
            cacheService.remove(cacheKey);
            cacheService.remove(CACHE_KEY_ROOT_CATEGORIES);
            cacheService.remove("category_tree");
            
            // If the category has a parent, clear that parent's children cache
            Category category = categoryOpt.get();
            if (category.getParentId() != null) {
                cacheService.remove("children_" + category.getParentId());
            }
        } catch (CategoryNotFoundException | CategoryOperationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting category", e);
            throw new CategoryOperationException("Failed to delete category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a category by ID
     */
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long categoryId) {
        // Try to get from cache
        String cacheKey = "category_" + categoryId;
        CategoryDTO cachedCategory = cacheService.get(cacheKey, CategoryDTO.class);
        
        if (cachedCategory != null) {
            return cachedCategory;
        }
        
        // Not in cache, get from repository
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        
        if (!categoryOpt.isPresent()) {
            throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
        }
        
        // Convert to DTO
        CategoryDTO categoryDTO = convertToDTO(categoryOpt.get());
        
        // Cache for future use
        cacheService.put(cacheKey, categoryDTO);
        
        return categoryDTO;
    }
    
    /**
     * Get all root categories
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getRootCategories() {
        // Try to get from cache
        List<CategoryDTO> cachedRootCategories = cacheService.get(CACHE_KEY_ROOT_CATEGORIES, List.class);
        
        if (cachedRootCategories != null) {
            return cachedRootCategories;
        }
        
        // Not in cache, get from repository
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();
        
        List<CategoryDTO> rootCategoryDTOs = rootCategories.stream()
                .filter(Category::isActive)  // Only return active categories
                .sorted(Comparator.comparing(Category::getDisplayOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // Cache for future use
        cacheService.put(CACHE_KEY_ROOT_CATEGORIES, rootCategoryDTOs);
        
        return rootCategoryDTOs;
    }
    
    /**
     * Get child categories of a parent category
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildCategories(Long parentId) {
        // Check if parent exists
        Optional<Category> parentOpt = categoryRepository.findById(parentId);
        
        if (!parentOpt.isPresent()) {
            throw new CategoryNotFoundException("Parent category not found with ID: " + parentId);
        }
        
        // Get children
        List<Category> children = categoryRepository.findByParentId(parentId);
        
        // Convert to DTOs
        return children.stream()
                .filter(Category::isActive)
                .sorted(Comparator.comparing(Category::getDisplayOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert entity to DTO with child information
     */
    private CategoryDTO convertToDTOWithChildren(Category category, Set<Long> processedIds) {
        CategoryDTO dto = convertToDTO(category);
        
        // Prevent infinite recursion by tracking processed IDs
        if (processedIds.contains(category.getId())) {
            return dto;
        }
        
        processedIds.add(category.getId());
        
        // Get children
        List<Category> childEntities = categoryRepository.findByParentId(category.getId());
        
        if (childEntities != null && !childEntities.isEmpty()) {
            List<CategoryDTO> childDTOs = new ArrayList<>();
            for (Category child : childEntities) {
                // Only include active children
                if (Boolean.TRUE.equals(child.isActive())) {
                    childDTOs.add(convertToDTOWithChildren(child, new HashSet<>(processedIds)));
                }
            }
            dto.setChildren(childDTOs);
        }
        
        return dto;
    }
    
    /**
     * Convert entity to basic DTO
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
}