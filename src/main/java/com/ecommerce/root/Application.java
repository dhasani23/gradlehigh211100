package com.ecommerce.root;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

/**
 * Main application class for the eCommerce system.
 */
@SpringBootApplication
@EnableRetry
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    /**
     * Create RestTemplate bean for HTTP communication.
     * 
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}