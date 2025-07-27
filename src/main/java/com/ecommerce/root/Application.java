package com.ecommerce.root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main application class providing the entry point for the Spring Boot application.
 * This class initializes the application context, configures startup parameters,
 * and handles graceful shutdown procedures.
 */
@SpringBootApplication
public class Application {

    // Logger instance for application startup logging
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    // Spring application context for accessing beans
    private static ApplicationContext applicationContext;
    
    // Runtime variables
    private static boolean isInitialized = false;
    private static boolean isShutdownHookRegistered = false;
    private static String[] activeProfiles;
    private static Thread shutdownHook;
    private static int shutdownTimeout = 30; // Default timeout in seconds

    /**
     * Main method to start the Spring Boot application
     * Entry point for the application
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting E-commerce Application...");
        
        try {
            // Prepare startup parameters with conditional logic (increasing cyclomatic complexity)
            String[] startupArgs = prepareStartupParameters(args);
            
            // Start Spring Application with prepared arguments
            ConfigurableApplicationContext context = SpringApplication.run(Application.class, startupArgs);
            applicationContext = context;
            
            // Store active profiles
            Environment env = applicationContext.getEnvironment();
            activeProfiles = env.getActiveProfiles();
            
            // Log startup information
            logStartupInformation();
            
            // Configure shutdown hook for graceful application termination
            configureShutdownHook();
            
            logger.info("Application startup complete");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    /**
     * Prepares startup parameters based on input arguments and system properties
     * Contains complex conditional logic to demonstrate high cyclomatic complexity
     *
     * @param originalArgs The original command line arguments
     * @return Modified arguments array
     */
    private static String[] prepareStartupParameters(String[] originalArgs) {
        // Complex conditional logic to prepare startup parameters
        if (originalArgs == null || originalArgs.length == 0) {
            // No args provided, check system properties
            String profile = System.getProperty("spring.profiles.active");
            if (profile == null || profile.isEmpty()) {
                // No profile set, use default based on other conditions
                if (Boolean.getBoolean("development.mode")) {
                    return new String[]{"--spring.profiles.active=dev"};
                } else if (Boolean.getBoolean("production.mode")) {
                    return new String[]{"--spring.profiles.active=prod"};
                } else if (System.getProperty("test.mode") != null) {
                    // For test mode, check additional conditions
                    String testType = System.getProperty("test.type");
                    if ("integration".equals(testType)) {
                        return new String[]{"--spring.profiles.active=integration"};
                    } else if ("performance".equals(testType)) {
                        return new String[]{"--spring.profiles.active=perf"};
                    } else {
                        return new String[]{"--spring.profiles.active=test"};
                    }
                } else {
                    // Last resort default
                    return new String[]{"--spring.profiles.active=default"};
                }
            } else {
                // Profile is set via system property, use it
                return new String[]{"--spring.profiles.active=" + profile};
            }
        } else {
            // Args provided, check if they contain profile information
            boolean hasProfileInfo = false;
            for (String arg : originalArgs) {
                if (arg.contains("spring.profiles.active")) {
                    hasProfileInfo = true;
                    break;
                }
            }
            
            // If no profile info in args, add default profile based on conditions
            if (!hasProfileInfo) {
                String[] newArgs = Arrays.copyOf(originalArgs, originalArgs.length + 1);
                
                // Complex conditions to determine profile
                if (System.getProperty("ENV") != null) {
                    String env = System.getProperty("ENV");
                    if ("DEV".equalsIgnoreCase(env)) {
                        newArgs[newArgs.length - 1] = "--spring.profiles.active=dev";
                    } else if ("PROD".equalsIgnoreCase(env)) {
                        newArgs[newArgs.length - 1] = "--spring.profiles.active=prod";
                    } else if ("STAGING".equalsIgnoreCase(env)) {
                        newArgs[newArgs.length - 1] = "--spring.profiles.active=staging";
                    } else if ("TEST".equalsIgnoreCase(env)) {
                        newArgs[newArgs.length - 1] = "--spring.profiles.active=test";
                    } else {
                        newArgs[newArgs.length - 1] = "--spring.profiles.active=default";
                    }
                } else {
                    newArgs[newArgs.length - 1] = "--spring.profiles.active=default";
                }
                return newArgs;
            }
            
            return originalArgs;
        }
    }

    /**
     * Logs detailed startup information including active profiles,
     * system properties, and environment variables
     */
    private static void logStartupInformation() {
        if (applicationContext != null) {
            Environment env = applicationContext.getEnvironment();
            String[] profiles = env.getActiveProfiles();
            
            StringBuilder sb = new StringBuilder();
            sb.append("\n------------------------------------------------------------\n");
            sb.append("Application is running with the following configuration:\n");
            sb.append("------------------------------------------------------------\n");
            
            if (profiles.length > 0) {
                sb.append("Active profiles: ").append(String.join(", ", profiles)).append("\n");
            } else {
                sb.append("No active profiles - using default\n");
            }
            
            sb.append("Application startup parameters:\n");
            Map<String, Object> systemProps = env.getSystemProperties();
            for (Map.Entry<String, Object> entry : systemProps.entrySet()) {
                if (entry.getKey().startsWith("spring.") || entry.getKey().startsWith("server.")) {
                    sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            
            sb.append("------------------------------------------------------------\n");
            logger.info(sb.toString());
        }
    }

    /**
     * CommandLineRunner implementation for post-startup initialization
     * @return CommandLineRunner bean
     */
    @Bean
    public CommandLineRunner run() {
        return (String... args) -> {
            logger.info("Executing post-startup initialization...");
            
            try {
                // Initialize application data
                initializeApplicationData();
                isInitialized = true;
                logger.info("Application initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize application data", e);
                throw e; // Re-throw to fail startup
            }
        };
    }

    /**
     * Initialize application data and perform startup checks
     * Contains complex conditional logic to increase cyclomatic complexity
     */
    private void initializeApplicationData() {
        logger.info("Initializing application data...");
        
        // Get environment for configuration
        Environment env = applicationContext.getEnvironment();
        String[] profiles = env.getActiveProfiles();
        
        // Simulate complex startup initialization with multiple conditions
        if (profiles.length > 0) {
            boolean isDev = Arrays.stream(profiles).anyMatch(p -> p.equals("dev"));
            boolean isProd = Arrays.stream(profiles).anyMatch(p -> p.equals("prod"));
            boolean isTest = Arrays.stream(profiles).anyMatch(p -> p.equals("test"));
            
            // Development profile initialization
            if (isDev) {
                logger.info("Initializing development environment");
                
                // Initialize with development-specific data
                String devDataSource = env.getProperty("spring.datasource.url");
                if (devDataSource != null && !devDataSource.contains("localhost")) {
                    logger.warn("Development profile with non-localhost database. Verify configuration.");
                    
                    // Additional verification based on other properties
                    if (env.getProperty("spring.jpa.hibernate.ddl-auto", "none").equals("create-drop")) {
                        logger.warn("Using create-drop with non-localhost database may cause data loss!");
                    }
                }
                
                // Check for development-specific features
                if (Boolean.parseBoolean(env.getProperty("feature.mock.enabled", "false"))) {
                    logger.info("Mock data services enabled for development");
                    // Initialize mock data would happen here
                }
            } 
            // Production profile initialization
            else if (isProd) {
                logger.info("Initializing production environment");
                
                // Perform production-specific checks
                String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", "none");
                if (!ddlAuto.equals("none") && !ddlAuto.equals("validate")) {
                    logger.error("CRITICAL: Production environment with DDL set to: " + ddlAuto);
                    logger.error("This may cause data loss. Application will not start.");
                    throw new RuntimeException("Unsafe DDL mode for production: " + ddlAuto);
                }
                
                // Verify essential production configurations
                String[] requiredProps = {
                    "spring.datasource.url",
                    "server.ssl.key-store",
                    "server.ssl.enabled"
                };
                
                for (String prop : requiredProps) {
                    String value = env.getProperty(prop);
                    if (StringUtils.isEmpty(value)) {
                        logger.error("Missing required production property: " + prop);
                        throw new RuntimeException("Missing required production configuration: " + prop);
                    }
                }
                
                // Additional production checks based on complex conditions
                if (Boolean.parseBoolean(env.getProperty("metrics.enabled", "false"))) {
                    String metricsEndpoint = env.getProperty("management.endpoints.web.exposure.include", "");
                    if (metricsEndpoint.contains("*")) {
                        logger.warn("Security warning: All actuator endpoints exposed in production!");
                    }
                }
            }
            // Test profile initialization
            else if (isTest) {
                logger.info("Initializing test environment");
                // No need to perform extensive checks in test environment
            }
            // Other profiles
            else {
                logger.info("Initializing with profile: " + Arrays.toString(profiles));
                
                // Generic initialization for other profiles
                if (env.getProperty("initialization.level", "basic").equals("comprehensive")) {
                    logger.info("Performing comprehensive initialization...");
                    // Would perform detailed initialization here
                } else {
                    logger.info("Performing basic initialization...");
                    // Would perform basic initialization here
                }
            }
        } else {
            logger.warn("No active profiles detected, using default initialization");
            // Default initialization logic
        }
        
        // Performance checks
        String heapSize = Runtime.getRuntime().maxMemory() / (1024 * 1024) + "M";
        logger.info("JVM maximum heap size: {}", heapSize);
        
        if (Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024) { // Less than 512MB
            logger.warn("Low memory configuration detected. Performance may be impacted.");
        }
        
        // Simulating complex initialization tasks
        logger.info("Application data initialization complete");
    }

    /**
     * Configure graceful shutdown hook for the application
     */
    private static void configureShutdownHook() {
        if (!isShutdownHookRegistered) {
            Environment env = applicationContext.getEnvironment();
            
            // Get shutdown timeout from configuration or use default
            try {
                String timeoutProperty = env.getProperty("application.shutdown.timeout");
                if (timeoutProperty != null) {
                    shutdownTimeout = Integer.parseInt(timeoutProperty);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid shutdown timeout configuration, using default: {} seconds", shutdownTimeout);
            }
            
            shutdownHook = new Thread(() -> {
                logger.info("Shutdown signal received. Beginning graceful shutdown (timeout: {} seconds)...", shutdownTimeout);
                
                try {
                    // Perform pre-shutdown operations
                    logger.info("Executing pre-shutdown tasks...");
                    
                    // Check if we need special handling based on active profiles
                    if (activeProfiles != null) {
                        for (String profile : activeProfiles) {
                            if ("prod".equals(profile)) {
                                logger.info("Production shutdown procedure initiated");
                                // Additional production shutdown procedures would go here
                            }
                        }
                    }
                    
                    // Close application context if it exists
                    if (applicationContext != null && applicationContext instanceof ConfigurableApplicationContext) {
                        logger.info("Closing Spring application context...");
                        ((ConfigurableApplicationContext) applicationContext).close();
                    }
                    
                    logger.info("Application shutdown complete");
                } catch (Exception e) {
                    logger.error("Error during application shutdown", e);
                }
            });
            
            shutdownHook.setName("Application-Shutdown-Hook");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            isShutdownHookRegistered = true;
            logger.info("Shutdown hook configured (timeout: {} seconds)", shutdownTimeout);
        }
    }
    
    /**
     * PreDestroy method executed before bean destruction
     * Performs cleanup operations
     */
    @PreDestroy
    public void onDestroy() {
        logger.info("Application context is being destroyed, performing cleanup...");
        
        // Simulate resource cleanup with timing
        try {
            logger.info("Closing application resources...");
            TimeUnit.MILLISECONDS.sleep(500); // Simulating resource cleanup time
            logger.info("Resources closed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Cleanup interrupted", e);
        }
    }
}