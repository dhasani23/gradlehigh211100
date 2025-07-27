package com.gradlehigh211100.productcatalog.controller;

import com.gradlehigh211100.productcatalog.model.Product;
import com.gradlehigh211100.productcatalog.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for product search operations.
 * Exposes endpoints for searching products using various criteria.
 */
@RestController
@RequestMapping("/api/products/search")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @Autowired
    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    /**
     * Search products by name.
     *
     * @param name The name to search for
     * @return List of matching products
     */
    @GetMapping("/name")
    public ResponseEntity<List<Product>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(productSearchService.searchByName(name));
    }

    /**
     * Search products by description.
     *
     * @param description The description to search for
     * @return List of matching products
     */
    @GetMapping("/description")
    public ResponseEntity<List<Product>> searchByDescription(@RequestParam String description) {
        return ResponseEntity.ok(productSearchService.searchByDescription(description));
    }

    /**
     * Search products by tags.
     *
     * @param tags The tags to search for
     * @return List of matching products
     */
    @GetMapping("/tags")
    public ResponseEntity<List<Product>> searchByTags(@RequestParam String tags) {
        return ResponseEntity.ok(productSearchService.searchByTags(tags));
    }

    /**
     * Search products by category name.
     *
     * @param categoryName The category name to search for
     * @return List of matching products
     */
    @GetMapping("/category")
    public ResponseEntity<List<Product>> searchByCategory(@RequestParam String categoryName) {
        return ResponseEntity.ok(productSearchService.searchByCategoryName(categoryName));
    }

    /**
     * Search products by brand.
     *
     * @param brand The brand to search for
     * @return List of matching products
     */
    @GetMapping("/brand")
    public ResponseEntity<List<Product>> searchByBrand(@RequestParam String brand) {
        return ResponseEntity.ok(productSearchService.searchByBrand(brand));
    }

    /**
     * Advanced search with multiple criteria.
     *
     * @param term The general search term
     * @param brand Optional brand filter
     * @param category Optional category filter
     * @param minPrice Optional minimum price
     * @param maxPrice Optional maximum price
     * @param tags Optional tags to filter by
     * @param inStock If true, only return products in stock
     * @return List of products matching all criteria
     */
    @GetMapping("/complex")
    public ResponseEntity<List<Product>> complexSearch(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false, defaultValue = "false") boolean inStock) {
        
        return ResponseEntity.ok(productSearchService.complexSearch(term, brand, category, 
                                                                minPrice, maxPrice, tags, inStock));
    }

    /**
     * Union search across all fields.
     *
     * @param term The search term
     * @return List of products matching in any field
     */
    @GetMapping("/union")
    public ResponseEntity<List<Product>> unionSearch(@RequestParam String term) {
        return ResponseEntity.ok(productSearchService.unionSearch(term));
    }

    /**
     * Fuzzy search to handle typos.
     *
     * @param term The possibly misspelled search term
     * @return List of products matching the fuzzy search
     */
    @GetMapping("/fuzzy")
    public ResponseEntity<List<Product>> fuzzySearch(@RequestParam String term) {
        return ResponseEntity.ok(productSearchService.fuzzySearch(term));
    }
}