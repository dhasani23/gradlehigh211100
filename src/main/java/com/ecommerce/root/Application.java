package com.ecommerce.root;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application class for the e-commerce application
 */
@SpringBootApplication
@EnableRetry
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}