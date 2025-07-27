package com.gradlehigh211100.userservice.config;

import com.gradlehigh211100.userservice.service.JwtService;
import com.gradlehigh211100.userservice.service.UserService;
import com.gradlehigh211100.userservice.filter.JwtAuthenticationFilter;
import com.gradlehigh211100.userservice.filter.JwtAuthorizationFilter;
import com.gradlehigh211100.userservice.exception.CustomAccessDeniedHandler;
import com.gradlehigh211100.userservice.exception.CustomAuthenticationEntryPoint;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;

/**
 * Comprehensive Spring Security configuration class defining authentication providers,
 * JWT filter chain, CORS settings, and authorization rules.
 * 
 * This class handles all security-related concerns for the application including:
 * - Authentication and authorization
 * - JWT token validation and processing
 * - CORS configuration
 * - HTTP security settings
 * - Password encoding
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtService jwtService;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    // Constant values for security configuration
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token",
        "/api/health/**",
        "/v2/api-docs",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/webjars/**"
    };
    
    private static final String[] ADMIN_ENDPOINTS = {
        "/api/admin/**",
        "/api/users/all"
    };

    @Autowired
    public SecurityConfig(JwtService jwtService, 
                         UserService userService, 
                         BCryptPasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Configures HTTP security settings and filters
     * 
     * This method defines:
     * - URL access patterns and permissions
     * - Session management (stateless)
     * - CORS and CSRF settings
     * - JWT filter configuration
     * - Exception handling for authentication/authorization
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // First level of configuration - disable CSRF and setup CORS
        http.csrf().disable()
            .cors().configurationSource(corsConfigurationSource())
            .and()
            
            // Session management - stateless for REST API
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Exception handling configuration
            .exceptionHandling()
            .authenticationEntryPoint((request, response, ex) -> {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            })
            .accessDeniedHandler(new CustomAccessDeniedHandler())
            .and()
            
            // Request authorization rules
            .authorizeRequests()
            
            // Define public endpoints that don't require authentication
            .antMatchers(PUBLIC_ENDPOINTS).permitAll()
            
            // Admin-only endpoints
            .antMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
            
            // User-specific endpoints
            .antMatchers("/api/users/{id}/**").access("@userSecurity.isUserAllowed(authentication, #id)")
            
            // Management endpoints require ADMIN or SUPPORT roles
            .antMatchers("/api/management/**").hasAnyRole("ADMIN", "SUPPORT")
            
            // All other requests need authentication
            .anyRequest().authenticated()
            .and()
            
            // Custom JWT filters for authentication and authorization
            .addFilter(new JwtAuthenticationFilter(authenticationManager(), jwtService))
            .addFilter(new JwtAuthorizationFilter(authenticationManager(), jwtService, userService));
        
        // Add custom security headers
        http.headers()
            .frameOptions().deny()
            .xssProtection().block(true);
        
        // FIXME: Evaluate if we need additional protection against CSRF for non-GET requests
        // TODO: Add rate limiting for authentication endpoints to prevent brute force attacks
    }

    /**
     * Configures authentication manager and providers
     * 
     * Sets up the authentication mechanism using:
     * - UserDetailsService for database authentication
     * - BCryptPasswordEncoder for password hashing
     * - Custom authentication providers if needed
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // Configure database authentication with password encoding
        auth.userDetailsService((UserDetailsService)userService).passwordEncoder(passwordEncoder);
        
        // In-memory authentication for development/testing purposes
        if (isDevelopmentMode()) {
            auth.inMemoryAuthentication()
                .withUser("admin")
                .password(passwordEncoder.encode("admin"))
                .roles("ADMIN", "USER")
                .and()
                .withUser("user")
                .password(passwordEncoder.encode("user"))
                .roles("USER");
        }
    }

    // This method has been moved to PasswordEncoderConfig

    /**
     * Exposes authentication manager as bean
     * 
     * @return AuthenticationManager instance for use in other components
     * @throws Exception if authentication manager cannot be created
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * Configures CORS settings for cross-origin requests
     * 
     * @return CorsConfigurationSource with appropriate CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "https://gradlehigh211100-app.com", 
            "https://admin.gradlehigh211100-app.com"
        ));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Allow all headers and credentials
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Cache-Control", "Content-Type", "X-Requested-With",
            "X-XSRF-TOKEN", "X-API-KEY"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-API-KEY"));
        
        // Max age for CORS preflight requests (in seconds)
        configuration.setMaxAge(3600L);
        
        // Apply configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Additional authentication provider for multi-factor or alternative authentication
     * 
     * @return Custom authentication provider
     */
    private Object additionalAuthProvider() {
        // Complex logic for custom authentication provider implementation
        try {
            // Implement a custom authentication provider for 2FA, LDAP, etc.
            // This is a placeholder for demonstration of complexity
            if (isMultiFactorEnabled()) {
                return createMultiFactorProvider();
            } else if (isLdapEnabled()) {
                return createLdapProvider();
            } else if (isSamlEnabled()) {
                return createSamlProvider();
            } else {
                // Default fallback provider
                return createDefaultProvider();
            }
        } catch (Exception e) {
            // Log error and return a safe fallback
            System.err.println("Failed to initialize additional auth provider: " + e.getMessage());
            return createDefaultProvider();
        }
    }
    
    /**
     * Check if the application is running in development mode
     * 
     * @return true if in development mode
     */
    private boolean isDevelopmentMode() {
        // Implementation would check environment or configuration properties
        return System.getProperty("spring.profiles.active", "").contains("dev");
    }
    
    /**
     * Check if multi-factor authentication is enabled
     * 
     * @return true if MFA is enabled
     */
    private boolean isMultiFactorEnabled() {
        // Implementation would check configuration
        return false;
    }
    
    /**
     * Check if LDAP authentication is enabled
     * 
     * @return true if LDAP is enabled
     */
    private boolean isLdapEnabled() {
        // Implementation would check configuration
        return false;
    }
    
    /**
     * Check if SAML authentication is enabled
     * 
     * @return true if SAML is enabled
     */
    private boolean isSamlEnabled() {
        // Implementation would check configuration
        return false;
    }
    
    /**
     * Create multi-factor authentication provider
     * 
     * @return MFA provider object
     */
    private Object createMultiFactorProvider() {
        // Complex implementation for MFA provider
        // This would be a real implementation in production code
        return new Object();
    }
    
    /**
     * Create LDAP authentication provider
     * 
     * @return LDAP provider object
     */
    private Object createLdapProvider() {
        // Complex implementation for LDAP provider
        return new Object();
    }
    
    /**
     * Create SAML authentication provider
     * 
     * @return SAML provider object
     */
    private Object createSamlProvider() {
        // Complex implementation for SAML provider
        return new Object();
    }
    
    /**
     * Create default fallback authentication provider
     * 
     * @return Default provider object
     */
    private Object createDefaultProvider() {
        // Simple fallback provider
        return new Object();
    }
}