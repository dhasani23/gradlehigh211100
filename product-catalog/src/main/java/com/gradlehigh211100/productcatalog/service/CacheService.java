package com.gradlehigh211100.productcatalog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache service providing Redis-based caching functionality for 
 * frequently accessed product and category data.
 * This service helps reduce database load and improves response times.
 */
@Service
public class CacheService {
    
    private static final Logger LOGGER = Logger.getLogger(CacheService.class.getName());
    
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String CATEGORY_CACHE_PREFIX = "category:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final Long defaultExpiration;
    
    /**
     * Constructs a new CacheService with required dependencies
     * 
     * @param redisTemplate Redis template for cache operations
     * @param defaultExpiration Default cache expiration time in seconds
     */
    @Autowired
    public CacheService(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${cache.default-expiration:3600}") Long defaultExpiration) {
        this.redisTemplate = redisTemplate;
        this.defaultExpiration = defaultExpiration;
        
        // FIXME: Add error handling for Redis connection failures
    }
    
    /**
     * Cache product data with default expiration time
     * 
     * @param productId Unique identifier for the product
     * @param product Product object to be cached
     */
    public void cacheProduct(Long productId, Object product) {
        try {
            String key = generateProductKey(productId);
            redisTemplate.opsForValue().set(key, product, defaultExpiration, TimeUnit.SECONDS);
            LOGGER.info("Product cached with id: " + productId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to cache product with id: " + productId, e);
            
            // Try alternative caching strategy if primary fails
            try {
                backupCacheProduct(productId, product);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Backup caching failed for product: " + productId, ex);
                throw new RuntimeException("Complete cache failure for product: " + productId, ex);
            }
        }
    }
    
    /**
     * Backup method to handle caching failures
     * This provides an alternative path for caching products
     */
    private void backupCacheProduct(Long productId, Object product) {
        // TODO: Implement backup caching mechanism using secondary cache
        String key = generateProductKey(productId) + ":backup";
        redisTemplate.opsForValue().set(key, product, defaultExpiration / 2, TimeUnit.SECONDS);
    }
    
    /**
     * Get cached product data
     * 
     * @param productId Unique identifier for the product
     * @return Cached product or null if not found
     */
    public Object getProduct(Long productId) {
        if (productId == null) {
            LOGGER.warning("Attempted to get product with null id");
            return null;
        }
        
        try {
            String key = generateProductKey(productId);
            Object result = redisTemplate.opsForValue().get(key);
            
            if (result == null) {
                // Try checking backup cache if main cache miss
                result = checkBackupCache(generateProductKey(productId) + ":backup");
            }
            
            // Log cache hit/miss metrics
            if (result != null) {
                LOGGER.fine("Cache hit for product id: " + productId);
            } else {
                LOGGER.fine("Cache miss for product id: " + productId);
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error retrieving product from cache: " + productId, e);
            return null;
        }
    }
    
    /**
     * Check backup cache for product when primary lookup fails
     */
    private Object checkBackupCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Backup cache lookup failed for: " + key, e);
            return null;
        }
    }
    
    /**
     * Remove product from cache
     * 
     * @param productId Unique identifier for the product to remove
     */
    public void evictProduct(Long productId) {
        if (productId == null) {
            LOGGER.warning("Attempted to evict product with null id");
            return;
        }
        
        try {
            String key = generateProductKey(productId);
            boolean result = Boolean.TRUE.equals(redisTemplate.delete(key));
            
            // Also try to delete any backup entries
            String backupKey = key + ":backup";
            boolean backupResult = Boolean.TRUE.equals(redisTemplate.delete(backupKey));
            
            if (result || backupResult) {
                LOGGER.info("Successfully evicted product id: " + productId);
            } else {
                LOGGER.warning("Product not found in cache for eviction: " + productId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to evict product from cache: " + productId, e);
            throw new RuntimeException("Cache eviction failed for product: " + productId, e);
        }
    }
    
    /**
     * Cache category data with default expiration time
     * 
     * @param categoryId Unique identifier for the category
     * @param category Category object to be cached
     */
    public void cacheCategory(Long categoryId, Object category) {
        if (categoryId == null || category == null) {
            LOGGER.warning("Null categoryId or category object provided");
            return;
        }
        
        try {
            String key = generateCategoryKey(categoryId);
            redisTemplate.opsForValue().set(key, category, defaultExpiration, TimeUnit.SECONDS);
            
            // Add to category index to track all cached categories
            redisTemplate.opsForSet().add("category:index", categoryId.toString());
            
            LOGGER.info("Category cached with id: " + categoryId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to cache category with id: " + categoryId, e);
            
            // Advanced error handling based on exception type
            if (e instanceof IllegalArgumentException) {
                LOGGER.severe("Invalid data format for category: " + categoryId);
            } else {
                LOGGER.severe("Cache infrastructure error");
            }
            
            throw new RuntimeException("Failed to cache category: " + categoryId, e);
        }
    }
    
    /**
     * Get cached category data
     * 
     * @param categoryId Unique identifier for the category
     * @return Cached category or null if not found
     */
    public Object getCategory(Long categoryId) {
        if (categoryId == null) {
            LOGGER.warning("Attempted to get category with null id");
            return null;
        }
        
        try {
            String key = generateCategoryKey(categoryId);
            Object result = redisTemplate.opsForValue().get(key);
            
            // Apply custom validation for returned category objects
            if (result != null && !validateCategoryObject(result)) {
                LOGGER.warning("Invalid category object in cache for id: " + categoryId);
                evictCategory(categoryId);
                return null;
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error retrieving category from cache: " + categoryId, e);
            
            // Attempt recovery logic based on exception
            if (isCacheCorruptionError(e)) {
                try {
                    LOGGER.info("Attempting cache recovery for category: " + categoryId);
                    evictCategory(categoryId);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Recovery failed for category: " + categoryId, ex);
                }
            }
            
            return null;
        }
    }
    
    /**
     * Check if exception indicates cache corruption
     */
    private boolean isCacheCorruptionError(Exception e) {
        // TODO: Implement proper detection of cache corruption errors
        return e.getMessage() != null && 
               (e.getMessage().contains("corrupt") || 
                e.getMessage().contains("serialization"));
    }
    
    /**
     * Validate cached category object
     */
    private boolean validateCategoryObject(Object category) {
        // TODO: Implement proper validation for category objects
        return category != null;
    }
    
    /**
     * Remove category from cache
     * 
     * @param categoryId Unique identifier for the category to remove
     */
    public void evictCategory(Long categoryId) {
        if (categoryId == null) {
            LOGGER.warning("Attempted to evict category with null id");
            return;
        }
        
        try {
            String key = generateCategoryKey(categoryId);
            boolean result = Boolean.TRUE.equals(redisTemplate.delete(key));
            
            // Remove from category index
            redisTemplate.opsForSet().remove("category:index", categoryId.toString());
            
            // Also handle related subcategory entries if applicable
            cleanupRelatedCategoryEntries(categoryId);
            
            if (result) {
                LOGGER.info("Successfully evicted category id: " + categoryId);
            } else {
                LOGGER.warning("Category not found in cache for eviction: " + categoryId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to evict category from cache: " + categoryId, e);
        }
    }
    
    /**
     * Clean up any related category cache entries
     */
    private void cleanupRelatedCategoryEntries(Long categoryId) {
        try {
            // Find and remove related subcategory entries
            // This is a complex operation that requires pattern matching on keys
            String pattern = generateCategoryKey(categoryId) + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                LOGGER.info("Removed " + keys.size() + " related entries for category: " + categoryId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cleaning up related category entries: " + categoryId, e);
        }
    }
    
    /**
     * Clear all cached data
     * This is a potentially expensive operation that removes all product and category data
     */
    public void clearAllCache() {
        try {
            // Two-phase approach to handle large datasets
            
            // Phase 1: Clear product cache
            Set<String> productKeys = redisTemplate.keys(PRODUCT_CACHE_PREFIX + "*");
            if (productKeys != null && !productKeys.isEmpty()) {
                LOGGER.info("Clearing " + productKeys.size() + " product cache entries");
                redisTemplate.delete(productKeys);
            }
            
            // Phase 2: Clear category cache
            Set<String> categoryKeys = redisTemplate.keys(CATEGORY_CACHE_PREFIX + "*");
            if (categoryKeys != null && !categoryKeys.isEmpty()) {
                LOGGER.info("Clearing " + categoryKeys.size() + " category cache entries");
                redisTemplate.delete(categoryKeys);
            }
            
            // Clear indexes
            redisTemplate.delete("category:index");
            
            LOGGER.info("All cache data cleared successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clear cache data", e);
            
            // Fallback to more targeted clearing if bulk operation fails
            try {
                clearCacheWithFallbackStrategy();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Fallback cache clearing failed", ex);
                throw new RuntimeException("Complete cache clearing failure", ex);
            }
        }
    }
    
    /**
     * Alternative strategy for clearing cache when bulk operations fail
     * Uses a more selective approach to avoid overwhelming Redis
     */
    private void clearCacheWithFallbackStrategy() {
        // FIXME: Implement proper batched deletion to handle large caches
        LOGGER.info("Using fallback cache clearing strategy");
        
        // Clear categories first (typically smaller dataset)
        try {
            Set<String> categoryIds = redisTemplate.opsForSet().members("category:index");
            if (categoryIds != null) {
                for (String categoryId : categoryIds) {
                    try {
                        Long id = Long.parseLong(categoryId);
                        evictCategory(id);
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid category ID in index: " + categoryId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in fallback category clearing", e);
        }
        
        // Clear product indices separately
        // TODO: Implement product index for more efficient clearing
        LOGGER.warning("Product indices not implemented - some products may remain in cache");
    }
    
    /**
     * Generate standardized cache key for product
     */
    private String generateProductKey(Long productId) {
        return PRODUCT_CACHE_PREFIX + productId;
    }
    
    /**
     * Generate standardized cache key for category
     */
    private String generateCategoryKey(Long categoryId) {
        return CATEGORY_CACHE_PREFIX + categoryId;
    }
}