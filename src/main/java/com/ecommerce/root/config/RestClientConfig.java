package com.ecommerce.root.config;

/**
 * REST client configuration
 */
public class RestClientConfig {

    /**
     * Creates a REST template
     *
     * @param builder The REST template builder
     * @return The REST template
     */
    public Object restTemplate(Object builder) {
        return new Object();
    }

    /**
     * Creates a client HTTP request factory
     *
     * @return The client HTTP request factory
     */
    private Object clientHttpRequestFactory() {
        return new Object();
    }
}