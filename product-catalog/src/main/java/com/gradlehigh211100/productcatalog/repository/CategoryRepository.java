package com.gradlehigh211100.productcatalog.repository;

import com.gradlehigh211100.productcatalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository interface for Category entities.
 * Provides CRUD operations and specialized queries for Category hierarchies.
 * 
 * This repository manages category data persistence and offers methods
 * for hierarchical data queries to navigate and filter the category tree structure.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find all root categories (categories without parents)
     * These represent the top level of the category hierarchy.
     *
     * @return List of root categories
     */
    List<Category> findByParentIsNull();
    
    /**
     * Find all child categories for a given parent ID
     * 
     * @param parentId The ID of the parent category
     * @return List of child categories
     */
    List<Category> findByParentId(Long parentId);
    
    /**
     * Find all active categories in the system
     * Active categories are available for product association.
     *
     * @return List of active categories
     */
    List<Category> findByIsActiveTrue();
    
    /**
     * Find categories at a specific hierarchical level
     * Level 0 represents root categories, level 1 their children, and so on.
     * 
     * @param level The hierarchical level to query for
     * @return List of categories at the specified level
     */
    List<Category> findByLevel(Integer level);
    
    /**
     * Find categories by partial name match (case insensitive)
     * 
     * @param name The search string to match against category names
     * @return List of categories with names containing the search string
     */
    List<Category> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find top selling categories based on associated product sales
     * FIXME: Performance issues with large product catalogs
     * 
     * @param limit Maximum number of categories to return
     * @return List of top selling categories
     */
    @Query(value = "SELECT c FROM Category c JOIN c.products p " +
           "GROUP BY c.id ORDER BY SUM(p.salesCount) DESC")
    List<Category> findTopSellingCategories(@Param("limit") int limit);
    
    /**
     * Find categories that have no associated products
     * 
     * @return List of empty categories
     */
    @Query("SELECT c FROM Category c WHERE c.products IS EMPTY")
    List<Category> findEmptyCategories();
    
    /**
     * Find categories with nested children beyond specified depth
     * Categories with deep nesting may need optimization in UI rendering
     * 
     * TODO: Add pagination support for better performance
     * 
     * @param maxDepth Maximum acceptable depth
     * @return List of categories with excessive nesting
     */
    @Query(value = "WITH RECURSIVE CategoryHierarchy AS (" +
           "SELECT id, name, parent_id, 1 AS depth FROM category WHERE parent_id IS NOT NULL " +
           "UNION ALL " +
           "SELECT c.id, c.name, c.parent_id, ch.depth + 1 FROM category c " +
           "JOIN CategoryHierarchy ch ON c.parent_id = ch.id) " +
           "SELECT DISTINCT c.* FROM category c JOIN CategoryHierarchy ch ON c.id = ch.id " +
           "WHERE ch.depth > :maxDepth", nativeQuery = true)
    List<Category> findCategoriesWithDeepNesting(@Param("maxDepth") int maxDepth);
    
    /**
     * Calculate total number of products in a category and all its subcategories
     * 
     * @param categoryId The parent category ID
     * @return Total product count across the category hierarchy
     */
    @Query(value = "WITH RECURSIVE CategoryTree AS (" +
           "SELECT id FROM category WHERE id = :categoryId " +
           "UNION ALL " +
           "SELECT c.id FROM category c JOIN CategoryTree ct ON c.parent_id = ct.id) " +
           "SELECT COUNT(p.id) FROM product p WHERE p.category_id IN (SELECT id FROM CategoryTree)", 
           nativeQuery = true)
    Long countProductsInCategoryHierarchy(@Param("categoryId") Long categoryId);
}