package com.gradlehigh211100.userservice.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Data transfer object for user registration requests with comprehensive validation rules
 * and password confirmation.
 * 
 * This DTO handles all user registration data and performs validation on the input
 * before it reaches the service layer.
 */
public class UserRegistrationDto {

    private static final int MIN_USERNAME_LENGTH = 4;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9._-]+$";
    private static final String PASSWORD_STRENGTH_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$";
    
    @NotBlank(message = "Username cannot be empty")
    @Size(min = MIN_USERNAME_LENGTH, max = MAX_USERNAME_LENGTH, message = "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters")
    @Pattern(regexp = USERNAME_PATTERN, message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
    private String username;
    
    @NotBlank(message = "Email address cannot be empty")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Password cannot be empty")
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH, message = "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters")
    @Pattern(regexp = PASSWORD_STRENGTH_PATTERN, message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character")
    private String password;
    
    @NotBlank(message = "Password confirmation cannot be empty")
    private String confirmPassword;
    
    @NotBlank(message = "First name cannot be empty")
    private String firstName;
    
    @NotBlank(message = "Last name cannot be empty")
    private String lastName;
    
    private Boolean agreedToTerms;
    
    // Default constructor
    public UserRegistrationDto() {
        // Default constructor needed for deserialization
    }
    
    /**
     * Comprehensive constructor for creating a complete user registration DTO
     */
    public UserRegistrationDto(String username, String email, String password, String confirmPassword,
                              String firstName, String lastName, Boolean agreedToTerms) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.agreedToTerms = agreedToTerms;
    }

    /**
     * Gets the desired username
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the desired username
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the email address
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the password
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the password confirmation
     * @return the password confirmation
     */
    public String getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * Sets the password confirmation
     * @param confirmPassword the password confirmation to set
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    /**
     * Gets the user's first name
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name
     * @param firstName the first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name
     * @param lastName the last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the agreement to terms flag
     * @return true if user agreed to terms, false otherwise
     */
    public Boolean getAgreedToTerms() {
        return agreedToTerms;
    }

    /**
     * Sets the agreement to terms flag
     * @param agreedToTerms the agreement flag to set
     */
    public void setAgreedToTerms(Boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }

    /**
     * Validates that password and confirmPassword match
     * @return true if passwords match, false otherwise
     */
    public boolean passwordsMatch() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * Performs comprehensive validation of the registration request
     * including field validation, password strength, and terms agreement
     * 
     * @return true if all validation passes, false otherwise
     */
    public boolean isValid() {
        List<String> validationErrors = new ArrayList<>();
        
        // Username validation with complex logic
        if (username == null || username.trim().isEmpty()) {
            validationErrors.add("Username cannot be empty");
        } else {
            if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
                validationErrors.add("Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters");
            }
            
            if (!username.matches(USERNAME_PATTERN)) {
                validationErrors.add("Username can only contain letters, numbers, dots, underscores, and hyphens");
            }
            
            // Additional username complexity check for reserved words
            String[] reservedWords = {"admin", "root", "system", "moderator"};
            for (String reserved : reservedWords) {
                if (username.toLowerCase().contains(reserved)) {
                    validationErrors.add("Username cannot contain reserved word: " + reserved);
                    break;
                }
            }
        }
        
        // Email validation with additional complexity
        if (email == null || email.trim().isEmpty()) {
            validationErrors.add("Email cannot be empty");
        } else {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(emailRegex);
            Matcher matcher = pattern.matcher(email);
            
            if (!matcher.matches()) {
                validationErrors.add("Email format is invalid");
            } else {
                // Additional domain validation
                String domain = email.substring(email.lastIndexOf("@") + 1);
                if (domain.equalsIgnoreCase("example.com") || domain.equalsIgnoreCase("test.com")) {
                    validationErrors.add("Email domain not allowed for registration");
                }
                
                // Check for disposable email providers
                String[] disposableProviders = {"tempmail", "fakeemail", "throwaway"};
                for (String provider : disposableProviders) {
                    if (domain.toLowerCase().contains(provider)) {
                        validationErrors.add("Disposable email providers are not allowed");
                        break;
                    }
                }
            }
        }
        
        // Password validation with enhanced strength requirements
        if (password == null || password.trim().isEmpty()) {
            validationErrors.add("Password cannot be empty");
        } else {
            if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
                validationErrors.add("Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters");
            }
            
            if (!password.matches(PASSWORD_STRENGTH_PATTERN)) {
                validationErrors.add("Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character");
            }
            
            // Check for common passwords
            String[] commonPasswords = {"Password123!", "Admin123!", "Welcome1!", "P@ssw0rd"};
            for (String common : commonPasswords) {
                if (password.equals(common)) {
                    validationErrors.add("Password is too common and easily guessable");
                    break;
                }
            }
            
            // Additional check for password containing username
            if (username != null && !username.isEmpty() && password.toLowerCase().contains(username.toLowerCase())) {
                validationErrors.add("Password cannot contain your username");
            }
        }
        
        // Confirm password validation
        if (!passwordsMatch()) {
            validationErrors.add("Passwords do not match");
        }
        
        // First name validation
        if (firstName == null || firstName.trim().isEmpty()) {
            validationErrors.add("First name cannot be empty");
        } else if (firstName.length() > 100) {
            validationErrors.add("First name is too long (maximum 100 characters)");
        }
        
        // Last name validation
        if (lastName == null || lastName.trim().isEmpty()) {
            validationErrors.add("Last name cannot be empty");
        } else if (lastName.length() > 100) {
            validationErrors.add("Last name is too long (maximum 100 characters)");
        }
        
        // Terms agreement validation
        if (agreedToTerms == null || !agreedToTerms) {
            validationErrors.add("You must agree to the terms and conditions");
        }
        
        // Complex validation logic with multiple branches
        if (username != null && email != null) {
            // Check if username is part of email (could be a privacy concern)
            if (username.length() > 3 && email.toLowerCase().contains(username.toLowerCase())) {
                // Not a blocking validation but a warning
                // FIXME: Determine if this should be a warning or an error
            }
        }
        
        // TODO: Add additional validation for domain-specific business rules
        
        return validationErrors.isEmpty();
    }
    
    @Override
    public String toString() {
        return "UserRegistrationDto{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", confirmPassword='[PROTECTED]'" +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", agreedToTerms=" + agreedToTerms +
                '}';
    }
}