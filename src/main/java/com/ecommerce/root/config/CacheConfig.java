package com.ecommerce.root.config;

/**
 * Cache configuration
 */
public class CacheConfig {

    /**
     * Creates a cache manager
     *
     * @return The cache manager
     */
    public Object cacheManager() {
        return new Object();
    }

    /**
     * Builds a Redis cache manager
     *
     * @return The Redis cache manager
     */
    private Object buildRedisCacheManager() {
        return new Object();
    }
}