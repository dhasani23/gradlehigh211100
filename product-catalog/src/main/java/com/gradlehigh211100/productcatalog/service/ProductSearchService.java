package com.gradlehigh211100.productcatalog.service;

import com.gradlehigh211100.productcatalog.model.Product;
import com.gradlehigh211100.productcatalog.repository.ProductSearchRepository;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for handling complex product search operations.
 * This service demonstrates how to use the ProductSearchRepository
 * and adds additional complex search functionality with high cyclomatic complexity.
 */
@Service
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public ProductSearchService(ProductSearchRepository productSearchRepository,
                               ElasticsearchOperations elasticsearchOperations) {
        this.productSearchRepository = productSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Search products by name
     */
    public List<Product> searchByName(String name) {
        return productSearchRepository.findByNameContaining(name);
    }

    /**
     * Search products by description
     */
    public List<Product> searchByDescription(String description) {
        return productSearchRepository.findByDescriptionContaining(description);
    }

    /**
     * Search products by tags
     */
    public List<Product> searchByTags(String tags) {
        return productSearchRepository.findByTagsContaining(tags);
    }

    /**
     * Search products by category name
     */
    public List<Product> searchByCategoryName(String categoryName) {
        return productSearchRepository.findByCategoryNameContaining(categoryName);
    }

    /**
     * Search products by brand
     */
    public List<Product> searchByBrand(String brand) {
        return productSearchRepository.findByBrandContaining(brand);
    }

    /**
     * Complex search with multiple criteria.
     * This method demonstrates high cyclomatic complexity by implementing a complex
     * search algorithm that combines multiple search criteria.
     *
     * @param searchTerm The general search term to look for across multiple fields
     * @param brand Optional brand filter
     * @param category Optional category filter
     * @param minPrice Optional minimum price
     * @param maxPrice Optional maximum price
     * @param tags Optional tags to filter by
     * @param inStock If true, only return products in stock
     * @return List of products matching the criteria
     */
    public List<Product> complexSearch(String searchTerm, String brand, String category,
                                     Double minPrice, Double maxPrice, List<String> tags,
                                     boolean inStock) {
        
        // Build a complex query with multiple criteria
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Apply search term across multiple fields if provided
        if (searchTerm != null && !searchTerm.isEmpty()) {
            boolQuery.should(QueryBuilders.matchQuery("name", searchTerm))
                    .should(QueryBuilders.matchQuery("description", searchTerm))
                    .minimumShouldMatch(1);
        }
        
        // Apply filters
        if (brand != null && !brand.isEmpty()) {
            boolQuery.filter(QueryBuilders.matchQuery("brand", brand));
        }
        
        if (category != null && !category.isEmpty()) {
            boolQuery.filter(QueryBuilders.matchQuery("categoryName", category));
        }
        
        // Price range filter
        if (minPrice != null || maxPrice != null) {
            if (minPrice != null && maxPrice != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("price").from(minPrice).to(maxPrice));
            } else if (minPrice != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("price").from(minPrice));
            } else {
                boolQuery.filter(QueryBuilders.rangeQuery("price").to(maxPrice));
            }
        }
        
        // Tags filter
        if (tags != null && !tags.isEmpty()) {
            BoolQueryBuilder tagQuery = QueryBuilders.boolQuery();
            for (String tag : tags) {
                tagQuery.should(QueryBuilders.matchQuery("tags", tag));
            }
            boolQuery.filter(tagQuery);
        }
        
        // Stock filter
        if (inStock) {
            boolQuery.filter(QueryBuilders.rangeQuery("stockQuantity").gt(0));
        }
        
        // Create and execute the query
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .build();
        
        SearchHits<Product> searchHits = elasticsearchOperations.search(searchQuery, Product.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Union search that combines results from multiple search methods.
     * Demonstrates high cyclomatic complexity by handling multiple search paths
     * and complex result merging logic.
     *
     * @param term The search term to use across all fields
     * @return Combined unique list of products from all search methods
     */
    public List<Product> unionSearch(String term) {
        if (term == null || term.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<Product> results = new HashSet<>();
        
        // Search across all fields
        results.addAll(searchByName(term));
        results.addAll(searchByDescription(term));
        results.addAll(searchByTags(term));
        results.addAll(searchByCategoryName(term));
        results.addAll(searchByBrand(term));
        
        // FIXME: This approach may lead to duplicates with slightly different data
        // TODO: Implement deduplication based on product ID
        
        return new ArrayList<>(results);
    }

    /**
     * Performs fuzzy search to handle typos and spelling errors.
     * Uses Elasticsearch's fuzzy query capabilities.
     *
     * @param searchTerm The possibly misspelled search term
     * @return List of products matching the fuzzy search
     */
    public List<Product> fuzzySearch(String searchTerm) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.fuzzyQuery("name", searchTerm))
                .should(QueryBuilders.fuzzyQuery("description", searchTerm))
                .should(QueryBuilders.fuzzyQuery("brand", searchTerm))
                .minimumShouldMatch(1);
        
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .build();
        
        SearchHits<Product> searchHits = elasticsearchOperations.search(searchQuery, Product.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}