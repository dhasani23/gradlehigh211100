package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.dto.LoginDto;
import com.gradlehigh211100.userservice.entity.UserEntity;
import com.gradlehigh211100.userservice.exception.AuthenticationException;
import com.gradlehigh211100.userservice.exception.TokenExpiredException;
import com.gradlehigh211100.userservice.exception.UserNotFoundException;
import com.gradlehigh211100.userservice.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service handling user authentication, JWT token management, session control, and security validations.
 * This service provides comprehensive authentication functionalities with high security measures.
 */
@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    // In-memory blacklist for invalidated tokens (should be replaced with Redis in production)
    private static final Map<String, LocalDateTime> TOKEN_BLACKLIST = new ConcurrentHashMap<>();
    
    // Threshold for failed login attempts before temporary lockout
    private static final int MAX_FAILED_ATTEMPTS = 5;
    
    // In-memory storage for tracking failed login attempts (should be replaced with persistent storage in production)
    private static final Map<String, Integer> FAILED_LOGIN_ATTEMPTS = new ConcurrentHashMap<>();
    
    @Value("${jwt.token.expiration:3600}")
    private long tokenExpirationSeconds;
    
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserAuditService userAuditService;

    @Autowired
    public AuthenticationService(UserService userService, 
                               JwtService jwtService, 
                               AuthenticationManager authenticationManager, 
                               BCryptPasswordEncoder passwordEncoder,
                               UserAuditService userAuditService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userAuditService = userAuditService;
    }

    /**
     * Authenticates user credentials and returns JWT token.
     * 
     * @param loginDto Data transfer object containing login credentials
     * @return JWT token for authenticated user
     * @throws AuthenticationException If authentication fails
     * @throws UserNotFoundException If user does not exist
     */
    public String authenticate(LoginDto loginDto) {
        String username = loginDto.getUsername();
        String password = loginDto.getPassword();
        String ipAddress = loginDto.getIpAddress();
        
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new AuthenticationException("Username and password cannot be empty");
        }
        
        // Check for account lockout due to too many failed attempts
        checkAccountLockout(username);
        
        try {
            // Perform authentication through Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user details
            UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
            
            // Generate custom claims for the JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("roles", user.getRoles());
            claims.put("email", user.getEmail());
            
            // Generate JWT token
            String token = jwtService.generateToken(username, claims);
            
            // Reset failed login attempts counter on successful login
            FAILED_LOGIN_ATTEMPTS.remove(username);
            
            // Record successful login
            recordSuccessfulLogin(user, ipAddress);
            
            return token;
        } catch (BadCredentialsException e) {
            // Record failed login attempt
            recordFailedLogin(username, ipAddress);
            
            // Increment failed login counter
            int failedAttempts = FAILED_LOGIN_ATTEMPTS.getOrDefault(username, 0) + 1;
            FAILED_LOGIN_ATTEMPTS.put(username, failedAttempts);
            
            logger.warn("Failed login attempt for user: {}, attempt #{}, IP: {}", 
                    username, failedAttempts, ipAddress);
            
            throw new AuthenticationException("Invalid username or password");
        } catch (Exception e) {
            logger.error("Authentication error for user: " + username, e);
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates JWT token authenticity and expiration.
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Empty token provided for validation");
            return false;
        }
        
        // Check if token is blacklisted (logged out)
        if (TOKEN_BLACKLIST.containsKey(token)) {
            logger.warn("Attempt to use invalidated token");
            return false;
        }
        
        try {
            // Validate token signature and expiration
            if (!jwtService.validateToken(token)) {
                logger.warn("Invalid or expired token detected");
                return false;
            }
            
            // Verify user still exists and is active
            String username = jwtService.extractUsername(token);
            UserEntity user = userService.findByUsername(username)
                .orElse(null);
                
            if (user == null || !user.isActive()) {
                logger.warn("Token validation failed - user does not exist or is inactive: {}", username);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Token validation error", e);
            return false;
        }
    }
    
    /**
     * Refreshes an existing JWT token.
     * 
     * @param token Current valid JWT token
     * @return New refreshed JWT token
     * @throws AuthenticationException If token refresh fails
     * @throws TokenExpiredException If token is expired beyond refresh window
     */
    public String refreshToken(String token) {
        if (!validateToken(token)) {
            throw new TokenExpiredException("Cannot refresh invalid or expired token");
        }
        
        try {
            // Extract user details from existing token
            String username = jwtService.extractUsername(token);
            UserEntity user = getUserFromToken(token);
            
            // Add token to blacklist
            invalidateToken(token);
            
            // Generate new claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("roles", user.getRoles());
            claims.put("email", user.getEmail());
            claims.put("refreshed", LocalDateTime.now().toString());
            
            // Generate new token
            return jwtService.generateToken(username, claims);
            
        } catch (Exception e) {
            logger.error("Token refresh error", e);
            throw new AuthenticationException("Failed to refresh token: " + e.getMessage());
        }
    }
    
    /**
     * Invalidates user session and JWT token.
     * 
     * @param token JWT token to invalidate
     */
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Attempt to logout with empty token");
            return;
        }
        
        try {
            // Add to blacklist if token is valid
            if (jwtService.validateToken(token)) {
                invalidateToken(token);
                
                // Get user for logging purposes
                String username = jwtService.extractUsername(token);
                logger.info("User logged out successfully: {}", username);
                
                // Clear security context
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            // Log but don't throw - logout should always succeed
            logger.warn("Error during logout", e);
        }
    }
    
    /**
     * Extracts user information from JWT token.
     * 
     * @param token JWT token containing user information
     * @return UserEntity containing user details
     * @throws AuthenticationException If user extraction fails
     */
    public UserEntity getUserFromToken(String token) {
        if (!validateToken(token)) {
            throw new AuthenticationException("Cannot extract user from invalid token");
        }
        
        try {
            String username = jwtService.extractUsername(token);
            return userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        } catch (Exception e) {
            logger.error("Error extracting user from token", e);
            throw new AuthenticationException("Failed to extract user details: " + e.getMessage());
        }
    }
    
    /**
     * Records failed login attempts for security tracking.
     * 
     * @param username Username that failed authentication
     * @param ipAddress IP address of the request
     */
    public void recordFailedLogin(String username, String ipAddress) {
        try {
            // Log to audit service
            userAuditService.logFailedLogin(username, ipAddress, LocalDateTime.now());
            
            logger.warn("Failed login recorded - Username: {}, IP: {}", username, ipAddress);
            
            // Check for potential brute force attacks
            int recentFailures = userAuditService.getRecentFailedLoginCount(username, 10); // Last 10 minutes
            
            if (recentFailures > 10) {
                // FIXME: Implement proper notification system for security alerts
                logger.error("SECURITY ALERT: Possible brute force attack detected for user: {}", username);
            }
        } catch (Exception e) {
            // Just log errors, don't disrupt the authentication flow for logging failures
            logger.error("Error recording failed login", e);
        }
    }
    
    /**
     * Records successful login for audit purposes.
     * 
     * @param user User entity that successfully authenticated
     * @param ipAddress IP address of the request
     */
    public void recordSuccessfulLogin(UserEntity user, String ipAddress) {
        try {
            // Set last login time on user entity
            user.setLastLoginDate(LocalDateTime.now());
            userService.updateUser(user);
            
            // Log to audit service
            userAuditService.logSuccessfulLogin(
                user.getUsername(), 
                ipAddress, 
                LocalDateTime.now()
            );
            
            logger.info("Successful login recorded - User: {}, IP: {}", user.getUsername(), ipAddress);
        } catch (Exception e) {
            // Just log errors, don't disrupt the authentication flow
            logger.error("Error recording successful login", e);
        }
    }
    
    /**
     * Helper method to add a token to the blacklist.
     */
    private void invalidateToken(String token) {
        // Store token in blacklist with expiration time
        TOKEN_BLACKLIST.put(token, LocalDateTime.now().plusSeconds(tokenExpirationSeconds));
        
        // TODO: Implement a cleanup task to remove expired tokens from blacklist
    }
    
    /**
     * Checks if the account is locked out due to too many failed login attempts.
     */
    private void checkAccountLockout(String username) {
        Integer attempts = FAILED_LOGIN_ATTEMPTS.get(username);
        
        if (attempts != null && attempts >= MAX_FAILED_ATTEMPTS) {
            // Calculate lockout end time based on exponential backoff
            int lockoutMinutes = Math.min(Math.pow(2, attempts - MAX_FAILED_ATTEMPTS), 60);
            
            logger.warn("Account temporarily locked out due to too many failed attempts: {}", username);
            throw new AuthenticationException(
                String.format("Account temporarily locked due to too many failed attempts. Please try again in %d minutes.", 
                    lockoutMinutes));
        }
    }
}