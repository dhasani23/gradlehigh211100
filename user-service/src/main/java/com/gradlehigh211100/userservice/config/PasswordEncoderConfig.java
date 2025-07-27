package com.gradlehigh211100.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuration class for password encoding.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Creates password encoder bean
     * 
     * @return BCryptPasswordEncoder configured with appropriate strength
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // Higher strength means more secure but slower hashing
        return new BCryptPasswordEncoder(12);
    }
}