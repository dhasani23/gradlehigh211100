package com.ecommerce.root.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Swagger configuration
 */
public class SwaggerConfig {

    /**
     * Creates the API documentation
     *
     * @return The Docket
     */
    public Object api() {
        return new Object();
    }

    /**
     * Creates API info
     *
     * @return The API info
     */
    private Object apiInfo() {
        return new Object();
    }

    /**
     * Creates security contexts
     *
     * @return The security contexts
     */
    private List<Object> securityContexts() {
        return new ArrayList<>();
    }

    /**
     * Creates default auth
     *
     * @return The security references
     */
    private List<Object> defaultAuth() {
        return new ArrayList<>();
    }

    /**
     * Creates admin auth
     *
     * @return The security references
     */
    private List<Object> adminAuth() {
        return new ArrayList<>();
    }

    /**
     * Creates security schemes
     *
     * @return The security schemes
     */
    private List<Object> securitySchemes() {
        return new ArrayList<>();
    }

    /**
     * Creates API key
     *
     * @return The API key
     */
    private Object apiKey() {
        return new Object();
    }

    /**
     * Adds resource handlers
     *
     * @param registry The resource handler registry
     */
    protected void addResourceHandlers(Object registry) {
        // Do nothing for now
    }
}