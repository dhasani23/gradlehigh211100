package com.ecommerce.root.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application properties for configuring various aspects of the application.
 * Properties are loaded from application.yml or application.properties with the prefix 'application'.
 */
@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {
    
    private final Cache cache = new Cache();
    
    // Add other property groups as needed
    
    public Cache getCache() {
        return cache;
    }
    
    /**
     * Cache specific configuration properties
     */
    public static class Cache {
        private String redisHost = "localhost";
        private int redisPort = 6379;
        private String redisPassword;
        private int redisDatabase = 0;
        private long defaultTtl = 3600; // Default 1 hour in seconds
        private String prefix = "ecommerce";
        private boolean useJsonSerialization = true;
        private boolean disableCachingNullValues = true;
        private boolean enableTransactions = false;
        private int poolMaxTotal = 8;
        private int poolMaxIdle = 8;
        private int poolMinIdle = 0;
        private long connectionTimeout = 2000;
        private long readTimeout = 2000;
        private List<String> additionalCaches;

        public String getRedisHost() {
            return redisHost;
        }

        public void setRedisHost(String redisHost) {
            this.redisHost = redisHost;
        }

        public int getRedisPort() {
            return redisPort;
        }

        public void setRedisPort(int redisPort) {
            this.redisPort = redisPort;
        }

        public String getRedisPassword() {
            return redisPassword;
        }

        public void setRedisPassword(String redisPassword) {
            this.redisPassword = redisPassword;
        }

        public int getRedisDatabase() {
            return redisDatabase;
        }

        public void setRedisDatabase(int redisDatabase) {
            this.redisDatabase = redisDatabase;
        }

        public long getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(long defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public boolean isUseJsonSerialization() {
            return useJsonSerialization;
        }

        public void setUseJsonSerialization(boolean useJsonSerialization) {
            this.useJsonSerialization = useJsonSerialization;
        }

        public boolean isDisableCachingNullValues() {
            return disableCachingNullValues;
        }

        public void setDisableCachingNullValues(boolean disableCachingNullValues) {
            this.disableCachingNullValues = disableCachingNullValues;
        }

        public boolean isEnableTransactions() {
            return enableTransactions;
        }

        public void setEnableTransactions(boolean enableTransactions) {
            this.enableTransactions = enableTransactions;
        }

        public int getPoolMaxTotal() {
            return poolMaxTotal;
        }

        public void setPoolMaxTotal(int poolMaxTotal) {
            this.poolMaxTotal = poolMaxTotal;
        }

        public int getPoolMaxIdle() {
            return poolMaxIdle;
        }

        public void setPoolMaxIdle(int poolMaxIdle) {
            this.poolMaxIdle = poolMaxIdle;
        }

        public int getPoolMinIdle() {
            return poolMinIdle;
        }

        public void setPoolMinIdle(int poolMinIdle) {
            this.poolMinIdle = poolMinIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
        }

        public List<String> getAdditionalCaches() {
            return additionalCaches;
        }

        public void setAdditionalCaches(List<String> additionalCaches) {
            this.additionalCaches = additionalCaches;
        }
    }
}