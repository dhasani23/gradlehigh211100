package com.ecommerce.root.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties wrapper for various configuration properties.
 * Captures values from application.yml or application.properties file.
 */
@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {
    
    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Cache cache = new Cache();
    
    public Jwt getJwt() {
        return jwt;
    }
    
    public Cors getCors() {
        return cors;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    /**
     * JWT token configuration properties
     */
    public static class Jwt {
        private String secret;
        private long tokenValidityInSeconds = 86400; // 24 hours by default
        private long tokenValidityInSecondsForRememberMe = 2592000; // 30 days
        
        public String getSecret() {
            return secret;
        }
        
        public void setSecret(String secret) {
            this.secret = secret;
        }
        
        public long getTokenValidityInSeconds() {
            return tokenValidityInSeconds;
        }
        
        public void setTokenValidityInSeconds(long tokenValidityInSeconds) {
            this.tokenValidityInSeconds = tokenValidityInSeconds;
        }
        
        public long getTokenValidityInSecondsForRememberMe() {
            return tokenValidityInSecondsForRememberMe;
        }
        
        public void setTokenValidityInSecondsForRememberMe(long tokenValidityInSecondsForRememberMe) {
            this.tokenValidityInSecondsForRememberMe = tokenValidityInSecondsForRememberMe;
        }
    }
    
    /**
     * CORS configuration properties
     */
    public static class Cors {
        private String[] allowedOrigins = {"*"};
        private String[] allowedMethods = {"*"};
        private String[] allowedHeaders = {"*"};
        private boolean allowCredentials = true;
        private long maxAge = 1800;
        
        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }
        
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
        
        public String[] getAllowedMethods() {
            return allowedMethods;
        }
        
        public void setAllowedMethods(String[] allowedMethods) {
            this.allowedMethods = allowedMethods;
        }
        
        public String[] getAllowedHeaders() {
            return allowedHeaders;
        }
        
        public void setAllowedHeaders(String[] allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }
        
        public boolean isAllowCredentials() {
            return allowCredentials;
        }
        
        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
        
        public long getMaxAge() {
            return maxAge;
        }
        
        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
    
    /**
     * Cache configuration properties
     */
    public static class Cache {
        private int timeToLiveSeconds = 3600;
        private int maxEntries = 1000;
        
        public int getTimeToLiveSeconds() {
            return timeToLiveSeconds;
        }
        
        public void setTimeToLiveSeconds(int timeToLiveSeconds) {
            this.timeToLiveSeconds = timeToLiveSeconds;
        }
        
        public int getMaxEntries() {
            return maxEntries;
        }
        
        public void setMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }
    }
}