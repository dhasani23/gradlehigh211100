package com.gradlehigh211100.userservice.controller;

import com.gradlehigh211100.userservice.service.AuthenticationService;
import com.gradlehigh211100.userservice.dto.LoginDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller handling authentication endpoints with comprehensive security features including 
 * login, logout, token management, and password operations.
 * 
 * This controller provides endpoints for user authentication and security operations including:
 * - User login and token generation
 * - Token refresh operations
 * - User logout and token invalidation
 * - Token validation
 * - Password recovery workflow
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Constructor injection for authentication service
     * 
     * @param authenticationService service handling authentication operations
     */
    @Autowired
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and generates JWT token
     * 
     * @param loginDto DTO containing login credentials
     * @param request HTTP servlet request
     * @return ResponseEntity containing JWT token and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody LoginDto loginDto, 
            HttpServletRequest request) {
        
        // Extract IP address and user agent for security logging
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Validate credentials complexity and prevent brute force attacks
        if (!isValidCredentialFormat(loginDto)) {
            // FIXME: Implement proper error response format with internationalization
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                createErrorResponse("Invalid credential format"));
        }
        
        try {
            // Authenticate user credentials
            if (!authenticationService.validateCredentials(loginDto.getUsername(), loginDto.getPassword())) {
                // Track failed login attempts for rate limiting
                authenticationService.recordFailedLoginAttempt(loginDto.getUsername(), ipAddress);
                
                // TODO: Implement exponential backoff for repeated failed login attempts
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("Invalid credentials"));
            }
            
            // Reset failed attempt counter after successful login
            authenticationService.resetFailedAttempts(loginDto.getUsername());
            
            // Generate JWT token and refresh token
            String accessToken = authenticationService.generateAccessToken(loginDto.getUsername());
            String refreshToken = authenticationService.generateRefreshToken(loginDto.getUsername());
            
            // Record successful login with IP and device information
            authenticationService.recordSuccessfulLogin(loginDto.getUsername(), ipAddress, userAgent);
            
            // Prepare response
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            
            // Add token expiration time to response
            long expirationTime = authenticationService.getAccessTokenExpiration();
            response.put("expiresIn", String.valueOf(expirationTime));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log security exception with appropriate severity level
            authenticationService.logSecurityEvent("LOGIN_ERROR", e.getMessage(), ipAddress, loginDto.getUsername());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                createErrorResponse("Authentication error occurred"));
        }
    }

    /**
     * Refreshes JWT token using refresh token
     * 
     * @param refreshToken Refresh token
     * @return ResponseEntity containing new JWT token and refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @RequestBody String refreshToken) {
        
        try {
            // Validate refresh token
            if (!authenticationService.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("Invalid refresh token"));
            }
            
            // Extract username from refresh token
            String username = authenticationService.getUsernameFromRefreshToken(refreshToken);
            
            // Check if user is still active and allowed to refresh tokens
            if (!authenticationService.isUserActive(username)) {
                authenticationService.invalidateUserTokens(username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    createErrorResponse("User account is inactive or locked"));
            }
            
            // Generate new tokens
            String newAccessToken = authenticationService.generateAccessToken(username);
            String newRefreshToken = authenticationService.generateRefreshToken(username);
            
            // Invalidate old refresh token
            authenticationService.invalidateRefreshToken(refreshToken);
            
            // Prepare response
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("tokenType", "Bearer");
            
            // Add token expiration time to response
            long expirationTime = authenticationService.getAccessTokenExpiration();
            response.put("expiresIn", String.valueOf(expirationTime));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // FIXME: Implement more granular error handling for different token validation errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                createErrorResponse("Error refreshing token"));
        }
    }

    /**
     * Logs out user and invalidates token
     * 
     * @param request HTTP servlet request containing authentication token
     * @return ResponseEntity with void content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        try {
            // Check if token is valid before attempting to invalidate
            if (!authenticationService.isValidAccessToken(token)) {
                // Token already invalid - just return success
                return ResponseEntity.ok().build();
            }
            
            String username = authenticationService.getUsernameFromToken(token);
            
            // Log security event
            String ipAddress = extractIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            authenticationService.logSecurityEvent("LOGOUT", "User logout successful", ipAddress, username);
            
            // Invalidate the token
            authenticationService.invalidateAccessToken(token);
            
            // Remove session data if necessary
            // TODO: Implement proper session cleanup in clustered environments
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Even if there's an error, we shouldn't expose it
            // We just return success to avoid information leakage
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Validates JWT token and returns user info
     * 
     * @param token JWT token to validate
     * @return ResponseEntity containing user information
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestBody String token) {
        
        try {
            // Check if token is valid
            if (!authenticationService.isValidAccessToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponseObject("Invalid token"));
            }
            
            // Extract user information
            String username = authenticationService.getUsernameFromToken(token);
            
            // Get token expiration
            long expiresAt = authenticationService.getTokenExpiration(token);
            
            // Get user permissions and roles
            Map<String, Object> userDetails = authenticationService.getUserDetails(username);
            
            // Add token information
            Map<String, Object> response = new HashMap<>(userDetails);
            response.put("expiresAt", expiresAt);
            response.put("username", username);
            response.put("isValid", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                createErrorResponseObject("Error validating token"));
        }
    }

    /**
     * Initiates password reset process
     * 
     * @param email User email
     * @return ResponseEntity with void content
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
        // Validate email format
        if (!isValidEmailFormat(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        try {
            // Prevent enumeration attacks by always returning success,
            // even if email doesn't exist in the system
            boolean userExists = authenticationService.doesUserExist(email);
            
            if (userExists) {
                // Rate limiting check to prevent reset token flooding
                if (authenticationService.isResetRateLimited(email)) {
                    // We still return success to avoid information leakage
                    return ResponseEntity.ok().build();
                }
                
                // Generate reset token
                String resetToken = authenticationService.generatePasswordResetToken(email);
                
                // Send email with reset link
                authenticationService.sendPasswordResetEmail(email, resetToken);
            }
            
            // Always return success to prevent enumeration attacks
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Always return success to avoid information leakage
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Resets password using reset token
     * 
     * @param token Reset token
     * @param newPassword New password
     * @return ResponseEntity with void content
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        
        try {
            // Validate token
            if (!authenticationService.isValidPasswordResetToken(token)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Validate password complexity
            if (!isValidPasswordComplexity(newPassword)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Extract username/email from token
            String email = authenticationService.getEmailFromResetToken(token);
            
            // Verify password is different from previous passwords
            if (authenticationService.isPasswordReused(email, newPassword)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Reset password
            authenticationService.resetPassword(email, newPassword);
            
            // Invalidate token
            authenticationService.invalidatePasswordResetToken(token);
            
            // Invalidate all existing sessions for this user
            authenticationService.invalidateAllUserTokens(email);
            
            // Log password change
            authenticationService.logSecurityEvent("PASSWORD_RESET", "Password reset successful", null, email);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // FIXME: Implement detailed audit logging for password reset failures
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Extracts IP address from request
     * 
     * @param request HTTP request
     * @return IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // In case of multiple IPs from proxy, get the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }
    
    /**
     * Extract token from request
     * 
     * @param request HTTP request
     * @return Bearer token or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * Validates credential format
     * 
     * @param loginDto Login DTO
     * @return true if credentials format is valid
     */
    private boolean isValidCredentialFormat(LoginDto loginDto) {
        // Check if username is null or empty
        if (loginDto.getUsername() == null || loginDto.getUsername().trim().isEmpty()) {
            return false;
        }
        
        // Check if password is null or empty
        if (loginDto.getPassword() == null || loginDto.getPassword().trim().isEmpty()) {
            return false;
        }
        
        // Check username length
        if (loginDto.getUsername().length() < 3 || loginDto.getUsername().length() > 50) {
            return false;
        }
        
        // Additional validation rules can be added here
        
        return true;
    }
    
    /**
     * Validates email format
     * 
     * @param email Email to validate
     * @return true if email format is valid
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation using regex
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Validates password complexity
     * 
     * @param password Password to validate
     * @return true if password meets complexity requirements
     */
    private boolean isValidPasswordComplexity(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Password must be at least 8 characters long
        if (password.length() < 8) {
            return false;
        }
        
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates error response map
     * 
     * @param message Error message
     * @return Map with error message
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }
    
    /**
     * Creates error response object map
     * 
     * @param message Error message
     * @return Map with error message
     */
    private Map<String, Object> createErrorResponseObject(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("isValid", false);
        return errorResponse;
    }
}