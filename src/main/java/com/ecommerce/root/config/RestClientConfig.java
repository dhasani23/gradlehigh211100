package com.ecommerce.root.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration class for REST clients
 */
@Configuration
public class RestClientConfig {

    /**
     * Create a RestTemplate bean with custom configuration
     * 
     * @param builder RestTemplateBuilder for constructing RestTemplate
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }
    
    /**
     * Custom request factory with connection pooling
     * 
     * @return ClientHttpRequestFactory implementation
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        factory.setConnectionRequestTimeout(5000);
        return factory;
    }
}