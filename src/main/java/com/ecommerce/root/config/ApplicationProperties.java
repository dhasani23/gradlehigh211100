package com.ecommerce.root.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application properties configuration holder for externalized configuration management.
 * This class loads and manages application configuration properties from external sources
 * such as property files, environment variables, and system properties.
 *
 * Configuration properties are hierarchically loaded with the following precedence:
 * 1. System properties
 * 2. Environment variables
 * 3. Configuration files (application.properties, application-{profile}.properties)
 *
 * @author Ecommerce Platform Team
 * @version 1.0
 */
public class ApplicationProperties {
    
    private static final Logger LOGGER = Logger.getLogger(ApplicationProperties.class.getName());
    
    // Default values for configuration properties
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final String DEFAULT_DB_URL = "jdbc:h2:mem:testdb";
    private static final String DEFAULT_DB_USERNAME = "sa";
    private static final String DEFAULT_DB_PASSWORD = "";
    private static final String DEFAULT_JWT_SECRET = "default_insecure_jwt_secret_key_do_not_use_in_production";
    private static final boolean DEFAULT_CACHE_ENABLED = false;
    private static final boolean DEFAULT_MONITORING_ENABLED = false;
    
    // Configuration property keys
    private static final String SERVER_PORT_KEY = "server.port";
    private static final String DB_URL_KEY = "db.url";
    private static final String DB_USERNAME_KEY = "db.username";
    private static final String DB_PASSWORD_KEY = "db.password";
    private static final String JWT_SECRET_KEY = "jwt.secret";
    private static final String CACHE_ENABLED_KEY = "cache.enabled";
    private static final String MONITORING_ENABLED_KEY = "monitoring.enabled";
    
    // Actual property values
    private int serverPort;
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private String jwtSecret;
    private boolean cacheEnabled;
    private boolean monitoringEnabled;
    
    // Configuration sources
    private Properties configProperties;
    private Map<String, String> envProperties;
    private Properties systemProperties;
    
    /**
     * Constructs an ApplicationProperties instance with default configuration.
     * Loads properties from available sources based on precedence rules.
     */
    public ApplicationProperties() {
        this("application.properties");
    }
    
    /**
     * Constructs an ApplicationProperties instance with configuration from the specified property file.
     * 
     * @param configFilePath path to the properties file
     */
    public ApplicationProperties(String configFilePath) {
        initialize();
        loadProperties(configFilePath);
        resolveProperties();
    }
    
    /**
     * Initializes configuration sources and sets default values.
     */
    private void initialize() {
        configProperties = new Properties();
        envProperties = new HashMap<>();
        systemProperties = System.getProperties();
        
        // Set default values
        serverPort = DEFAULT_SERVER_PORT;
        dbUrl = DEFAULT_DB_URL;
        dbUsername = DEFAULT_DB_USERNAME;
        dbPassword = DEFAULT_DB_PASSWORD;
        jwtSecret = DEFAULT_JWT_SECRET;
        cacheEnabled = DEFAULT_CACHE_ENABLED;
        monitoringEnabled = DEFAULT_MONITORING_ENABLED;
        
        // Load environment variables
        loadEnvironmentVariables();
    }
    
    /**
     * Loads properties from the specified configuration file.
     * 
     * @param configFilePath path to the properties file
     */
    private void loadProperties(String configFilePath) {
        try (FileInputStream fileInputStream = new FileInputStream(configFilePath)) {
            configProperties.load(fileInputStream);
            LOGGER.info("Successfully loaded configuration from: " + configFilePath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load properties from: " + configFilePath, e);
            // Fallback to classpath resource
            try {
                configProperties.load(getClass().getClassLoader().getResourceAsStream(configFilePath));
                LOGGER.info("Successfully loaded configuration from classpath: " + configFilePath);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load properties from classpath: " + configFilePath, ex);
            }
        }
    }
    
    /**
     * Loads environment variables for configuration.
     */
    private void loadEnvironmentVariables() {
        try {
            Map<String, String> env = System.getenv();
            // Convert environment variable names to property keys
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String key = entry.getKey().toLowerCase().replace('_', '.');
                envProperties.put(key, entry.getValue());
            }
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Security manager prevented accessing environment variables", e);
        }
    }
    
    /**
     * Resolves configuration properties from all sources based on precedence rules.
     */
    private void resolveProperties() {
        // Server port
        serverPort = resolveIntProperty(SERVER_PORT_KEY, DEFAULT_SERVER_PORT);
        
        // Database configuration
        dbUrl = resolveStringProperty(DB_URL_KEY, DEFAULT_DB_URL);
        dbUsername = resolveStringProperty(DB_USERNAME_KEY, DEFAULT_DB_USERNAME);
        dbPassword = resolveStringProperty(DB_PASSWORD_KEY, DEFAULT_DB_PASSWORD);
        
        // Security configuration
        jwtSecret = resolveStringProperty(JWT_SECRET_KEY, DEFAULT_JWT_SECRET);
        
        // Feature flags
        cacheEnabled = resolveBooleanProperty(CACHE_ENABLED_KEY, DEFAULT_CACHE_ENABLED);
        monitoringEnabled = resolveBooleanProperty(MONITORING_ENABLED_KEY, DEFAULT_MONITORING_ENABLED);
    }
    
    /**
     * Resolves a String property value from available sources.
     * 
     * @param key property key
     * @param defaultValue default value if property not found
     * @return resolved property value
     */
    private String resolveStringProperty(String key, String defaultValue) {
        // Check system properties (highest precedence)
        String value = systemProperties.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Check environment variables
        value = envProperties.get(key);
        if (value != null) {
            return value;
        }
        
        // Check config file properties
        value = configProperties.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Return default value
        return defaultValue;
    }
    
    /**
     * Resolves an Integer property value from available sources.
     * 
     * @param key property key
     * @param defaultValue default value if property not found or invalid
     * @return resolved property value
     */
    private int resolveIntProperty(String key, int defaultValue) {
        String value = resolveStringProperty(key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid integer value for " + key + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    /**
     * Resolves a Boolean property value from available sources.
     * 
     * @param key property key
     * @param defaultValue default value if property not found
     * @return resolved property value
     */
    private boolean resolveBooleanProperty(String key, boolean defaultValue) {
        String value = resolveStringProperty(key, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * Reloads configuration from all sources.
     * 
     * @param configFilePath path to the properties file
     */
    public void reload(String configFilePath) {
        initialize();
        loadProperties(configFilePath);
        resolveProperties();
        LOGGER.info("Configuration reloaded successfully");
    }
    
    /**
     * Get server port configuration.
     *
     * @return the server port
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Set server port configuration.
     *
     * @param serverPort the server port to set
     */
    public void setServerPort(int serverPort) {
        // Validate port range
        if (serverPort < 0 || serverPort > 65535) {
            LOGGER.warning("Invalid port number: " + serverPort + ". Port must be between 0 and 65535");
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        
        this.serverPort = serverPort;
        // Update system property to maintain consistency
        System.setProperty(SERVER_PORT_KEY, String.valueOf(serverPort));
    }

    /**
     * Get database URL.
     *
     * @return the database URL
     */
    public String getDbUrl() {
        return dbUrl;
    }

    /**
     * Set database URL.
     *
     * @param dbUrl the database URL to set
     */
    public void setDbUrl(String dbUrl) {
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            LOGGER.warning("Database URL cannot be null or empty");
            throw new IllegalArgumentException("Database URL cannot be null or empty");
        }
        
        // FIXME: Add proper URL validation logic to ensure this is a valid JDBC URL
        
        this.dbUrl = dbUrl;
        // Update system property to maintain consistency
        System.setProperty(DB_URL_KEY, dbUrl);
    }
    
    /**
     * Get database username.
     *
     * @return the database username
     */
    public String getDbUsername() {
        return dbUsername;
    }
    
    /**
     * Set database username.
     *
     * @param dbUsername the database username to set
     */
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
        // Update system property to maintain consistency
        if (dbUsername != null) {
            System.setProperty(DB_USERNAME_KEY, dbUsername);
        } else {
            System.clearProperty(DB_USERNAME_KEY);
        }
    }
    
    /**
     * Get database password.
     *
     * @return the database password
     */
    public String getDbPassword() {
        return dbPassword;
    }
    
    /**
     * Set database password.
     *
     * @param dbPassword the database password to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
        // Update system property to maintain consistency
        if (dbPassword != null) {
            System.setProperty(DB_PASSWORD_KEY, dbPassword);
        } else {
            System.clearProperty(DB_PASSWORD_KEY);
        }
    }
    
    /**
     * Get JWT token secret key.
     *
     * @return the JWT secret
     */
    public String getJwtSecret() {
        return jwtSecret;
    }
    
    /**
     * Set JWT token secret key.
     *
     * @param jwtSecret the JWT secret to set
     */
    public void setJwtSecret(String jwtSecret) {
        // TODO: Implement secret strength validation
        if (jwtSecret != null && jwtSecret.length() < 32) {
            LOGGER.warning("JWT secret is too short. It should be at least 32 characters for security");
        }
        
        this.jwtSecret = jwtSecret;
        // Do not store JWT secret in system properties for security reasons
    }
    
    /**
     * Check if caching is enabled.
     *
     * @return true if caching is enabled, false otherwise
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    /**
     * Set whether caching is enabled.
     *
     * @param cacheEnabled true to enable caching, false to disable
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        // Update system property to maintain consistency
        System.setProperty(CACHE_ENABLED_KEY, String.valueOf(cacheEnabled));
    }
    
    /**
     * Check if monitoring is enabled.
     *
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
    
    /**
     * Set whether monitoring is enabled.
     *
     * @param monitoringEnabled true to enable monitoring, false to disable
     */
    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
        // Update system property to maintain consistency
        System.setProperty(MONITORING_ENABLED_KEY, String.valueOf(monitoringEnabled));
    }
    
    /**
     * Returns a string representation of this configuration, with sensitive values masked.
     *
     * @return string representation of the configuration
     */
    @Override
    public String toString() {
        return "ApplicationProperties{" +
                "serverPort=" + serverPort +
                ", dbUrl='" + dbUrl + '\'' +
                ", dbUsername='" + dbUsername + '\'' +
                ", dbPassword='" + (dbPassword != null ? "********" : null) + '\'' +
                ", jwtSecret='" + (jwtSecret != null ? "********" : null) + '\'' +
                ", cacheEnabled=" + cacheEnabled +
                ", monitoringEnabled=" + monitoringEnabled +
                '}';
    }
}