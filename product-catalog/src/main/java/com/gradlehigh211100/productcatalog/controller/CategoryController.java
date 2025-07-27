package com.gradlehigh211100.productcatalog.controller;

import com.gradlehigh211100.productcatalog.dto.CategoryDTO;
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
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    
    private final CategoryService categoryService;
    private Map<String, Object> categoryCache;
    
    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
        this.categoryCache = new HashMap<>();
    }

    /**
     * Create new category endpoint
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
            
            // Try to create the category
            CategoryDTO createdCategory = categoryService.createCategory(categoryData);
            
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
            }
            
            // Set the ID to ensure we're updating the right entity
            categoryData.setId(categoryId);
            
            // Update the category
            CategoryDTO updatedCategory = categoryService.updateCategory(categoryId, categoryData);
            
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
            
            // Check if category has associated products
            if (categoryService.hasDependencies(categoryId)) {
                logger.warn("Cannot delete category {} because it has dependencies", categoryId);
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
            
            // Perform deletion
            categoryService.deleteCategory(categoryId);
            
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
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable("categoryId") Long categoryId) {
        logger.debug("Getting category by ID: {}", categoryId);
        
        try {
            // Retrieve category from service
            CategoryDTO category = categoryService.getCategoryById(categoryId);
            
            if (category == null) {
                logger.warn("Category not found with ID: {}", categoryId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
     */
    @GetMapping("/roots")
    public ResponseEntity<List<CategoryDTO>> getRootCategories() {
        logger.debug("Getting root categories");
        
        try {
            List<CategoryDTO> rootCategories = categoryService.getRootCategories();
            logger.debug("Retrieved {} root categories", rootCategories.size());
            return new ResponseEntity<>(rootCategories, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving root categories", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get child categories endpoint
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CategoryDTO>> getChildCategories(@PathVariable("parentId") Long parentId) {
        logger.debug("Getting child categories for parent ID: {}", parentId);
        
        try {
            // Check if parent category exists
            if (!categoryService.checkCategoryExists(parentId)) {
                logger.warn("Parent category not found: {}", parentId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Get child categories
            List<CategoryDTO> childCategories = categoryService.getChildCategories(parentId);
            
            logger.debug("Retrieved {} child categories for parent ID: {}", childCategories.size(), parentId);
            return new ResponseEntity<>(childCategories, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving child categories for parent ID: " + parentId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Clear the category cache when data changes
     */
    private void clearCategoryCache() {
        logger.debug("Clearing category cache");
        categoryCache.clear();
    }
}