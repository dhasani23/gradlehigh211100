package com.ecommerce.root.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter for JWT token authentication and validation.
 * Processes all requests and validates JWT tokens in the Authorization header.
 */
public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final String HEADER_STRING = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private final String jwtSecret;

    /**
     * Constructor for JwtAuthenticationFilter
     * @param jwtSecret Secret key used to validate JWT tokens
     */
    public JwtAuthenticationFilter(String jwtSecret) {
        super(new AntPathRequestMatcher("/api/**"));
        this.jwtSecret = jwtSecret;
        // Allow the filter to continue even if authentication fails
        setAuthenticationFailureHandler((request, response, exception) -> {
            // FIXME: Add proper logging of authentication failures
            System.err.println("JWT authentication failed: " + exception.getMessage());
        });
    }

    /**
     * Attempt to authenticate the request based on the JWT token
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        String token = getJwtFromRequest(request);
        
        if (token == null) {
            // No token provided, skip authentication
            return null;
        }
        
        try {
            // Parse and validate the JWT token
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            // Check if token has expired
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                throw new AuthenticationException("JWT token has expired") {};
            }
            
            // Extract username and roles from the token
            String username = claims.getSubject();
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            
            if (roles == null) {
                roles = Collections.emptyList();
            }
            
            // Create authorities from roles
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
            
            // Create user principal with extracted data
            User principal = new User(username, "", authorities);
            
            // Create authenticated token
            return new UsernamePasswordAuthenticationToken(principal, token, authorities);
            
        } catch (SignatureException e) {
            // Token validation failed
            throw new AuthenticationException("Invalid JWT signature") {};
        } catch (Exception e) {
            // Other JWT processing errors
            throw new AuthenticationException("JWT token processing error: " + e.getMessage()) {};
        }
    }

    /**
     * Continue the filter chain and set the authentication in the context if successful
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        
        SecurityContextHolder.getContext().setAuthentication(authResult);
        chain.doFilter(request, response);
    }
    
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException failed) throws IOException, ServletException {
        // Clear any existing authentication
        SecurityContextHolder.clearContext();
        
        // Continue filter chain to allow public resources access even with invalid token
        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }
    
    /**
     * Extract JWT token from the request's Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_STRING);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            // TODO: Implement additional token format validation
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        
        return null;
    }
}