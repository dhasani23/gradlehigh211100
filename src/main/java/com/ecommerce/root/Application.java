package com.ecommerce.root;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import com.ecommerce.root.config.ApplicationProperties;

/**
 * Main application entry point.
 */
@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}