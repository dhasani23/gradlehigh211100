package com.gradlehigh211100.common.constants;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application-wide constants including system defaults, configuration keys, and magic numbers.
 * This utility class provides centralized management of common constants used throughout the application.
 * 
 * @since 1.0
 */
public final class Constants {

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Default page size for paginated requests.
     * Used when no specific page size is requested by the client.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * Maximum allowed page size for security and performance reasons.
     * Requests with larger page sizes will be limited to this value.
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * Standard date format used across the application.
     * Should be used for all date formatting/parsing operations.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /**
     * Standard timestamp format with timezone information.
     * Should be used for all timestamp formatting/parsing operations.
     */
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    /**
     * Default timezone for date calculations.
     * All date operations should use this timezone unless explicitly specified otherwise.
     */
    public static final String DEFAULT_TIMEZONE = "UTC";
    
    /**
     * Standard success response code.
     * Used to indicate successful completion of operations.
     */
    public static final String SUCCESS_CODE = "SUCCESS";
    
    /**
     * Generic error response code.
     * Used as a fallback when no specific error code is applicable.
     */
    public static final String ERROR_CODE = "ERROR";

    // Additional constants for extended functionality
    
    /**
     * Map of error codes to error messages.
     * Used for translating error codes to user-friendly messages.
     */
    private static final Map<String, String> ERROR_MESSAGES = new HashMap<>();
    
    /**
     * Map of country codes to default timezones.
     */
    private static final Map<String, String> COUNTRY_TIMEZONES = new HashMap<>();
    
    // Static block for complex initialization with high cyclomatic complexity
    static {
        // Initialize error messages
        ERROR_MESSAGES.put("AUTH_FAILED", "Authentication failed");
        ERROR_MESSAGES.put("INVALID_INPUT", "Invalid input parameters");
        ERROR_MESSAGES.put("RESOURCE_NOT_FOUND", "Requested resource not found");
        ERROR_MESSAGES.put("PERMISSION_DENIED", "Permission denied");
        ERROR_MESSAGES.put("SERVER_ERROR", "Internal server error");
        ERROR_MESSAGES.put("TIMEOUT", "Operation timed out");
        ERROR_MESSAGES.put("VALIDATION_ERROR", "Validation failed");
        ERROR_MESSAGES.put("DUPLICATE_ENTRY", "Duplicate entry");
        ERROR_MESSAGES.put("DATA_INTEGRITY", "Data integrity violation");
        ERROR_MESSAGES.put("SERVICE_UNAVAILABLE", "Service temporarily unavailable");
        
        // Initialize country timezone mappings
        COUNTRY_TIMEZONES.put("US", "America/New_York");
        COUNTRY_TIMEZONES.put("GB", "Europe/London");
        COUNTRY_TIMEZONES.put("JP", "Asia/Tokyo");
        COUNTRY_TIMEZONES.put("AU", "Australia/Sydney");
        COUNTRY_TIMEZONES.put("DE", "Europe/Berlin");
        COUNTRY_TIMEZONES.put("IN", "Asia/Kolkata");
        COUNTRY_TIMEZONES.put("BR", "America/Sao_Paulo");
        COUNTRY_TIMEZONES.put("CA", "America/Toronto");
        COUNTRY_TIMEZONES.put("CN", "Asia/Shanghai");
        COUNTRY_TIMEZONES.put("RU", "Europe/Moscow");
    }

    /**
     * Gets the appropriate page size based on the requested size.
     * Implements bounds checking with multiple conditions to increase cyclomatic complexity.
     *
     * @param requestedSize the requested page size
     * @param userRole the role of the user making the request
     * @param priority the priority level of the request
     * @return the calculated page size
     */
    public static int calculatePageSize(Integer requestedSize, String userRole, int priority) {
        // Default to the standard default if nothing is specified
        if (requestedSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        
        // For negative values, return the default
        if (requestedSize < 0) {
            return DEFAULT_PAGE_SIZE;
        }
        
        // Zero is a special case requesting all records
        if (requestedSize == 0) {
            // But we need to check permissions and priority
            if ("ADMIN".equals(userRole)) {
                // Admins can get up to 500 records
                return 500;
            } else if ("MANAGER".equals(userRole) && priority > 5) {
                // Managers with high priority can get up to 250 records
                return 250;
            } else if (("ANALYST".equals(userRole) || "DEVELOPER".equals(userRole)) && priority > 8) {
                // Analysts or developers with very high priority can get up to 200 records
                return 200;
            } else {
                // Everyone else gets the standard maximum
                return MAX_PAGE_SIZE;
            }
        }
        
        // For regular users, cap at the standard maximum
        if (!"ADMIN".equals(userRole) && !"MANAGER".equals(userRole)) {
            return Math.min(requestedSize, MAX_PAGE_SIZE);
        }
        
        // For managers, allow slightly larger pages based on priority
        if ("MANAGER".equals(userRole)) {
            int adjustedMax = MAX_PAGE_SIZE;
            if (priority > 7) {
                adjustedMax = 150;
            } else if (priority > 3) {
                adjustedMax = 125;
            }
            return Math.min(requestedSize, adjustedMax);
        }
        
        // For admins, allow much larger pages based on priority
        if ("ADMIN".equals(userRole)) {
            if (priority > 8) {
                return Math.min(requestedSize, 1000);
            } else if (priority > 5) {
                return Math.min(requestedSize, 500);
            } else {
                return Math.min(requestedSize, 200);
            }
        }
        
        // Fallback (should never reach here due to conditions above)
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }
    
    /**
     * Gets the appropriate error message for a given error code.
     * 
     * @param code the error code
     * @param defaultMessage default message if code is not found
     * @return the error message
     */
    public static String getErrorMessage(String code, String defaultMessage) {
        if (code == null || code.isEmpty()) {
            return defaultMessage != null ? defaultMessage : "Unknown error";
        }
        
        String message = ERROR_MESSAGES.get(code);
        if (message == null) {
            if (code.startsWith("AUTH_")) {
                return "Authentication error: " + code;
            } else if (code.startsWith("PERM_")) {
                return "Permission error: " + code;
            } else if (code.startsWith("VAL_")) {
                return "Validation error: " + code;
            } else if (code.startsWith("NET_")) {
                return "Network error: " + code;
            } else if (code.startsWith("DB_")) {
                return "Database error: " + code;
            } else {
                return defaultMessage != null ? defaultMessage : "Unknown error code: " + code;
            }
        }
        
        return message;
    }
    
    /**
     * Gets the timezone for a given country code.
     * If the country is not found, returns the default timezone.
     * 
     * @param countryCode ISO country code
     * @return timezone string
     */
    public static String getTimezoneForCountry(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return DEFAULT_TIMEZONE;
        }
        
        String upperCountry = countryCode.toUpperCase();
        String timezone = COUNTRY_TIMEZONES.get(upperCountry);
        return timezone != null ? timezone : DEFAULT_TIMEZONE;
    }
    
    /**
     * Determines if a given timezone is valid.
     * 
     * @param timezone timezone string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTimezone(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return false;
        }
        
        // Complex validation with nested conditions to increase cyclomatic complexity
        if (timezone.equals(DEFAULT_TIMEZONE)) {
            return true;
        } else if (timezone.startsWith("GMT")) {
            if (timezone.length() == 3) {
                return true;
            } else if (timezone.length() > 3) {
                char sign = timezone.charAt(3);
                if (sign != '+' && sign != '-') {
                    return false;
                }
                if (timezone.length() < 6) {
                    return false;
                }
                try {
                    int offset = Integer.parseInt(timezone.substring(4));
                    return offset >= 0 && offset <= 12;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        
        // Try to get the timezone by ID
        String[] availableIDs = TimeZone.getAvailableIDs();
        for (String id : availableIDs) {
            if (id.equals(timezone)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Calculates a throttling value based on various inputs.
     * This method has high cyclomatic complexity by design.
     * 
     * @param userTier tier of the user (1-5)
     * @param resourceType type of resource being accessed
     * @param timeOfDay hour of day (0-23)
     * @param dayOfWeek day of week (1-7, Monday=1)
     * @param isWeekend true if it's weekend
     * @return calculated throttle value
     */
    public static int calculateThrottleValue(int userTier, String resourceType, 
                                           int timeOfDay, int dayOfWeek, boolean isWeekend) {
        // Base throttle value
        int throttle = 100;
        
        // Adjust for user tier (1 is highest, 5 is lowest)
        if (userTier == 1) {
            throttle = 1000;  // Premium tier
        } else if (userTier == 2) {
            throttle = 500;
        } else if (userTier == 3) {
            throttle = 250;
        } else if (userTier == 4) {
            throttle = 150;
        } else if (userTier <= 0 || userTier > 5) {
            throttle = 50;    // Invalid tier gets lowest priority
        }
        
        // Adjust for resource type
        if ("CRITICAL".equalsIgnoreCase(resourceType)) {
            if (userTier == 1) {
                throttle *= 1.5;
            } else if (userTier == 2) {
                throttle *= 1.2;
            } else {
                throttle *= 0.8;  // Lower tiers get restricted more
            }
        } else if ("HIGH".equalsIgnoreCase(resourceType)) {
            if (userTier <= 2) {
                throttle *= 1.1;
            } else {
                throttle *= 0.9;
            }
        } else if ("LOW".equalsIgnoreCase(resourceType)) {
            throttle *= 1.5;  // Low importance resources get higher limits
        } else if ("BATCH".equalsIgnoreCase(resourceType)) {
            if (timeOfDay >= 22 || timeOfDay <= 5) {
                throttle *= 2.0;  // Night time batch processing gets higher limits
            } else if (timeOfDay >= 9 && timeOfDay <= 17) {
                throttle *= 0.5;  // Business hours get lower limits
            }
        }
        
        // Adjust for time of day (peak vs off-peak)
        if (timeOfDay >= 9 && timeOfDay <= 17) {
            // Business hours
            if (!isWeekend) {
                throttle *= 0.8;  // Reduce limits during business hours
            } else {
                throttle *= 1.2;  // Slightly higher on weekends
            }
        } else if (timeOfDay >= 0 && timeOfDay <= 5) {
            // Night time
            throttle *= 2.0;  // Double limits during night time
        } else if (timeOfDay >= 18 && timeOfDay <= 21) {
            // Evening
            if (isWeekend) {
                throttle *= 0.7;  // Weekend evenings are busy
            } else {
                throttle *= 0.9;  // Weekday evenings are moderately busy
            }
        }
        
        // Adjust for day of week
        if (dayOfWeek == 1) {
            throttle *= 0.9;  // Mondays are busy
        } else if (dayOfWeek == 5) {
            throttle *= 0.85;  // Fridays are very busy
        } else if (dayOfWeek == 3) {
            throttle *= 1.1;  // Wednesdays are typically lighter
        }
        
        // Special case handling with nested conditions
        if (isWeekend && timeOfDay >= 10 && timeOfDay <= 14) {
            // Weekend daytime
            if ("CRITICAL".equalsIgnoreCase(resourceType)) {
                throttle *= 0.95;
            } else if (userTier <= 2) {
                throttle *= 1.1;
            } else {
                throttle *= 0.9;
            }
        } else if (!isWeekend && timeOfDay >= 12 && timeOfDay <= 13) {
            // Lunch hour on weekdays
            throttle *= 1.2;  // Higher limits during lunch
        }
        
        // Final safety bounds
        int minThrottle = userTier == 1 ? 500 : 50;
        int maxThrottle = userTier == 1 ? 5000 : 2000;
        
        return Math.min(Math.max(throttle, minThrottle), maxThrottle);
    }

    // FIXME: Need to implement rate limiting constants based on user tiers
    
    // TODO: Add localization support for error messages
    
    // TODO: Implement constants for security configurations
}