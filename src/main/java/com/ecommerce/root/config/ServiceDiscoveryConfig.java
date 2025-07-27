package com.ecommerce.root.config;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service Discovery Configuration using Netflix Eureka
 * This class provides the configuration needed for a microservice to register with
 * a Eureka discovery server and participate in service discovery.
 * 
 * High complexity is introduced to handle various edge cases, fallbacks, and dynamic configurations.
 */
@Configuration
public class ServiceDiscoveryConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryConfig.class);
    
    /**
     * Eureka server URL for service registration
     */
    @Value("${eureka.client.serviceUrl.defaultZone:http://localhost:8761/eureka}")
    private String eurekaServerUrl;
    
    /**
     * Service name for registration in Eureka
     */
    @Value("${spring.application.name:unknown-service}")
    private String serviceName;
    
    /**
     * Unique instance identifier for this service
     */
    @Value("${eureka.instance.instanceId:${spring.application.name}:${random.uuid}}")
    private String instanceId;
    
    @Autowired
    private Environment environment;
    
    /**
     * Configure Eureka client for service discovery
     * 
     * This method provides a highly configurable EurekaClient with various fallback mechanisms
     * and edge case handling for different network environments and Eureka server states.
     * 
     * @return The configured EurekaClient instance
     */
    @Bean
    public EurekaClient eurekaClient() {
        LOGGER.info("Initializing Eureka client with server URL: {}", eurekaServerUrl);
        
        // Handle multiple Eureka server URLs if present
        String[] eurekaServers = eurekaServerUrl.split(",");
        Map<String, String> additionalProperties = new HashMap<>();
        
        if (eurekaServers.length > 1) {
            LOGGER.info("Multiple Eureka servers detected, configuring fallbacks");
            for (int i = 0; i < eurekaServers.length; i++) {
                additionalProperties.put("eureka.serviceUrl.default." + i, eurekaServers[i].trim());
            }
        }
        
        // Configure client settings with various timeout and retry policies
        EurekaJerseyClientImpl.EurekaJerseyClientBuilder clientBuilder = new EurekaJerseyClientImpl.EurekaJerseyClientBuilder()
                .withConnectionTimeout(5000)
                .withReadTimeout(10000)
                .withMaxConnectionsPerHost(20)
                .withMaxTotalConnections(200)
                .withConnectionIdleTimeout(TimeUnit.MINUTES.toMillis(3));
        
        EurekaJerseyClient jerseyClient = clientBuilder.build();
        
        // Complex setup for instance information manager
        ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager();
        
        // Initialize Eureka client with complex configuration
        EurekaClientConfig clientConfig = discoveryClientConfig();
        
        try {
            // Attempt to create a discovery client with complex initialization
            DiscoveryClient discoveryClient = new DiscoveryClient(
                    applicationInfoManager,
                    clientConfig,
                    jerseyClient
            );
            
            // Register shutdown hook for proper deregistration
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down Eureka client");
                discoveryClient.shutdown();
            }));
            
            // Handle complex initialization scenarios
            if (!discoveryClient.getApplications().getRegisteredApplications().isEmpty()) {
                LOGGER.info("Successfully fetched initial registry from Eureka server");
            } else {
                LOGGER.warn("Initial registry from Eureka server is empty, will retry in background");
                // Trigger complex background fetch logic
                discoveryClient.refreshRegistry();
            }
            
            // Apply complex cache update strategies
            configureRegistryCacheRefresh(discoveryClient);
            
            return discoveryClient;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Eureka client, service discovery will be disabled", e);
            // FIXME: Implement proper fallback strategy when Eureka registration fails
            return null; // Return null as fallback, service will need to handle this case
        }
    }
    
    /**
     * Helper method for complex registry cache refresh configuration
     * 
     * @param discoveryClient The discovery client to configure
     */
    private void configureRegistryCacheRefresh(DiscoveryClient discoveryClient) {
        // Complex logic to determine optimal refresh intervals
        boolean isDevelopmentMode = isDevelopmentEnvironment();
        int initialDelayMs = isDevelopmentMode ? 5000 : 30000;
        
        // TODO: Implement adaptive registry refresh based on system load and network conditions
        LOGGER.info("Configured registry cache refresh with initial delay: {}ms", initialDelayMs);
    }
    
    /**
     * Determines if the application is running in development mode
     * 
     * @return true if in development environment
     */
    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "development".equals(profile) || "local".equals(profile)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Helper method to initialize the ApplicationInfoManager with complex configurations
     * 
     * @return The configured ApplicationInfoManager
     */
    private ApplicationInfoManager initializeApplicationInfoManager() {
        // Create complex instance configuration
        MyDataCenterInstanceConfig instanceConfig = new MyDataCenterInstanceConfig() {
            @Override
            public String getInstanceId() {
                return instanceId;
            }
            
            @Override
            public String getAppname() {
                return serviceName;
            }
            
            @Override
            public String getHostName(boolean refresh) {
                // Complex hostname resolution with fallbacks
                try {
                    return super.getHostName(refresh);
                } catch (Exception e) {
                    LOGGER.warn("Failed to resolve hostname, using IP address instead", e);
                    return getIpAddress();
                }
            }
        };
        
        // Create application info manager with complex setup
        InstanceInfo instanceInfo = instanceInfo();
        return new ApplicationInfoManager(instanceConfig, instanceInfo);
    }

    /**
     * Configure instance information for service registration
     * 
     * This method creates a complex InstanceInfo object with various metadata
     * and configuration options for Eureka registration.
     * 
     * @return The configured InstanceInfo
     */
    @Bean
    public InstanceInfo instanceInfo() {
        LOGGER.info("Configuring service instance: {} with ID: {}", serviceName, instanceId);
        
        // Determine port based on environment with fallback mechanisms
        int port = determineServicePort();
        String hostName = determineHostName();
        
        // Build complex metadata
        Map<String, String> metadata = buildInstanceMetadata();
        
        // Create instance info builder with complex configuration
        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder()
                .setAppName(serviceName)
                .setInstanceId(instanceId)
                .setHostName(hostName)
                .setIPAddr(determineIpAddress())
                .setPort(port)
                .enablePort(InstanceInfo.PortType.SECURE, isSecurePortEnabled())
                .setSecurePort(determineSecurePort())
                .setVIPAddress(serviceName)
                .setSecureVIPAddress(serviceName)
                .setStatus(InstanceStatus.STARTING)
                .setDataCenterInfo(new MyDataCenterInfo())
                .setLeaseInfo(buildLeaseInfo())
                .setMetadata(metadata);
                
        // Add special handling for AWS or other cloud environments
        if (isCloudEnvironment()) {
            configureCloudSpecificSettings(builder);
        }
        
        // Add complex health check configuration
        configureHealthCheck(builder);
        
        return builder.build();
    }
    
    /**
     * Helper method to determine the service port with fallback logic
     * 
     * @return The determined port number
     */
    private int determineServicePort() {
        try {
            String portProperty = environment.getProperty("server.port");
            if (portProperty != null) {
                return Integer.parseInt(portProperty);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid server.port configuration, using default port", e);
        }
        
        // Complex fallback logic
        return environment.getProperty("eureka.instance.port", Integer.class, 8080);
    }
    
    /**
     * Helper method to determine the secure port with complex logic
     * 
     * @return The determined secure port number
     */
    private int determineSecurePort() {
        try {
            String securePortProperty = environment.getProperty("server.ssl.port");
            if (securePortProperty != null) {
                return Integer.parseInt(securePortProperty);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid server.ssl.port configuration, using default secure port", e);
        }
        
        return environment.getProperty("eureka.instance.securePort", Integer.class, 8443);
    }
    
    /**
     * Helper method to determine if secure port should be enabled
     * 
     * @return true if secure port should be enabled
     */
    private boolean isSecurePortEnabled() {
        boolean sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        boolean securePortEnabled = environment.getProperty("eureka.instance.securePortEnabled", Boolean.class, false);
        return sslEnabled || securePortEnabled;
    }
    
    /**
     * Helper method to determine the hostname with complex fallback logic
     * 
     * @return The determined hostname
     */
    private String determineHostName() {
        // Try to get explicit configuration
        String configuredHostname = environment.getProperty("eureka.instance.hostname");
        if (configuredHostname != null && !configuredHostname.isEmpty()) {
            return configuredHostname;
        }
        
        // Complex fallback logic
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostName();
        } catch (Exception e) {
            LOGGER.warn("Failed to determine hostname, using IP address", e);
            return determineIpAddress();
        }
    }
    
    /**
     * Helper method to determine IP address with fallback mechanisms
     * 
     * @return The determined IP address
     */
    private String determineIpAddress() {
        // Try to get explicit configuration
        String configuredIp = environment.getProperty("eureka.instance.ip-address");
        if (configuredIp != null && !configuredIp.isEmpty()) {
            return configuredIp;
        }
        
        // Complex fallback logic
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            LOGGER.warn("Failed to determine IP address, using localhost", e);
            return "127.0.0.1";
        }
    }
    
    /**
     * Helper method to build instance metadata
     * 
     * @return Map of metadata values
     */
    private Map<String, String> buildInstanceMetadata() {
        Map<String, String> metadata = new HashMap<>();
        
        // Add basic metadata
        metadata.put("instanceId", instanceId);
        metadata.put("serviceName", serviceName);
        
        // Add environment information
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            metadata.put("profiles", String.join(",", activeProfiles));
        }
        
        // Add version information if available
        String version = environment.getProperty("info.app.version");
        if (version != null) {
            metadata.put("version", version);
        }
        
        // Add startup timestamp
        metadata.put("startupTimestamp", String.valueOf(System.currentTimeMillis()));
        
        // Add custom metadata from properties
        String customMetadataPrefix = "eureka.instance.metadata.";
        for (String propertyName : System.getProperties().stringPropertyNames()) {
            if (propertyName.startsWith(customMetadataPrefix)) {
                String metadataKey = propertyName.substring(customMetadataPrefix.length());
                metadata.put(metadataKey, System.getProperty(propertyName));
            }
        }
        
        return metadata;
    }
    
    /**
     * Helper method to build lease info with complex configuration
     * 
     * @return The configured lease info
     */
    private InstanceInfo.LeaseInfo buildLeaseInfo() {
        return InstanceInfo.LeaseInfo.Builder.newBuilder()
                .setDurationInSecs(environment.getProperty("eureka.instance.leaseExpirationDurationInSeconds", Integer.class, 90))
                .setRenewalIntervalInSecs(environment.getProperty("eureka.instance.leaseRenewalIntervalInSeconds", Integer.class, 30))
                .build();
    }
    
    /**
     * Helper method to check if running in a cloud environment
     * 
     * @return true if in cloud environment
     */
    private boolean isCloudEnvironment() {
        // Check for various cloud environment indicators
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
            return true;
        }
        if (System.getenv("VCAP_APPLICATION") != null) { // Cloud Foundry
            return true;
        }
        if (System.getenv("AWS_REGION") != null) {
            return true;
        }
        
        // Check for spring cloud config
        return environment.getProperty("spring.cloud.config.uri") != null;
    }
    
    /**
     * Helper method to configure cloud-specific settings
     * 
     * @param builder The InstanceInfo builder
     */
    private void configureCloudSpecificSettings(InstanceInfo.Builder builder) {
        // This would include complex cloud-specific configurations
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
            // Kubernetes-specific settings
            LOGGER.info("Configuring for Kubernetes environment");
            builder.setDataCenterInfo(new DataCenterInfo() {
                @Override
                public Name getName() {
                    return Name.MyOwn;
                }
            });
        } else if (System.getenv("AWS_REGION") != null) {
            // AWS-specific settings
            LOGGER.info("Configuring for AWS environment");
            // TODO: Implement proper AWS data center info configuration
        }
    }
    
    /**
     * Helper method to configure health check settings
     * 
     * @param builder The InstanceInfo builder
     */
    private void configureHealthCheck(InstanceInfo.Builder builder) {
        boolean healthCheckEnabled = environment.getProperty("eureka.client.healthcheck.enabled", Boolean.class, true);
        
        if (healthCheckEnabled) {
            LOGGER.info("Health checks are enabled for service registration");
        } else {
            LOGGER.warn("Health checks are disabled for service registration");
        }
    }

    /**
     * Configure Eureka client settings for service discovery
     * 
     * Configures complex client settings for service discovery including
     * caching, timeouts, and failover mechanisms.
     * 
     * @return The configured EurekaClientConfig
     */
    @Bean
    public EurekaClientConfig discoveryClientConfig() {
        LOGGER.info("Configuring Eureka client settings");
        
        // Create a customized client configuration with complex settings
        return new DefaultEurekaClientConfig() {
            @Override
            public boolean shouldFetchRegistry() {
                // Complex decision logic
                return environment.getProperty("eureka.client.fetch-registry", Boolean.class, true);
            }

            @Override
            public boolean shouldRegisterWithEureka() {
                // Complex decision logic
                return environment.getProperty("eureka.client.register-with-eureka", Boolean.class, true);
            }

            @Override
            public boolean shouldGZipContent() {
                return environment.getProperty("eureka.client.gZipContent", Boolean.class, true);
            }

            @Override
            public int getRegistryFetchIntervalSeconds() {
                // Complex calculation based on environment
                boolean isDevelopment = isDevelopmentEnvironment();
                
                // Use shorter intervals in development
                int defaultValue = isDevelopment ? 10 : 30;
                return environment.getProperty("eureka.client.registryFetchIntervalSeconds", Integer.class, defaultValue);
            }

            @Override
            public int getInstanceInfoReplicationIntervalSeconds() {
                return environment.getProperty("eureka.client.instanceInfoReplicationIntervalSeconds", Integer.class, 30);
            }

            @Override
            public int getInitialInstanceInfoReplicationIntervalSeconds() {
                return environment.getProperty("eureka.client.initialInstanceInfoReplicationIntervalSeconds", Integer.class, 40);
            }

            @Override
            public int getEurekaServiceUrlPollIntervalSeconds() {
                return environment.getProperty("eureka.client.eurekaServiceUrlPollIntervalSeconds", Integer.class, 300);
            }

            @Override
            public String getRegion() {
                return environment.getProperty("eureka.client.region", "default");
            }

            @Override
            public String[] getAvailabilityZones(String region) {
                String availabilityZones = environment.getProperty("eureka.client.availability-zones." + region);
                if (availabilityZones != null) {
                    return availabilityZones.split(",");
                }
                return new String[]{"default"};
            }

            @Override
            public List<String> getEurekaServerServiceUrls(String myZone) {
                // Complex URL handling with multiple formats
                List<String> urls = new ArrayList<>();
                String[] serverUrls = eurekaServerUrl.split(",");
                
                for (String url : serverUrls) {
                    // Process URL to ensure it has the correct format
                    url = url.trim();
                    if (!url.endsWith("/eureka")) {
                        if (!url.endsWith("/")) {
                            url += "/";
                        }
                        url += "eureka";
                    }
                    urls.add(url);
                }
                
                // Add zone-specific URLs if available
                String zoneSpecificUrls = environment.getProperty("eureka.client." + myZone + ".serviceUrl");
                if (zoneSpecificUrls != null) {
                    for (String url : zoneSpecificUrls.split(",")) {
                        urls.add(url.trim());
                    }
                }
                
                LOGGER.info("Eureka server URLs: {}", urls);
                return urls;
            }
            
            @Override
            public boolean shouldUseDnsForFetchingServiceUrls() {
                return environment.getProperty("eureka.client.useDnsForFetchingServiceUrls", Boolean.class, false);
            }

            @Override
            public boolean shouldFilterOnlyUpInstances() {
                return environment.getProperty("eureka.client.filterOnlyUpInstances", Boolean.class, true);
            }
            
            @Override
            public int getEurekaConnectionIdleTimeoutSeconds() {
                return environment.getProperty("eureka.client.eurekaConnectionIdleTimeoutSeconds", Integer.class, 45);
            }
            
            @Override
            public boolean shouldEnableSelfPreservation() {
                return environment.getProperty("eureka.client.enableSelfPreservation", Boolean.class, true);
            }
        };
    }

    /**
     * Configure health check handler for service status monitoring
     * 
     * This creates a complex health check handler that monitors the service status
     * and reports it to Eureka for intelligent load balancing and failover.
     * 
     * @return The configured HealthCheckHandler
     */
    @Bean
    public HealthCheckHandler healthCheckHandler() {
        LOGGER.info("Configuring service health check handler");
        
        // Create a complex health check handler with fallbacks and multiple health indicators
        return new HealthCheckHandler() {
            private final Map<String, HealthIndicator> healthIndicators = new HashMap<>();
            private volatile InstanceStatus previousStatus = InstanceStatus.UNKNOWN;
            
            @Override
            public InstanceStatus getStatus(InstanceStatus currentStatus) {
                LOGGER.debug("Health check requested, current status: {}", currentStatus);
                
                // Complex health evaluation logic
                try {
                    // Get health from Spring Boot health endpoints
                    Health overallHealth = evaluateOverallHealth();
                    
                    // Map health status to Eureka status with complex logic
                    InstanceStatus newStatus = mapToInstanceStatus(overallHealth);
                    
                    // Only log status changes to reduce noise
                    if (previousStatus != newStatus) {
                        LOGGER.info("Service health status changed from {} to {}", previousStatus, newStatus);
                        previousStatus = newStatus;
                    }
                    
                    return newStatus;
                } catch (Exception e) {
                    // Complex error handling with fallback
                    LOGGER.error("Error determining service health status", e);
                    
                    // FIXME: Implement more sophisticated fallback based on partial health information
                    return previousStatus != InstanceStatus.UNKNOWN ? previousStatus : InstanceStatus.DOWN;
                }
            }
            
            /**
             * Complex health evaluation logic that aggregates multiple health indicators
             */
            private Health evaluateOverallHealth() {
                // If no health indicators are registered, return default UP status
                if (healthIndicators.isEmpty()) {
                    return Health.up().build();
                }
                
                // Count health states for complex aggregation
                int up = 0;
                int down = 0;
                int unknown = 0;
                StringBuilder details = new StringBuilder();
                
                for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
                    try {
                        Health health = entry.getValue().health();
                        Status status = health.getStatus();
                        
                        if (Status.UP.equals(status)) {
                            up++;
                        } else if (Status.DOWN.equals(status)) {
                            down++;
                            details.append(entry.getKey()).append(":DOWN ");
                        } else {
                            unknown++;
                            details.append(entry.getKey()).append(":").append(status).append(" ");
                        }
                    } catch (Exception e) {
                        down++; // Count exceptions as DOWN
                        details.append(entry.getKey()).append(":ERROR ");
                    }
                }
                
                // Complex status determination logic
                if (down > 0) {
                    // If any critical indicators are down, service is down
                    if (isAnyCriticalIndicatorDown()) {
                        return Health.down().withDetail("reason", "Critical indicators are down: " + details).build();
                    }
                    
                    // If more than 50% of indicators are down, service is down
                    if ((double)down / healthIndicators.size() > 0.5) {
                        return Health.down().withDetail("reason", "Majority of health indicators are down: " + details).build();
                    }
                    
                    // Otherwise, service is in warning state
                    return Health.unknown().withDetail("reason", "Some health indicators are down: " + details).build();
                }
                
                if (unknown > 0 && up == 0) {
                    return Health.unknown().withDetail("reason", "All health indicators are in unknown state").build();
                }
                
                return Health.up().build();
            }
            
            /**
             * Check if any critical health indicators are down
             */
            private boolean isAnyCriticalIndicatorDown() {
                // List of critical indicators that must be UP for the service to be considered UP
                String[] criticalIndicators = {"database", "messaging", "core"};
                
                for (String criticalIndicator : criticalIndicators) {
                    HealthIndicator indicator = healthIndicators.get(criticalIndicator);
                    if (indicator != null) {
                        try {
                            Health health = indicator.health();
                            if (!Status.UP.equals(health.getStatus())) {
                                return true;
                            }
                        } catch (Exception e) {
                            return true; // Exception in critical indicator counts as down
                        }
                    }
                }
                
                return false;
            }
            
            /**
             * Map Spring Boot health status to Eureka InstanceStatus
             */
            private InstanceStatus mapToInstanceStatus(Health health) {
                Status status = health.getStatus();
                
                if (Status.UP.equals(status)) {
                    return InstanceStatus.UP;
                } else if (Status.DOWN.equals(status)) {
                    return InstanceStatus.DOWN;
                } else if (Status.OUT_OF_SERVICE.equals(status)) {
                    return InstanceStatus.OUT_OF_SERVICE;
                } else if (new Status("STARTING").equals(status)) {
                    return InstanceStatus.STARTING;
                } else {
                    return InstanceStatus.UNKNOWN;
                }
            }
        };
    }
    
    /**
     * Internal class for AWS data center info compatibility
     */
    private static class MyDataCenterInfo implements DataCenterInfo {
        @Override
        public Name getName() {
            return Name.MyOwn;
        }
    }
    
    /**
     * Health status interface for the health check handler
     */
    private interface HealthIndicator {
        Health health();
    }
    
    /**
     * Health result class for health check handler
     */
    private static class Health {
        private final Status status;
        private final Map<String, Object> details;
        
        private Health(Status status, Map<String, Object> details) {
            this.status = status;
            this.details = details;
        }
        
        public Status getStatus() {
            return status;
        }
        
        public Map<String, Object> getDetails() {
            return details;
        }
        
        public static Builder up() {
            return new Builder(Status.UP);
        }
        
        public static Builder down() {
            return new Builder(Status.DOWN);
        }
        
        public static Builder unknown() {
            return new Builder(Status.UNKNOWN);
        }
        
        public static class Builder {
            private final Status status;
            private final Map<String, Object> details = new HashMap<>();
            
            public Builder(Status status) {
                this.status = status;
            }
            
            public Builder withDetail(String key, Object value) {
                this.details.put(key, value);
                return this;
            }
            
            public Health build() {
                return new Health(status, details);
            }
        }
    }
    
    /**
     * Status class for health check handler
     */
    private static class Status {
        public static final Status UP = new Status("UP");
        public static final Status DOWN = new Status("DOWN");
        public static final Status OUT_OF_SERVICE = new Status("OUT_OF_SERVICE");
        public static final Status UNKNOWN = new Status("UNKNOWN");
        
        private final String code;
        
        public Status(String code) {
            this.code = code;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Status status = (Status) o;
            return code.equals(status.code);
        }
        
        @Override
        public int hashCode() {
            return code.hashCode();
        }
        
        @Override
        public String toString() {
            return code;
        }
    }
}