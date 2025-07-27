package com.gradlehigh211100.productcatalog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple cache service using in-memory map for storing objects
 * Used as a temporary replacement for Redis-based caching
 */
@Service
public class CacheService {
    
    private static final Logger LOGGER = Logger.getLogger(CacheService.class.getName());
    
    private final Map<String, Object> cache = new HashMap<>();
    private final Long defaultExpiration;
    
    /**
     * Constructs a new CacheService with required dependencies
     * 
     * @param defaultExpiration Default cache expiration time in seconds
     */
    @Autowired
    public CacheService(
            @Value("${cache.default-expiration:3600}") Long defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
    }
    
    /**
     * Store an object in the cache
     * 
     * @param key Cache key
     * @param value Object to cache
     */
    public <T> void put(String key, T value) {
        try {
            cache.put(key, value);
            LOGGER.info("Object cached with key: " + key);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to cache object with key: " + key, e);
        }
    }
    
    /**
     * Retrieve an object from the cache
     * 
     * @param key Cache key
     * @param clazz Expected class type
     * @return The cached object or null if not found
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = cache.get(key);
            if (value != null && clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error retrieving object from cache: " + key, e);
            return null;
        }
    }
    
    /**
     * Remove an object from the cache
     * 
     * @param key Cache key
     */
    public void remove(String key) {
        try {
            cache.remove(key);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing object from cache: " + key, e);
        }
    }
    
    /**
     * Clear all cached items
     */
    public void clearAll() {
        try {
            cache.clear();
            LOGGER.info("Cache cleared");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clear cache", e);
        }
    }
}