package com.gradlehigh211100.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gradlehigh211100.userservice.model.UserEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for JWT token creation, validation, parsing, and security management
 * with configurable expiration and signing.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationTime;

    /**
     * Generates JWT token for authenticated user.
     * 
     * @param user The authenticated user entity
     * @return JWT token string
     */
    public String generateToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        
        // Add essential user information to claims
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        
        // Add user roles to claims
        try {
            claims.put("roles", user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
        } catch (NullPointerException e) {
            // FIXME: Handle case where user has no roles more gracefully
            logger.warn("User {} has no roles assigned", user.getUsername());
            claims.put("roles", new ArrayList<String>());
        }
        
        // Add any additional custom claims
        claims.put("email", user.getEmail());
        claims.put("created", new Date());
        
        return createToken(claims, user.getUsername(), expirationTime);
    }

    /**
     * Generates refresh token with extended expiration.
     * 
     * @param user The authenticated user entity
     * @return Refresh token string
     */
    public String generateRefreshToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", user.getId());
        
        return createToken(claims, user.getUsername(), refreshExpirationTime);
    }

    /**
     * Internal method to create JWT token with custom claims.
     * 
     * @param claims Custom claims to add to token
     * @param subject Subject (typically username)
     * @param expiration Expiration time in milliseconds
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        try {
            return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
        } catch (Exception e) {
            // FIXME: Improve error handling for token generation failures
            logger.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    /**
     * Validates token signature and expiration.
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Empty token provided for validation");
            return false;
        }
        
        try {
            // Verify signature and parse the token
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            
            // Check if token is expired
            if (isTokenExpired(token)) {
                logger.info("Token validation failed: expired token");
                return false;
            }
            
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            logger.error("Unexpected error during token validation", e);
        }
        
        return false;
    }

    /**
     * Extracts username from JWT token claims.
     * 
     * @param token JWT token
     * @return Username from token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extracts expiration date from JWT token.
     * 
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from token using a resolver function.
     * 
     * @param <T> Type of the claim to extract
     * @param token JWT token
     * @param claimsResolver Function to extract a specific claim
     * @return The claim value
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        if (token == null) {
            // TODO: Consider using Optional<T> return type instead of null
            return null;
        }
        
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claimsResolver.apply(claims);
        } catch (ExpiredJwtException e) {
            // Special handling for expired tokens - we might still want to extract claims
            logger.warn("Extracting claim from expired token", e);
            return claimsResolver.apply(e.getClaims());
        } catch (Exception e) {
            logger.error("Error extracting claim from token", e);
            return null;
        }
    }

    /**
     * Extracts all claims from a JWT token.
     * 
     * @param token JWT token
     * @return Claims object containing all claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Checks if token has expired.
     * 
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            // Already expired by JWT's internal checks
            return true;
        } catch (Exception e) {
            // If we can't determine expiration, consider it expired for safety
            logger.error("Error checking token expiration", e);
            return true;
        }
    }

    /**
     * Extracts user roles from JWT token claims.
     * 
     * @param token JWT token
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object rolesObj = claims.get("roles");
            
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            } else {
                logger.warn("Roles claim is not a List: {}", rolesObj);
                return new ArrayList<>();
            }
        } catch (ExpiredJwtException e) {
            // For expired tokens, we might still want to extract roles
            Object rolesObj = e.getClaims().get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }
        } catch (Exception e) {
            logger.error("Error extracting roles from token", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Updates the JWT secret key (only for testing purposes).
     * 
     * @param newSecretKey The new secret key
     */
    public void setSecretKeyForTesting(String newSecretKey) {
        // FIXME: Remove this method in production or secure it properly
        if (System.getProperty("spring.profiles.active").equals("test")) {
            this.secretKey = newSecretKey;
        } else {
            logger.error("Attempted to change JWT secret key in non-test environment");
            throw new SecurityException("Cannot change secret key in production environment");
        }
    }
    
    /**
     * Validates a refresh token specifically.
     * 
     * @param refreshToken The refresh token to validate
     * @return true if valid refresh token, false otherwise
     */
    public boolean validateRefreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            return false;
        }
        
        try {
            Claims claims = getAllClaimsFromToken(refreshToken);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            logger.error("Error validating refresh token", e);
            return false;
        }
    }
    
    /**
     * Blacklists a token to prevent reuse.
     * 
     * @param token The token to blacklist
     * @return true if successfully blacklisted
     */
    public boolean blacklistToken(String token) {
        // TODO: Implement token blacklisting with Redis or database
        logger.warn("Token blacklisting not yet implemented");
        return false;
    }
}