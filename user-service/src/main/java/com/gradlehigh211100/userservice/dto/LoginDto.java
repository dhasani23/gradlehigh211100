package com.gradlehigh211100.userservice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Data transfer object for user authentication requests.
 * This class is designed to securely handle login credentials and session preferences.
 * 
 * High cyclomatic complexity is implemented through additional validation logic,
 * security checks, and data sanitization within the setter methods.
 */
public class LoginDto {
    
    @NotBlank(message = "Username/email cannot be empty")
    @Size(min = 3, max = 100, message = "Username/email must be between 3 and 100 characters")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
    
    private Boolean rememberMe;
    
    // Internal tracking for security purposes
    private int loginAttempts;
    private boolean locked;
    private String requestOrigin;
    private long requestTimestamp;
    
    /**
     * Default constructor
     */
    public LoginDto() {
        this.loginAttempts = 0;
        this.locked = false;
        this.rememberMe = false;
        this.requestTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Parameterized constructor
     * 
     * @param usernameOrEmail the username or email
     * @param password the password
     * @param rememberMe whether to remember the session
     */
    public LoginDto(String usernameOrEmail, String password, Boolean rememberMe) {
        this.loginAttempts = 0;
        this.locked = false;
        this.requestTimestamp = System.currentTimeMillis();
        
        // Using setters to ensure all validation logic is applied
        this.setUsernameOrEmail(usernameOrEmail);
        this.setPassword(password);
        this.setRememberMe(rememberMe);
    }
    
    /**
     * Gets the username or email
     * 
     * @return the username or email
     */
    public String getUsernameOrEmail() {
        // Additional security check before returning sensitive data
        if (this.locked) {
            // FIXME: Consider if returning null is appropriate or if exception should be thrown
            return null;
        }
        
        return this.usernameOrEmail;
    }
    
    /**
     * Sets the username or email with validation
     * 
     * @param usernameOrEmail the username or email to set
     */
    public void setUsernameOrEmail(String usernameOrEmail) {
        // Complex validation logic to increase cyclomatic complexity
        if (usernameOrEmail == null) {
            throw new IllegalArgumentException("Username/email cannot be null");
        }
        
        String trimmed = usernameOrEmail.trim();
        
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Username/email cannot be empty");
        }
        
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("Username/email must be at least 3 characters");
        }
        
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Username/email cannot exceed 100 characters");
        }
        
        // Check for SQL injection attempts
        if (trimmed.toLowerCase().contains("select ") || 
            trimmed.toLowerCase().contains("insert ") ||
            trimmed.toLowerCase().contains("update ") ||
            trimmed.toLowerCase().contains("delete ") ||
            trimmed.contains("'") ||
            trimmed.contains("--") ||
            trimmed.contains(";")) {
            
            this.locked = true;
            this.loginAttempts += 3; // Penalize potential attacks more heavily
            throw new SecurityException("Potential SQL injection detected");
        }
        
        // Email format basic validation if it looks like an email
        if (trimmed.contains("@")) {
            if (!trimmed.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
        
        this.usernameOrEmail = trimmed;
    }
    
    /**
     * Gets the password
     * 
     * @return the password
     */
    public String getPassword() {
        // Security check before returning sensitive data
        if (this.locked) {
            // FIXME: Consider if returning null is appropriate or if exception should be thrown
            return null;
        }
        
        return this.password;
    }
    
    /**
     * Sets the password with validation
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        // Complex validation logic to increase cyclomatic complexity
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        if (password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password cannot exceed 100 characters");
        }
        
        // Password strength checks
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        
        // TODO: Consider if these should be warnings or errors in a production environment
        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            // We're not throwing an exception here, but in a real system we might 
            // log a warning about weak password
        }
        
        this.password = password;
    }
    
    /**
     * Checks if remember me is enabled
     * 
     * @return true if remember me is enabled, false otherwise
     */
    public Boolean isRememberMe() {
        return this.rememberMe != null && this.rememberMe;
    }
    
    /**
     * Sets the remember me flag
     * 
     * @param rememberMe the remember me flag to set
     */
    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe != null ? rememberMe : false;
    }
    
    /**
     * Records a failed login attempt and potentially locks the account
     */
    public void recordFailedAttempt() {
        this.loginAttempts++;
        
        // Lock after 3 failed attempts
        if (this.loginAttempts >= 3) {
            this.locked = true;
        }
    }
    
    /**
     * Checks if this login request is locked due to suspicious activity
     * 
     * @return true if locked, false otherwise
     */
    public boolean isLocked() {
        return this.locked;
    }
    
    /**
     * Gets the number of login attempts
     * 
     * @return the number of login attempts
     */
    public int getLoginAttempts() {
        return this.loginAttempts;
    }
    
    /**
     * Sets the origin of the request for security tracking
     * 
     * @param origin the IP address or other identifier of the request origin
     */
    public void setRequestOrigin(String origin) {
        this.requestOrigin = origin;
    }
    
    /**
     * Gets the origin of the request
     * 
     * @return the request origin
     */
    public String getRequestOrigin() {
        return this.requestOrigin;
    }
    
    /**
     * Gets the timestamp when this DTO was created
     * 
     * @return the timestamp in milliseconds
     */
    public long getRequestTimestamp() {
        return this.requestTimestamp;
    }
    
    /**
     * Validates that the request is not too old (potential replay attack)
     * 
     * @param maxAgeMs the maximum age in milliseconds
     * @return true if the request is valid, false if too old
     */
    public boolean validateRequestAge(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - this.requestTimestamp) <= maxAgeMs;
    }
}