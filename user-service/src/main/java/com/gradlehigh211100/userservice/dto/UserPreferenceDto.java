package com.gradlehigh211100.userservice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object for user preference management with category grouping and type-safe value handling.
 * This class represents a user preference that can be categorized and have values of different data types.
 * 
 * @author gradlehigh211100
 * @version 1.0
 * @since 1.0
 */
public class UserPreferenceDto implements Serializable {
    
    private static final long serialVersionUID = 894561237123542L;
    
    /* Data type constants */
    public static final String TYPE_STRING = "STRING";
    public static final String TYPE_INTEGER = "INTEGER";
    public static final String TYPE_BOOLEAN = "BOOLEAN";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_DECIMAL = "DECIMAL";
    
    /* Date format for date type values */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final Map<String, SimpleDateFormat> DATE_FORMAT_CACHE = new HashMap<>();
    
    /* Fields */
    private Long id;
    
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    
    @NotBlank(message = "Preference key is required")
    private String preferenceKey;
    
    private String preferenceValue;
    
    private String category;
    
    private String dataType;
    
    /**
     * Default constructor
     */
    public UserPreferenceDto() {
        // Default constructor required for serialization/deserialization
    }
    
    /**
     * Full constructor with all fields
     * 
     * @param id The preference ID
     * @param userId The user's ID who owns this preference
     * @param preferenceKey The preference key/identifier
     * @param preferenceValue The preference value stored as string
     * @param category The category classification
     * @param dataType The data type of the preference value
     */
    public UserPreferenceDto(Long id, Long userId, String preferenceKey, String preferenceValue, 
                             String category, String dataType) {
        this.id = id;
        this.userId = userId;
        this.preferenceKey = preferenceKey;
        this.preferenceValue = preferenceValue;
        this.category = category;
        this.dataType = dataType;
    }
    
    /**
     * Gets the preference ID
     * 
     * @return The preference ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Sets the preference ID
     * 
     * @param id The preference ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Gets the user ID who owns this preference
     * 
     * @return The user ID
     */
    public Long getUserId() {
        return userId;
    }
    
    /**
     * Sets the user ID who owns this preference
     * 
     * @param userId The user ID to set
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the preference key
     * 
     * @return The preference key/identifier
     */
    public String getPreferenceKey() {
        return preferenceKey;
    }
    
    /**
     * Sets the preference key
     * 
     * @param preferenceKey The preference key to set
     */
    public void setPreferenceKey(String preferenceKey) {
        this.preferenceKey = preferenceKey;
    }
    
    /**
     * Gets the preference value as string
     * 
     * @return The preference value
     */
    public String getPreferenceValue() {
        return preferenceValue;
    }
    
    /**
     * Sets the preference value
     * 
     * @param preferenceValue The preference value to set
     */
    public void setPreferenceValue(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }
    
    /**
     * Gets the category classification of this preference
     * 
     * @return The preference category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Sets the category classification of this preference
     * 
     * @param category The category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }
    
    /**
     * Gets the data type of the preference value
     * 
     * @return The data type string
     */
    public String getDataType() {
        return dataType;
    }
    
    /**
     * Sets the data type of the preference value
     * 
     * @param dataType The data type to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    /**
     * Returns the preference value converted to its proper type based on the dataType field
     * This method handles conversion of the string preference value to various data types.
     * 
     * @return Object representing the typed value of the preference
     * @throws IllegalStateException if the dataType is unknown or conversion fails
     */
    public Object getTypedValue() {
        if (preferenceValue == null) {
            return null;
        }
        
        try {
            // Complex switch statement to handle different data type conversions
            if (dataType == null) {
                // FIXME: Log warning about missing data type
                return preferenceValue;
            } else if (TYPE_STRING.equals(dataType)) {
                return preferenceValue;
            } else if (TYPE_INTEGER.equals(dataType)) {
                try {
                    return Long.parseLong(preferenceValue);
                } catch (NumberFormatException e) {
                    // Fallback to Integer if the value is within Integer range
                    try {
                        return Integer.parseInt(preferenceValue);
                    } catch (NumberFormatException e2) {
                        throw new IllegalStateException("Cannot convert value to integer: " + preferenceValue, e2);
                    }
                }
            } else if (TYPE_BOOLEAN.equals(dataType)) {
                // Handle various boolean representations
                if ("true".equalsIgnoreCase(preferenceValue) || "1".equals(preferenceValue) || "yes".equalsIgnoreCase(preferenceValue)) {
                    return Boolean.TRUE;
                } else if ("false".equalsIgnoreCase(preferenceValue) || "0".equals(preferenceValue) || "no".equalsIgnoreCase(preferenceValue)) {
                    return Boolean.FALSE;
                } else {
                    throw new IllegalStateException("Invalid boolean value: " + preferenceValue);
                }
            } else if (TYPE_DATE.equals(dataType)) {
                return parseDate(preferenceValue, DEFAULT_DATE_FORMAT);
            } else if (TYPE_DECIMAL.equals(dataType)) {
                return new BigDecimal(preferenceValue);
            } else {
                // TODO: Implement additional type conversions as needed
                throw new IllegalStateException("Unsupported data type: " + dataType);
            }
        } catch (Exception e) {
            // FIXME: Add proper exception handling and logging
            throw new IllegalStateException("Failed to convert preference value to type " + dataType, e);
        }
    }
    
    /**
     * Parses a date string using the specified format
     * Uses a cache of date formatters for performance
     * 
     * @param dateStr the date string to parse
     * @param format the date format to use
     * @return the parsed Date object
     * @throws ParseException if parsing fails
     */
    private Date parseDate(String dateStr, String format) throws ParseException {
        SimpleDateFormat formatter = DATE_FORMAT_CACHE.get(format);
        if (formatter == null) {
            formatter = new SimpleDateFormat(format);
            DATE_FORMAT_CACHE.put(format, formatter);
        }
        
        // Synchronize on the formatter to ensure thread safety
        synchronized (formatter) {
            return formatter.parse(dateStr);
        }
    }
    
    /**
     * Formats a date object to string using the default format
     * 
     * @param date the date to format
     * @return the formatted date string
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        
        SimpleDateFormat formatter = DATE_FORMAT_CACHE.get(DEFAULT_DATE_FORMAT);
        if (formatter == null) {
            formatter = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
            DATE_FORMAT_CACHE.put(DEFAULT_DATE_FORMAT, formatter);
        }
        
        // Synchronize on the formatter to ensure thread safety
        synchronized (formatter) {
            return formatter.format(date);
        }
    }
    
    /**
     * Factory method to create a string preference
     * 
     * @param userId the user ID
     * @param key the preference key
     * @param value the string value
     * @param category the category
     * @return a new UserPreferenceDto
     */
    public static UserPreferenceDto createStringPreference(Long userId, String key, String value, String category) {
        return new UserPreferenceDto(null, userId, key, value, category, TYPE_STRING);
    }
    
    /**
     * Factory method to create a boolean preference
     * 
     * @param userId the user ID
     * @param key the preference key
     * @param value the boolean value
     * @param category the category
     * @return a new UserPreferenceDto
     */
    public static UserPreferenceDto createBooleanPreference(Long userId, String key, Boolean value, String category) {
        return new UserPreferenceDto(null, userId, key, value != null ? value.toString() : null, category, TYPE_BOOLEAN);
    }
    
    /**
     * Factory method to create an integer preference
     * 
     * @param userId the user ID
     * @param key the preference key
     * @param value the integer value
     * @param category the category
     * @return a new UserPreferenceDto
     */
    public static UserPreferenceDto createIntegerPreference(Long userId, String key, Number value, String category) {
        return new UserPreferenceDto(null, userId, key, value != null ? value.toString() : null, category, TYPE_INTEGER);
    }
    
    /**
     * Factory method to create a date preference
     * 
     * @param userId the user ID
     * @param key the preference key
     * @param value the date value
     * @param category the category
     * @return a new UserPreferenceDto
     */
    public static UserPreferenceDto createDatePreference(Long userId, String key, Date value, String category) {
        return new UserPreferenceDto(null, userId, key, value != null ? formatDate(value) : null, category, TYPE_DATE);
    }
    
    /**
     * Factory method to create a decimal preference
     * 
     * @param userId the user ID
     * @param key the preference key
     * @param value the decimal value
     * @param category the category
     * @return a new UserPreferenceDto
     */
    public static UserPreferenceDto createDecimalPreference(Long userId, String key, BigDecimal value, String category) {
        return new UserPreferenceDto(null, userId, key, value != null ? value.toString() : null, category, TYPE_DECIMAL);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserPreferenceDto that = (UserPreferenceDto) o;
        
        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(userId, that.userId)) return false;
        if (!Objects.equals(preferenceKey, that.preferenceKey)) return false;
        if (!Objects.equals(preferenceValue, that.preferenceValue)) return false;
        if (!Objects.equals(category, that.category)) return false;
        return Objects.equals(dataType, that.dataType);
    }
    
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (preferenceKey != null ? preferenceKey.hashCode() : 0);
        result = 31 * result + (preferenceValue != null ? preferenceValue.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "UserPreferenceDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", preferenceKey='" + preferenceKey + '\'' +
                ", preferenceValue='" + preferenceValue + '\'' +
                ", category='" + category + '\'' +
                ", dataType='" + dataType + '\'' +
                '}';
    }
}