package com.gradlehigh211100.userservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradlehigh211100.userservice.model.UserEntity;
import com.gradlehigh211100.userservice.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter for authentication using JWT tokens.
 * Handles login requests and generates JWT tokens for authenticated users.
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        // Set the URL that triggers this filter
        setFilterProcessesUrl("/api/auth/login");
    }

    /**
     * Attempts to authenticate the user with provided credentials.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);

            return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword(),
                    new ArrayList<>())
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse authentication request", e);
        }
    }

    /**
     * Called when authentication is successful.
     * Generates JWT token and sends it in the response.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) throws IOException, ServletException {
        org.springframework.security.core.userdetails.User user = 
            (org.springframework.security.core.userdetails.User) authResult.getPrincipal();
        
        // We need to convert Spring Security User to our UserEntity
        // This is a simplified example - in real code, we would look up the user from the database
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(user.getUsername());
        // Set other properties as needed
        
        // Generate JWT tokens
        String accessToken = jwtService.generateToken(userEntity);
        String refreshToken = jwtService.generateRefreshToken(userEntity);
        
        // Prepare response
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        
        // Write response
        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(tokens));
    }
    
    /**
     * Simple class to parse login request JSON.
     */
    private static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}