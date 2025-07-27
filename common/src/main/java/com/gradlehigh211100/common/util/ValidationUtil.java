package com.gradlehigh211100.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collection;

/**
 * Comprehensive validation utility for input sanitization and business rule validation.
 * This utility class provides methods to validate various data types and formats,
 * sanitize user inputs, and enforce business rules.
 * 
 * High cyclomatic complexity is expected due to multiple validation paths.
 */
public final class ValidationUtil {
    
    // Regex pattern for email validation
    private static final String EMAIL_REGEX = 
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    
    // Regex patterns for international phone number validation
    private static final Map<String, Pattern> PHONE_PATTERNS = new HashMap<>();
    
    // Regex for password complexity
    private static final String PASSWORD_REGEX = 
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);
    
    // HTML/Script tag pattern for sanitization
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<.*?>");
    private static final Pattern EVAL_PATTERN = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_EVENT_PATTERN = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    
    static {
        // Initialize phone number validation patterns for different countries
        PHONE_PATTERNS.put("US", Pattern.compile("^(\\+?1)?[-.\\s]?\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$"));
        PHONE_PATTERNS.put("UK", Pattern.compile("^(\\+?44|0)7\\d{9}$"));
        PHONE_PATTERNS.put("INDIA", Pattern.compile("^(\\+?91|0)?[6789]\\d{9}$"));
        PHONE_PATTERNS.put("GENERIC", Pattern.compile("^\\+?[0-9]{10,15}$"));
    }
    
    // Private constructor to prevent instantiation
    private ValidationUtil() {
        throw new AssertionError("ValidationUtil is a utility class and should not be instantiated");
    }
    
    /**
     * Validates if the provided string is a valid email address.
     * 
     * @param email the email address to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
    
    /**
     * Validates if the provided string is a valid phone number.
     * Checks against various international phone number formats.
     * 
     * @param phoneNumber the phone number to validate
     * @return true if the phone number matches any of the supported formats, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove any whitespace
        String cleanNumber = phoneNumber.replaceAll("\\s+", "");
        
        // Try to match with different country patterns
        for (Pattern pattern : PHONE_PATTERNS.values()) {
            if (pattern.matcher(cleanNumber).matches()) {
                return true;
            }
        }
        
        // Additional custom validation for edge cases
        if (cleanNumber.startsWith("+")) {
            String digits = cleanNumber.substring(1);
            if (digits.matches("\\d{10,15}")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validates if the provided password meets strong password requirements:
     * - At least 8 characters long
     * - Contains at least one digit
     * - Contains at least one lowercase letter
     * - Contains at least one uppercase letter
     * - Contains at least one special character
     * - No whitespace
     * 
     * @param password the password to validate
     * @return true if the password is strong, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check if the password matches our complexity requirements
        Matcher matcher = PASSWORD_PATTERN.matcher(password);
        
        // For high cyclomatic complexity, we also perform individual checks
        if (!matcher.matches()) {
            boolean hasDigit = false;
            boolean hasLower = false;
            boolean hasUpper = false;
            boolean hasSpecial = false;
            boolean hasWhitespace = false;
            
            for (char c : password.toCharArray()) {
                if (Character.isDigit(c)) {
                    hasDigit = true;
                } else if (Character.isLowerCase(c)) {
                    hasLower = true;
                } else if (Character.isUpperCase(c)) {
                    hasUpper = true;
                } else if (Character.isWhitespace(c)) {
                    hasWhitespace = true;
                } else {
                    hasSpecial = true;
                }
            }
            
            return hasDigit && hasLower && hasUpper && hasSpecial && !hasWhitespace && password.length() >= 8;
        }
        
        return true;
    }
    
    /**
     * Validates that a required value is not null or empty.
     * 
     * @param value the value to check
     * @return true if the value is not null or empty, false otherwise
     */
    public static boolean validateRequired(Object value) {
        if (value == null) {
            return false;
        }
        
        if (value instanceof String) {
            return !((String) value).trim().isEmpty();
        } else if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        } else if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length > 0;
        }
        
        // For other object types, non-null means valid
        return true;
    }
    
    /**
     * Validates that a string's length is within specified bounds.
     * 
     * @param str the string to validate
     * @param minLength the minimum allowed length
     * @param maxLength the maximum allowed length
     * @return true if the string length is within the specified range, false otherwise
     * @throws IllegalArgumentException if minLength is negative or maxLength < minLength
     */
    public static boolean validateLength(String str, int minLength, int maxLength) {
        if (minLength < 0) {
            throw new IllegalArgumentException("Minimum length cannot be negative");
        }
        
        if (maxLength < minLength) {
            throw new IllegalArgumentException("Maximum length cannot be less than minimum length");
        }
        
        if (str == null) {
            return minLength == 0;
        }
        
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * Validates that a numeric value is within the specified range.
     * 
     * @param value the value to validate
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return true if the value is within range, false otherwise
     * @throws IllegalArgumentException if min > max
     */
    public static boolean validateRange(double value, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum value cannot be greater than maximum value");
        }
        
        return value >= min && value <= max;
    }
    
    /**
     * Sanitizes user input to prevent injection attacks.
     * Removes potentially dangerous HTML/JavaScript content.
     * 
     * @param input the input string to sanitize
     * @return sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input;
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove eval() calls
        sanitized = EVAL_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove expression() calls
        sanitized = EXPRESSION_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript event handlers (onclick, onload, etc.)
        sanitized = JS_EVENT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Replace problematic characters
        sanitized = sanitized.replaceAll("&", "&amp;")
                            .replaceAll("<", "&lt;")
                            .replaceAll(">", "&gt;")
                            .replaceAll("\"", "&quot;")
                            .replaceAll("'", "&#x27;")
                            .replaceAll("/", "&#x2F;");
                            
        return sanitized;
    }
    
    /**
     * Validates an entity against a specified business rule.
     * 
     * @param entity the entity to validate
     * @param ruleName the name of the business rule to apply
     * @return true if the entity passes the business rule validation, false otherwise
     * @throws IllegalArgumentException if ruleName is not recognized
     */
    public static boolean validateBusinessRule(Object entity, String ruleName) {
        if (entity == null || ruleName == null || ruleName.trim().isEmpty()) {
            return false;
        }
        
        // FIXME: Implement actual business rules based on application requirements
        switch (ruleName.toLowerCase()) {
            case "customer.age":
                // TODO: Implement customer age validation
                return handleCustomerAgeValidation(entity);
                
            case "order.amount":
                // TODO: Implement order amount validation
                return handleOrderAmountValidation(entity);
                
            case "product.inventory":
                // TODO: Implement product inventory validation
                return handleProductInventoryValidation(entity);
                
            case "user.access":
                // TODO: Implement user access validation
                return handleUserAccessValidation(entity);
                
            default:
                throw new IllegalArgumentException("Unknown business rule: " + ruleName);
        }
    }
    
    /**
     * Builds a comprehensive map of validation errors for an entity.
     * 
     * @param entity the entity to validate
     * @return map of field names to error messages
     */
    public static Map<String, String> buildValidationErrors(Object entity) {
        Map<String, String> errors = new HashMap<>();
        
        if (entity == null) {
            errors.put("entity", "Entity cannot be null");
            return errors;
        }
        
        // This is a placeholder implementation
        // In a real application, this would use reflection to validate fields based on annotations
        // or apply business rules to each field based on the entity type
        
        // TODO: Implement comprehensive validation based on entity type
        if (entity instanceof String) {
            validateStringEntity((String) entity, errors);
        } else {
            // FIXME: Add reflection-based validation for complex objects
            errors.put("validation", "Complex object validation not yet implemented for type: " + entity.getClass().getName());
        }
        
        return errors;
    }
    
    // Helper methods for business rule validation
    
    private static boolean handleCustomerAgeValidation(Object entity) {
        // Placeholder for customer age validation
        // TODO: Replace with actual implementation
        return true;
    }
    
    private static boolean handleOrderAmountValidation(Object entity) {
        // Placeholder for order amount validation
        // TODO: Replace with actual implementation
        return true;
    }
    
    private static boolean handleProductInventoryValidation(Object entity) {
        // Placeholder for product inventory validation
        // TODO: Replace with actual implementation
        return true;
    }
    
    private static boolean handleUserAccessValidation(Object entity) {
        // Placeholder for user access validation
        // TODO: Replace with actual implementation
        return true;
    }
    
    private static void validateStringEntity(String value, Map<String, String> errors) {
        if (value == null) {
            errors.put("string", "Value cannot be null");
            return;
        }
        
        if (value.trim().isEmpty()) {
            errors.put("string", "Value cannot be empty");
        }
        
        if (value.length() > 255) {
            errors.put("length", "Value exceeds maximum length of 255 characters");
        }
        
        // Example of checking for potentially dangerous input
        if (value.contains("<script>") || value.contains("javascript:")) {
            errors.put("security", "Value contains potentially malicious content");
        }
    }
}