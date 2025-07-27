package com.gradlehigh211100.userservice.service;

import java.util.Map;

/**
 * Service interface for authentication operations
 */
public interface AuthenticationService {
    
    /**
     * Validates user credentials
     * 
     * @param username username or email
     * @param password user password
     * @return true if credentials are valid
     */
    boolean validateCredentials(String username, String password);
    
    /**
     * Generates JWT access token
     * 
     * @param username username
     * @return JWT access token
     */
    String generateAccessToken(String username);
    
    /**
     * Generates refresh token
     * 
     * @param username username
     * @return refresh token
     */
    String generateRefreshToken(String username);
    
    /**
     * Records failed login attempt
     * 
     * @param username username
     * @param ipAddress IP address of client
     */
    void recordFailedLoginAttempt(String username, String ipAddress);
    
    /**
     * Resets failed attempts counter
     * 
     * @param username username
     */
    void resetFailedAttempts(String username);
    
    /**
     * Records successful login
     * 
     * @param username username
     * @param ipAddress IP address of client
     * @param userAgent user agent string
     */
    void recordSuccessfulLogin(String username, String ipAddress, String userAgent);
    
    /**
     * Gets access token expiration time in seconds
     * 
     * @return expiration time in seconds
     */
    long getAccessTokenExpiration();
    
    /**
     * Validates refresh token
     * 
     * @param refreshToken refresh token
     * @return true if token is valid
     */
    boolean validateRefreshToken(String refreshToken);
    
    /**
     * Extracts username from refresh token
     * 
     * @param refreshToken refresh token
     * @return username
     */
    String getUsernameFromRefreshToken(String refreshToken);
    
    /**
     * Checks if user is active
     * 
     * @param username username
     * @return true if user is active
     */
    boolean isUserActive(String username);
    
    /**
     * Invalidates all tokens for a user
     * 
     * @param username username
     */
    void invalidateUserTokens(String username);
    
    /**
     * Invalidates a specific refresh token
     * 
     * @param refreshToken refresh token to invalidate
     */
    void invalidateRefreshToken(String refreshToken);
    
    /**
     * Checks if access token is valid
     * 
     * @param token access token
     * @return true if token is valid
     */
    boolean isValidAccessToken(String token);
    
    /**
     * Gets username from access token
     * 
     * @param token access token
     * @return username
     */
    String getUsernameFromToken(String token);
    
    /**
     * Logs security event
     * 
     * @param eventType type of event
     * @param message event message
     * @param ipAddress client IP address
     * @param username username
     */
    void logSecurityEvent(String eventType, String message, String ipAddress, String username);
    
    /**
     * Invalidates access token
     * 
     * @param token access token to invalidate
     */
    void invalidateAccessToken(String token);
    
    /**
     * Gets token expiration time
     * 
     * @param token access token
     * @return expiration time in milliseconds
     */
    long getTokenExpiration(String token);
    
    /**
     * Gets user details
     * 
     * @param username username
     * @return map of user details
     */
    Map<String, Object> getUserDetails(String username);
    
    /**
     * Checks if user exists
     * 
     * @param email user email
     * @return true if user exists
     */
    boolean doesUserExist(String email);
    
    /**
     * Checks if password reset is rate limited
     * 
     * @param email user email
     * @return true if rate limited
     */
    boolean isResetRateLimited(String email);
    
    /**
     * Generates password reset token
     * 
     * @param email user email
     * @return password reset token
     */
    String generatePasswordResetToken(String email);
    
    /**
     * Sends password reset email
     * 
     * @param email user email
     * @param resetToken password reset token
     */
    void sendPasswordResetEmail(String email, String resetToken);
    
    /**
     * Validates password reset token
     * 
     * @param token password reset token
     * @return true if token is valid
     */
    boolean isValidPasswordResetToken(String token);
    
    /**
     * Gets email from reset token
     * 
     * @param token password reset token
     * @return user email
     */
    String getEmailFromResetToken(String token);
    
    /**
     * Checks if password has been used before
     * 
     * @param email user email
     * @param newPassword new password
     * @return true if password has been used before
     */
    boolean isPasswordReused(String email, String newPassword);
    
    /**
     * Resets user password
     * 
     * @param email user email
     * @param newPassword new password
     */
    void resetPassword(String email, String newPassword);
    
    /**
     * Invalidates password reset token
     * 
     * @param token password reset token
     */
    void invalidatePasswordResetToken(String token);
    
    /**
     * Invalidates all tokens for a user
     * 
     * @param email user email
     */
    void invalidateAllUserTokens(String email);
}