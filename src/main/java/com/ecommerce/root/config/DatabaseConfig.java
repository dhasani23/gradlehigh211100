package com.ecommerce.root.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Database configuration class responsible for setting up datasources, connection pooling,
 * transaction management, and JPA entity manager factory configuration.
 * 
 * This configuration handles multiple datasource setup and optimizes database connection
 * management using HikariCP for high performance connection pooling.
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Autowired
    private ApplicationProperties applicationProperties;
    
    // Maximum database connection pool size
    private int maxPoolSize = 50;
    
    // Minimum database connection pool size
    private int minPoolSize = 10;
    
    /**
     * Configures the primary datasource with connection pooling optimized for high throughput
     * applications.
     * 
     * @return configured DataSource instance
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        // Get database properties from application configuration
        String url = applicationProperties.getDatabase().getUrl();
        String username = applicationProperties.getDatabase().getUsername();
        String password = applicationProperties.getDatabase().getPassword();
        
        // Configure HikariCP connection pool
        HikariConfig config = configureConnectionPool();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        
        // FIXME: Add SSL configuration for production environments
        
        HikariDataSource hikariDataSource = new HikariDataSource(config);
        
        // Wrap with lazy connection to avoid unnecessary database connections
        return new LazyConnectionDataSourceProxy(hikariDataSource);
    }
    
    /**
     * Configures the transaction manager for database operations.
     * 
     * @param dataSource the configured DataSource
     * @return configured PlatformTransactionManager instance
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        
        // Set entity manager factory
        transactionManager.setEntityManagerFactory(
                entityManagerFactory(dataSource).getObject()
        );
        
        // Configure transaction isolation level and timeout
        transactionManager.setDefaultTimeout(30); // 30 seconds timeout
        
        // Complex transaction management for high throughput requirements
        if (applicationProperties.getDatabase().isReadOnly()) {
            // Configure for read-only operations
            transactionManager.setNestedTransactionAllowed(false);
            transactionManager.setGlobalRollbackOnParticipationFailure(true);
        } else {
            // Configure for read-write operations with nested transaction support
            transactionManager.setNestedTransactionAllowed(true);
            transactionManager.setGlobalRollbackOnParticipationFailure(false);
            
            // TODO: Implement custom transaction synchronization for distributed transactions
        }
        
        return transactionManager;
    }
    
    /**
     * Configures JPA entity manager factory with advanced Hibernate properties.
     * 
     * @param dataSource the configured DataSource
     * @return configured LocalContainerEntityManagerFactoryBean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.ecommerce.root.domain");
        em.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(applicationProperties.getDatabase().isGenerateDdl());
        vendorAdapter.setShowSql(applicationProperties.getDatabase().isShowSql());
        vendorAdapter.setDatabasePlatform(applicationProperties.getDatabase().getDialect());
        em.setJpaVendorAdapter(vendorAdapter);
        
        // Set additional JPA properties for advanced configuration
        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.physical_naming_strategy", 
                "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        jpaProperties.put("hibernate.implicit_naming_strategy", 
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        
        // Performance optimization properties
        jpaProperties.put("hibernate.jdbc.batch_size", 50);
        jpaProperties.put("hibernate.order_inserts", true);
        jpaProperties.put("hibernate.order_updates", true);
        jpaProperties.put("hibernate.jdbc.batch_versioned_data", true);
        
        // Second level cache configuration
        if (applicationProperties.getDatabase().isCacheEnabled()) {
            jpaProperties.put("hibernate.cache.use_second_level_cache", true);
            jpaProperties.put("hibernate.cache.region.factory_class", 
                    "org.hibernate.cache.ehcache.EhCacheRegionFactory");
            jpaProperties.put("hibernate.cache.use_query_cache", true);
        } else {
            jpaProperties.put("hibernate.cache.use_second_level_cache", false);
            jpaProperties.put("hibernate.cache.use_query_cache", false);
        }
        
        // Additional query optimization
        jpaProperties.put("hibernate.query.fail_on_pagination_over_collection_fetch", true);
        jpaProperties.put("hibernate.query.in_clause_parameter_padding", true);
        
        em.setJpaPropertyMap(jpaProperties);
        
        return em;
    }
    
    /**
     * Configures HikariCP connection pool settings with optimized parameters
     * for high throughput applications.
     * 
     * @return configured HikariConfig
     */
    @Bean
    public HikariConfig configureConnectionPool() {
        HikariConfig config = new HikariConfig();
        
        // Apply pool size constraints
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minPoolSize);
        
        // Connection timeout settings
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30)); // 30 seconds
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));      // 10 minutes
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));      // 30 minutes
        
        // Connection validation and testing
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(5));  // 5 seconds
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(60)); // 1 minute
        
        // Add health check query based on database type
        if (applicationProperties.getDatabase().getType().equalsIgnoreCase("mysql")) {
            config.setConnectionTestQuery("SELECT 1");
        } else if (applicationProperties.getDatabase().getType().equalsIgnoreCase("postgresql")) {
            config.setConnectionTestQuery("SELECT 1");
        } else if (applicationProperties.getDatabase().getType().equalsIgnoreCase("oracle")) {
            config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        } else {
            // Default validation query
            config.setConnectionTestQuery("SELECT 1");
        }
        
        // Add connection initialization SQL if needed
        if (applicationProperties.getDatabase().isSetTimezone()) {
            config.setConnectionInitSql("SET time_zone = '+00:00'");
        }
        
        // FIXME: Connection pool metrics collection should be added for production monitoring
        
        // Advanced connection pool settings for high throughput
        config.setAutoCommit(false);  // Manage transactions explicitly
        
        // Configure connection pool isolation level based on application needs
        int isolationLevel = determineIsolationLevel();
        if (isolationLevel > 0) {
            config.setTransactionIsolation(String.valueOf(isolationLevel));
        }
        
        // Register pool listeners for debugging if enabled
        if (applicationProperties.getDatabase().isDebugEnabled()) {
            // TODO: Implement custom pooled connection listener for debugging
        }
        
        return config;
    }
    
    /**
     * Determines the appropriate transaction isolation level based on application configuration.
     * This method implements complex conditional logic to select the optimal isolation level.
     * 
     * @return transaction isolation level as an integer constant
     */
    private int determineIsolationLevel() {
        String configuredLevel = applicationProperties.getDatabase().getIsolationLevel();
        
        // Complex logic for determining isolation level based on multiple factors
        if (applicationProperties.getDatabase().isReadOnly()) {
            // For read-only operations, use READ COMMITTED
            return java.sql.Connection.TRANSACTION_READ_COMMITTED;
        }
        
        // Dynamically determine isolation level based on configured value
        if (configuredLevel != null) {
            switch (configuredLevel.toUpperCase()) {
                case "READ_UNCOMMITTED":
                    return java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
                case "READ_COMMITTED":
                    return java.sql.Connection.TRANSACTION_READ_COMMITTED;
                case "REPEATABLE_READ":
                    return java.sql.Connection.TRANSACTION_REPEATABLE_READ;
                case "SERIALIZABLE":
                    return java.sql.Connection.TRANSACTION_SERIALIZABLE;
                default:
                    // Fallback to database default
                    return -1;
            }
        }
        
        // Load testing mode configuration
        if (applicationProperties.getDatabase().isLoadTestingMode()) {
            // Use READ COMMITTED for load testing to maximize throughput
            return java.sql.Connection.TRANSACTION_READ_COMMITTED;
        }
        
        // High concurrency applications with optimistic locking
        if (applicationProperties.getDatabase().isOptimisticLocking()) {
            return java.sql.Connection.TRANSACTION_READ_COMMITTED;
        }
        
        // Default to READ COMMITTED if no specific configuration is provided
        return java.sql.Connection.TRANSACTION_READ_COMMITTED;
    }
    
    /**
     * Creates a secondary datasource for specific read-only operations.
     * This method demonstrates handling multiple datasources in complex applications.
     * 
     * @return configured secondary DataSource
     */
    @Bean(name = "readOnlyDataSource")
    public DataSource readOnlyDataSource() {
        // Check if read replica configuration is available
        if (!applicationProperties.getDatabase().isReplicaEnabled()) {
            // Fall back to primary if replica not configured
            return primaryDataSource();
        }
        
        // Configure read-only datasource with custom pool settings
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(applicationProperties.getDatabase().getReplicaUrl());
        config.setUsername(applicationProperties.getDatabase().getReplicaUsername());
        config.setPassword(applicationProperties.getDatabase().getReplicaPassword());
        
        // Optimize for read operations
        config.setReadOnly(true);
        config.setMaximumPoolSize(maxPoolSize * 2);  // Double connection pool for read operations
        config.setMinimumIdle(minPoolSize);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));  // Faster timeout for reads
        
        return new HikariDataSource(config);
    }
    
    /**
     * Configures advanced JPA properties for special use cases.
     * This method demonstrates complex configuration logic.
     * 
     * @return Properties object with configured Hibernate properties
     */
    private Properties getAdditionalHibernateProperties() {
        Properties props = new Properties();
        
        // Configure SQL statement logging
        if (applicationProperties.getDatabase().isShowSql()) {
            props.put("hibernate.show_sql", "true");
            props.put("hibernate.format_sql", "true");
            props.put("hibernate.use_sql_comments", "true");
        }
        
        // Configure statistics collection for performance monitoring
        if (applicationProperties.getDatabase().isStatisticsEnabled()) {
            props.put("hibernate.generate_statistics", "true");
        }
        
        // Configure connection handling
        props.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
        
        // Configure schema generation
        if (applicationProperties.getDatabase().isGenerateDdl()) {
            String ddlAuto = applicationProperties.getDatabase().getDdlAuto();
            if (ddlAuto == null) {
                ddlAuto = "update";  // Default to update
            }
            props.put("hibernate.hbm2ddl.auto", ddlAuto);
        }
        
        // Complex logic for determining fetch size based on database type
        String dbType = applicationProperties.getDatabase().getType();
        if ("mysql".equalsIgnoreCase(dbType)) {
            props.put("hibernate.jdbc.fetch_size", "100");
        } else if ("postgresql".equalsIgnoreCase(dbType)) {
            props.put("hibernate.jdbc.fetch_size", "50");
        } else if ("oracle".equalsIgnoreCase(dbType)) {
            props.put("hibernate.jdbc.fetch_size", "20");
        }
        
        return props;
    }
}