package com.ecommerce.root.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * SwaggerConfig
 * 
 * Configuration class for Swagger API documentation.
 * Provides a web interface for API testing and documentation.
 * 
 * This class configures Swagger with high complexity to support:
 * - Multiple security schemes
 * - Path filtering
 * - Custom documentation settings
 * - Environment-based configuration
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig extends WebMvcConfigurationSupport {

    // API Documentation metadata
    private final String apiTitle = "E-Commerce API Documentation";
    private final String apiDescription = "RESTful API documentation for the E-Commerce system";
    private final String apiVersion = "1.0.0";
    
    // Security scheme names
    private static final String SECURITY_REFERENCE = "Authorization";
    private static final String AUTHORIZATION_DESCRIPTION = "Full API Access";
    private static final String AUTHORIZATION_SCOPE = "global";
    
    // Environment variables and paths
    private List<String> securedPaths;
    private List<String> unsecuredPaths;
    private List<String> adminOnlyPaths;
    
    /**
     * Configure Swagger Docket for API documentation generation
     * 
     * This method creates a complex configuration for Swagger documentation,
     * including security contexts, API info, and path selection logic.
     * 
     * @return Docket object with API configuration
     */
    @Bean
    public Docket api() {
        // Initialize path filters for different security levels
        initializePathFilters();
        
        // Create complex predicate for path filtering
        Predicate<String> pathSelector = createPathSelectionPredicate();
        
        // Build and return the Swagger Docket with all configurations
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.ecommerce.root"))
                .paths(PathSelectors.regex(pathSelector.toString()))
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts())
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false)
                .enableUrlTemplating(true)
                .forCodeGeneration(true)
                .ignoredParameterTypes(getIgnoredParameterTypes())
                .directModelSubstitute(java.time.LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(java.time.ZonedDateTime.class, java.util.Date.class)
                .directModelSubstitute(java.time.LocalDateTime.class, java.util.Date.class);
    }

    /**
     * Initialize path filtering lists for different authorization levels
     * 
     * FIXME: These paths should be externalized to configuration properties
     */
    private void initializePathFilters() {
        // Define paths that require authentication
        securedPaths = Arrays.asList(
            "/api/v1/orders.*",
            "/api/v1/users.*",
            "/api/v1/payments.*",
            "/api/v1/shipping.*"
        );
        
        // Define paths that are publicly accessible
        unsecuredPaths = Arrays.asList(
            "/api/v1/products/public.*",
            "/api/v1/categories.*",
            "/api/v1/auth.*",
            "/health",
            "/info"
        );
        
        // Define paths that require admin privileges
        adminOnlyPaths = Arrays.asList(
            "/api/v1/admin.*",
            "/api/v1/analytics.*",
            "/api/v1/settings.*"
        );
    }

    /**
     * Create a complex predicate for path selection
     * 
     * @return Predicate<String> for filtering API paths
     */
    private Predicate<String> createPathSelectionPredicate() {
        // TODO: Implement more sophisticated path selection based on environment
        // For now returning a simple predicate that includes all paths
        return path -> true;
    }

    /**
     * Configure API information including title, description, and version
     * 
     * @return ApiInfo object with documentation metadata
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion)
                .termsOfServiceUrl("https://www.example.com/terms")
                .contact(new Contact("API Support", "https://www.example.com/support", "api@example.com"))
                .license("API License")
                .licenseUrl("https://www.example.com/license")
                .extensions(Collections.emptyList())
                .build();
    }

    /**
     * Configure security context for Swagger UI authentication
     * 
     * This method sets up security contexts with different authentication requirements
     * based on the API path patterns.
     * 
     * @return List of SecurityContext objects
     */
    private List<SecurityContext> securityContexts() {
        List<SecurityContext> contexts = new ArrayList<>();
        
        // Add security context for standard secured endpoints
        contexts.add(SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex(String.join("|", securedPaths)))
                .build());
        
        // Add security context with higher privileges for admin endpoints
        contexts.add(SecurityContext.builder()
                .securityReferences(adminAuth())
                .forPaths(PathSelectors.regex(String.join("|", adminOnlyPaths)))
                .build());
        
        return contexts;
    }

    /**
     * Configure default authorization scopes
     * 
     * @return List of SecurityReference objects for standard authentication
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope(
                AUTHORIZATION_SCOPE, AUTHORIZATION_DESCRIPTION);
        return Collections.singletonList(new SecurityReference(
                SECURITY_REFERENCE, new AuthorizationScope[] { authorizationScope }));
    }

    /**
     * Configure admin-level authorization scopes
     * 
     * @return List of SecurityReference objects for admin authentication
     */
    private List<SecurityReference> adminAuth() {
        AuthorizationScope[] authScopes = {
            new AuthorizationScope(AUTHORIZATION_SCOPE, AUTHORIZATION_DESCRIPTION),
            new AuthorizationScope("admin", "Administrative access")
        };
        return Collections.singletonList(new SecurityReference(SECURITY_REFERENCE, authScopes));
    }

    /**
     * Configure security schemes for Swagger
     * 
     * @return List of SecurityScheme objects
     */
    private List<SecurityScheme> securitySchemes() {
        return Collections.singletonList(apiKey());
    }

    /**
     * Configure API key security scheme for Swagger
     * 
     * @return ApiKey security scheme configuration
     */
    private ApiKey apiKey() {
        return new ApiKey(SECURITY_REFERENCE, "Authorization", "header");
    }

    /**
     * Get list of parameter types to ignore in documentation
     * 
     * @return Array of Class objects to ignore
     */
    private Class<?>[] getIgnoredParameterTypes() {
        return new Class<?>[] {
            javax.servlet.ServletRequest.class,
            javax.servlet.ServletResponse.class,
            javax.servlet.http.HttpServletRequest.class,
            javax.servlet.http.HttpServletResponse.class,
            java.lang.String.class,
            java.lang.Void.class
        };
    }

    /**
     * Configure resource handlers for Swagger UI
     * 
     * @param registry ResourceHandlerRegistry for registering static resource handlers
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Register Swagger UI resources
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // Ensure default resource handling is maintained
        super.addResourceHandlers(registry);
    }
}