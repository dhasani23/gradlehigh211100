package com.gradlehigh211100.productcatalog.repository;

import com.gradlehigh211100.productcatalog.model.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository interface for product search operations and full-text search.
 * This repository provides methods for performing complex search operations on Product documents
 * stored in Elasticsearch.
 * 
 * High cyclomatic complexity is achieved through the implementation of multiple search criteria
 * and complex query building in the underlying implementation.
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<Product, String> {

    /**
     * Full-text search by product name.
     * Uses Elasticsearch's text analysis capabilities to find products with names containing the search term.
     *
     * @param name The product name search term
     * @return List of products matching the search criteria
     */
    List<Product> findByNameContaining(String name);

    /**
     * Full-text search by product description.
     * Performs full text analysis on product descriptions to find relevant matches.
     *
     * @param description The description search term
     * @return List of products matching the description search criteria
     */
    List<Product> findByDescriptionContaining(String description);

    /**
     * Search by product tags.
     * Finds products that contain the specified tag in their tag collection.
     *
     * @param tags The tag to search for
     * @return List of products with matching tags
     */
    List<Product> findByTagsContaining(String tags);

    /**
     * Search by category name.
     * Finds products that belong to categories with names containing the search term.
     *
     * @param categoryName The category name to search for
     * @return List of products in matching categories
     */
    List<Product> findByCategoryNameContaining(String categoryName);

    /**
     * Search by brand name.
     * Finds products from brands with names containing the search term.
     *
     * @param brand The brand name to search for
     * @return List of products from matching brands
     */
    List<Product> findByBrandContaining(String brand);

    // FIXME: Current implementation may have performance issues with large result sets
    // TODO: Add pagination support for all search methods
    
    // TODO: Implement custom search method with multiple criteria combining all fields
    
    // TODO: Add faceted search capabilities for product filtering
    
    // TODO: Implement fuzzy search for handling typos in search queries
}