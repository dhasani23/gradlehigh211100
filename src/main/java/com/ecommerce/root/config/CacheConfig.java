package com.ecommerce.root.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.JedisPoolConfig;

/**
 * Cache configuration class for managing both Redis and in-memory cache.
 * Implements complex caching strategies and failover mechanisms.
 * 
 * This configuration provides:
 * - Redis cache with customizable TTLs
 * - In-memory cache fallback
 * - Dynamic cache selection based on application properties
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Autowired
    private ApplicationProperties applicationProperties;
    
    private final long defaultTtl;
    
    // Cache names for various business domains
    private static final String PRODUCT_CACHE = "productCache";
    private static final String USER_CACHE = "userCache";
    private static final String ORDER_CACHE = "orderCache";
    private static final String INVENTORY_CACHE = "inventoryCache";
    private static final String PROMOTION_CACHE = "promotionCache";
    
    // Different TTL configurations for different cache types
    private static final Map<String, Duration> CACHE_TTL_MAP = new HashMap<>();
    
    static {
        CACHE_TTL_MAP.put(PRODUCT_CACHE, Duration.ofHours(24));
        CACHE_TTL_MAP.put(USER_CACHE, Duration.ofHours(2));
        CACHE_TTL_MAP.put(ORDER_CACHE, Duration.ofMinutes(30));
        CACHE_TTL_MAP.put(INVENTORY_CACHE, Duration.ofMinutes(5));
        CACHE_TTL_MAP.put(PROMOTION_CACHE, Duration.ofHours(12));
    }
    
    /**
     * Constructor to initialize default TTL from application properties
     */
    public CacheConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.defaultTtl = applicationProperties.getCache().getDefaultTtl();
        
        // Additional validation to prevent negative TTL values
        if (this.defaultTtl < 0) {
            throw new IllegalArgumentException("Default TTL cannot be negative");
        }
    }

    /**
     * Configures the primary cache manager that combines Redis and in-memory caching
     * with failover capabilities and cache-specific TTL settings.
     *
     * @return CacheManager configured with multiple cache backends
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        // For high complexity, we'll implement a composite cache manager with fallback strategy
        List<CacheManager> cacheManagers = new ArrayList<>();
        
        try {
            // Primary cache manager (Redis-based)
            RedisCacheManager redisCacheManager = buildRedisCacheManager();
            cacheManagers.add(redisCacheManager);
            
            // Fallback cache manager (in-memory)
            SimpleCacheManager inMemoryCacheManager = new SimpleCacheManager();
            
            // Configure in-memory caches with the same names for fallback
            List<ConcurrentMapCache> caches = Arrays.asList(
                new ConcurrentMapCache(PRODUCT_CACHE),
                new ConcurrentMapCache(USER_CACHE),
                new ConcurrentMapCache(ORDER_CACHE),
                new ConcurrentMapCache(INVENTORY_CACHE),
                new ConcurrentMapCache(PROMOTION_CACHE)
            );
            inMemoryCacheManager.setCaches(caches);
            inMemoryCacheManager.initializeCaches();
            
            cacheManagers.add(inMemoryCacheManager);
        } catch (Exception e) {
            // FIXME: Improve error handling with retry mechanisms and circuit breaking
            // Currently falls back to in-memory cache only on Redis connection failure
            SimpleCacheManager fallbackManager = new SimpleCacheManager();
            fallbackManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("fallbackCache")
            ));
            fallbackManager.initializeCaches();
            cacheManagers.add(fallbackManager);
        }
        
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();
        compositeCacheManager.setCacheManagers(cacheManagers);
        compositeCacheManager.setFallbackToNoOpCache(true);
        
        return compositeCacheManager;
    }

    /**
     * Builds a Redis cache manager with cache-specific configurations
     *
     * @return Redis cache manager with different TTL settings for each cache
     */
    private RedisCacheManager buildRedisCacheManager() {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
            .builder(connectionFactory())
            .cacheDefaults(cacheConfiguration(Duration.ofSeconds(defaultTtl)));

        // Apply cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Configure each cache with its specific TTL from the map
        CACHE_TTL_MAP.forEach((cacheName, ttl) -> {
            // Apply additional cache-specific settings like prefixes
            RedisCacheConfiguration config = cacheConfiguration(ttl)
                .prefixCacheNameWith(applicationProperties.getCache().getPrefix() + ":")
                .disableCachingNullValues();
                
            cacheConfigurations.put(cacheName, config);
        });
        
        // Add dynamic cache configurations based on application properties
        if (applicationProperties.getCache().getAdditionalCaches() != null) {
            for (String additionalCache : applicationProperties.getCache().getAdditionalCaches()) {
                // Default to the application's default TTL for additional caches
                cacheConfigurations.put(additionalCache, 
                    cacheConfiguration(Duration.ofSeconds(defaultTtl)));
            }
        }
        
        builder.withInitialCacheConfigurations(cacheConfigurations);
        
        // Additional configuration for statistic collection and transaction awareness
        return builder
            .transactionAware()
            .enableStatistics()
            .build();
    }

    /**
     * Configures the Redis template for complex cache operations
     *
     * @return Configured RedisTemplate with serializers
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        
        // Key serializer
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        
        // Value serializers - configurable based on application properties
        if (applicationProperties.getCache().isUseJsonSerialization()) {
            GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
            template.setValueSerializer(valueSerializer);
            template.setHashValueSerializer(valueSerializer);
        } else {
            JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();
            template.setValueSerializer(valueSerializer);
            template.setHashValueSerializer(valueSerializer);
        }
        
        // Enable transaction support if configured
        if (applicationProperties.getCache().isEnableTransactions()) {
            template.setEnableTransactionSupport(true);
        }
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configures the Redis connection factory with pool settings
     * and connection timeouts for robust operation
     *
     * @return Configured JedisConnectionFactory
     */
    @Bean
    public JedisConnectionFactory connectionFactory() {
        // Configure Redis connection parameters
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(applicationProperties.getCache().getRedisHost());
        redisConfig.setPort(applicationProperties.getCache().getRedisPort());
        
        // Set password if provided
        if (applicationProperties.getCache().getRedisPassword() != null &&
            !applicationProperties.getCache().getRedisPassword().isEmpty()) {
            redisConfig.setPassword(applicationProperties.getCache().getRedisPassword());
        }
        
        // Configure Redis database if specified
        if (applicationProperties.getCache().getRedisDatabase() >= 0) {
            redisConfig.setDatabase(applicationProperties.getCache().getRedisDatabase());
        }
        
        // Configure connection pooling
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(applicationProperties.getCache().getPoolMaxTotal());
        poolConfig.setMaxIdle(applicationProperties.getCache().getPoolMaxIdle());
        poolConfig.setMinIdle(applicationProperties.getCache().getPoolMinIdle());
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        // Configure timeout and retry settings
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
            .connectTimeout(Duration.ofMillis(applicationProperties.getCache().getConnectionTimeout()))
            .readTimeout(Duration.ofMillis(applicationProperties.getCache().getReadTimeout()))
            .usePooling()
            .poolConfig(poolConfig)
            .build();
            
        // Create and return the connection factory
        return new JedisConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * Creates a Redis cache configuration with custom TTL settings and serializers
     *
     * @param ttl The time-to-live duration for cache entries
     * @return Configured RedisCacheConfiguration
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration(Duration ttl) {
        RedisSerializationContext.SerializationPair<String> keySerializer = RedisSerializationContext
            .SerializationPair.fromSerializer(new StringRedisSerializer());
            
        // Value serializer selection based on application properties
        RedisSerializationContext.SerializationPair<Object> valueSerializer;
        
        if (applicationProperties.getCache().isUseJsonSerialization()) {
            valueSerializer = RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer());
        } else {
            valueSerializer = RedisSerializationContext.SerializationPair
                .fromSerializer(new JdkSerializationRedisSerializer());
        }
        
        // Apply complex cache configuration options
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(keySerializer)
            .serializeValuesWith(valueSerializer);
            
        // Apply additional configuration based on application properties
        if (applicationProperties.getCache().isDisableCachingNullValues()) {
            config = config.disableCachingNullValues();
        }
        
        // Apply cache key prefix if configured
        if (applicationProperties.getCache().getPrefix() != null && 
            !applicationProperties.getCache().getPrefix().isEmpty()) {
            config = config.prefixCacheNameWith(applicationProperties.getCache().getPrefix() + ":");
        }
        
        return config;
    }
    
    /**
     * Custom key generator for more intelligent cache key generation
     * based on method signatures, parameter types and values.
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        // TODO: Implement a more sophisticated key generator that handles complex objects better
        return new SimpleKeyGenerator();
    }
    
    /**
     * Custom cache error handler to provide graceful degradation
     * when cache operations fail.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        // FIXME: Implement proper error logging and metrics collection for cache failures
        return new SimpleCacheErrorHandler();
    }
    
    /**
     * Cache resolver for dynamic cache resolution based on method context
     */
    @Override
    public CacheResolver cacheResolver() {
        // TODO: Implement dynamic cache resolution based on method parameters or business logic
        return context -> Collections.singletonList(cacheManager().getCache("default"));
    }
}