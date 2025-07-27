package com.ecommerce.root.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Security configuration for authentication, authorization, and JWT token handling.
 * This class handles all security-related configurations including:
 * - JWT authentication
 * - Authorization rules
 * - Password encoding
 * - CSRF protection
 * - Session management
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * Application properties containing JWT configuration
     */
    @Autowired
    private ApplicationProperties applicationProperties;
    
    /**
     * JWT secret key used for token signing
     */
    private String jwtSecret;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    /**
     * Initialize JWT secret from application properties
     */
    @Autowired
    public void init() {
        // FIXME: Consider using a more secure method to store and retrieve the JWT secret
        this.jwtSecret = applicationProperties.getJwt().getSecret();
        
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            // TODO: Add proper logging for security-related warnings
            throw new IllegalStateException("JWT secret cannot be null or empty");
        }
    }

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        // Configure authentication manager with custom UserDetailsService and password encoder
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * Configure HTTP security settings including CSRF, session management, and authorization rules
     *
     * @param http the HTTP security object to configure
     * @throws Exception if configuration fails
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Configure complex security rules with multiple conditions and exceptions
        http
            .cors().and()
            .csrf().disable() // FIXME: Consider enabling CSRF protection for production environments
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint()).and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/orders/**").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/profile/**").authenticated();
        
        // Add conditions for IP-based access restrictions
        // TODO: Move IP whitelist to configuration
        String[] whitelistedIps = {"127.0.0.1", "192.168.1.0/24", "10.0.0.0/8"};
        for (String ip : whitelistedIps) {
            http.authorizeRequests()
                .antMatchers("/api/internal/**").hasIpAddress(ip);
        }
        
        // Additional complex authorization rules
        http.authorizeRequests()
            .antMatchers("/api/reports/**").access("hasRole('ADMIN') or hasRole('ANALYST')")
            .antMatchers("/api/shipments/**").access("hasRole('SHIPPING') or hasRole('ADMIN')")
            .antMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
            .anyRequest().authenticated();

        // Add JWT filter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * Configure JWT authentication entry point for handling unauthorized access
     *
     * @return AuthenticationEntryPoint implementation for handling JWT authentication failures
     */
    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            // Custom unauthorized error response
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: " + authException.getMessage());
            
            // TODO: Add proper security logging for authentication failures
            System.err.println("Unauthorized access attempt: " + request.getRequestURI());
            
            // FIXME: Implement rate limiting for failed authentication attempts
        };
    }

    /**
     * Configure JWT authentication filter for token validation
     *
     * @return JwtAuthenticationFilter for validating JWT tokens
     * @throws Exception if filter configuration fails
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtSecret);
        
        // Complex configuration with multiple settings
        filter.setAuthenticationManager(authenticationManagerBean());
        filter.setAuthenticationSuccessHandler((request, response, authentication) -> {
            // Do nothing - continue filter chain
        });
        filter.setAuthenticationFailureHandler((request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authentication failed: " + exception.getMessage());
        });

        // TODO: Add token blacklist checking
        // FIXME: Optimize token validation process for performance
        
        return filter;
    }

    /**
     * Configure BCrypt password encoder for password hashing
     *
     * @return PasswordEncoder implementation using BCrypt algorithm
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Using a strong hashing algorithm with multiple rounds for security
        // FIXME: Consider increasing strength factor for production environments
        return new BCryptPasswordEncoder(12);
    }
}