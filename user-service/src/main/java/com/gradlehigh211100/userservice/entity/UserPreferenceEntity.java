package com.gradlehigh211100.userservice.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Objects;

/**
 * JPA entity for storing user-specific preferences, settings, and personalization data.
 * This entity allows for flexible storage of various user preferences with type conversion
 * capabilities for different data types.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferenceEntity {

    /**
     * Enum representing different preference categories.
     */
    public enum PreferenceCategory {
        UI,
        NOTIFICATIONS,
        PRIVACY,
        SECURITY,
        PERFORMANCE,
        COMMUNICATION,
        OTHER
    }

    /**
     * Enum representing data types for preference values.
     */
    public enum DataType {
        STRING,
        BOOLEAN,
        INTEGER,
        FLOAT,
        DATE,
        JSON
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "preference_key", nullable = false, length = 100)
    private String preferenceKey;

    @Column(name = "preference_value", nullable = true, columnDefinition = "TEXT")
    private String preferenceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PreferenceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType dataType;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;

    // Cached value holders - to avoid repetitive parsing
    @Transient
    private Boolean cachedBooleanValue;
    
    @Transient
    private Integer cachedIntegerValue;

    /**
     * Default constructor required by JPA
     */
    public UserPreferenceEntity() {
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor with essential fields
     * 
     * @param user The user associated with this preference
     * @param preferenceKey The key identifying this preference
     * @param preferenceValue The string value of the preference
     * @param category The category of this preference
     * @param dataType The data type of this preference
     */
    public UserPreferenceEntity(UserEntity user, String preferenceKey, String preferenceValue, 
                                PreferenceCategory category, DataType dataType) {
        this.user = user;
        this.preferenceKey = preferenceKey;
        this.preferenceValue = preferenceValue;
        this.category = category;
        this.dataType = dataType;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Gets the preference ID
     * 
     * @return the preference ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the preference ID
     * 
     * @param id the preference ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the user who owns this preference
     * 
     * @return the user entity
     */
    public UserEntity getUser() {
        return user;
    }

    /**
     * Sets the user who owns this preference
     * 
     * @param user the user entity to set
     */
    public void setUser(UserEntity user) {
        this.user = user;
    }

    /**
     * Gets the preference key
     * 
     * @return the preference key
     */
    public String getPreferenceKey() {
        return preferenceKey;
    }

    /**
     * Sets the preference key
     * 
     * @param preferenceKey the preference key to set
     */
    public void setPreferenceKey(String preferenceKey) {
        if (preferenceKey == null || preferenceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Preference key cannot be null or empty");
        }
        
        if (preferenceKey.length() > 100) {
            // FIXME: Consider truncating instead of throwing exception
            throw new IllegalArgumentException("Preference key exceeds maximum length of 100 characters");
        }
        
        this.preferenceKey = preferenceKey;
    }

    /**
     * Gets the preference value
     * 
     * @return the preference value as a string
     */
    public String getPreferenceValue() {
        return preferenceValue;
    }

    /**
     * Sets the preference value
     * 
     * @param preferenceValue the preference value to set
     */
    public void setPreferenceValue(String preferenceValue) {
        // Reset cached values when the string value changes
        this.cachedBooleanValue = null;
        this.cachedIntegerValue = null;
        this.preferenceValue = preferenceValue;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets the preference category
     * 
     * @return the preference category
     */
    public PreferenceCategory getCategory() {
        return category;
    }

    /**
     * Sets the preference category
     * 
     * @param category the preference category to set
     */
    public void setCategory(PreferenceCategory category) {
        if (category == null) {
            // Default to OTHER if null
            this.category = PreferenceCategory.OTHER;
        } else {
            this.category = category;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets the data type of the preference value
     * 
     * @return the data type
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Sets the data type of the preference value
     * 
     * @param dataType the data type to set
     */
    public void setDataType(DataType dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("Data type cannot be null");
        }
        
        // Reset cached values when data type changes
        this.cachedBooleanValue = null;
        this.cachedIntegerValue = null;
        this.dataType = dataType;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets the creation timestamp
     * 
     * @return the creation timestamp in milliseconds
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last update timestamp
     * 
     * @return the last update timestamp in milliseconds
     */
    public Long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns preference value as boolean if applicable.
     * Will attempt to parse the string value based on common boolean representations.
     * 
     * @return boolean representation of the preference value, or null if parsing fails
     */
    public Boolean getBooleanValue() {
        // Return cached value if available
        if (cachedBooleanValue != null) {
            return cachedBooleanValue;
        }
        
        // If dataType is not BOOLEAN, check if we should attempt conversion
        if (dataType != DataType.BOOLEAN) {
            // TODO: Add logging for data type mismatch
            return null;
        }
        
        if (preferenceValue == null || preferenceValue.isEmpty()) {
            return null;
        }
        
        try {
            // Handle common boolean string representations
            String normalizedValue = preferenceValue.trim().toLowerCase();
            if (normalizedValue.equals("true") || normalizedValue.equals("yes") || 
                normalizedValue.equals("1") || normalizedValue.equals("on")) {
                cachedBooleanValue = Boolean.TRUE;
                return Boolean.TRUE;
            } else if (normalizedValue.equals("false") || normalizedValue.equals("no") || 
                       normalizedValue.equals("0") || normalizedValue.equals("off")) {
                cachedBooleanValue = Boolean.FALSE;
                return Boolean.FALSE;
            }
            
            // For any other value, try direct parsing
            cachedBooleanValue = Boolean.valueOf(preferenceValue);
            return cachedBooleanValue;
        } catch (Exception e) {
            // FIXME: Consider adding proper exception handling or logging here
            return null;
        }
    }

    /**
     * Returns preference value as integer if applicable.
     * Will attempt to parse the string value as an integer.
     * 
     * @return integer representation of the preference value, or null if parsing fails
     */
    public Integer getIntegerValue() {
        // Return cached value if available
        if (cachedIntegerValue != null) {
            return cachedIntegerValue;
        }
        
        // If dataType is not INTEGER, check if we should attempt conversion
        if (dataType != DataType.INTEGER) {
            // TODO: Add logging for data type mismatch
            return null;
        }
        
        if (preferenceValue == null || preferenceValue.isEmpty()) {
            return null;
        }
        
        try {
            // Handle possible decimal values by truncating
            if (preferenceValue.contains(".")) {
                // Extract the integer part before the decimal point
                String integerPart = preferenceValue.substring(0, preferenceValue.indexOf('.'));
                cachedIntegerValue = Integer.parseInt(integerPart);
                return cachedIntegerValue;
            }
            
            // Try direct parsing
            cachedIntegerValue = Integer.parseInt(preferenceValue.trim());
            return cachedIntegerValue;
        } catch (NumberFormatException e) {
            // FIXME: Consider adding proper exception handling or logging here
            return null;
        }
    }
    
    /**
     * Checks if the preference value is present and not empty
     * 
     * @return true if the preference value exists and is not empty
     */
    public boolean hasValue() {
        return preferenceValue != null && !preferenceValue.isEmpty();
    }
    
    /**
     * Updates the preference value with type checking based on the set data type.
     * This method attempts to validate the new value against the specified data type.
     *
     * @param newValue the new preference value
     * @return true if the value was updated successfully, false if validation failed
     */
    public boolean updateValue(String newValue) {
        if (dataType == null) {
            // Cannot validate without a data type
            return false;
        }
        
        // Validate the new value against the data type
        boolean isValid = false;
        
        switch (dataType) {
            case BOOLEAN:
                isValid = validateBoolean(newValue);
                break;
            case INTEGER:
                isValid = validateInteger(newValue);
                break;
            case FLOAT:
                isValid = validateFloat(newValue);
                break;
            case DATE:
                isValid = validateDate(newValue);
                break;
            case JSON:
                isValid = validateJson(newValue);
                break;
            case STRING:
                // Strings are always valid
                isValid = true;
                break;
            default:
                // Unknown data type
                isValid = false;
        }
        
        if (isValid) {
            setPreferenceValue(newValue);
            return true;
        }
        
        return false;
    }
    
    /**
     * Validates if a string can be parsed as a boolean
     */
    private boolean validateBoolean(String value) {
        if (value == null) return false;
        
        String normalizedValue = value.trim().toLowerCase();
        return normalizedValue.equals("true") || normalizedValue.equals("false") ||
               normalizedValue.equals("yes") || normalizedValue.equals("no") ||
               normalizedValue.equals("1") || normalizedValue.equals("0") ||
               normalizedValue.equals("on") || normalizedValue.equals("off");
    }
    
    /**
     * Validates if a string can be parsed as an integer
     */
    private boolean validateInteger(String value) {
        if (value == null) return false;
        
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates if a string can be parsed as a float
     */
    private boolean validateFloat(String value) {
        if (value == null) return false;
        
        try {
            Float.parseFloat(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates if a string can be parsed as a date
     * Currently supports ISO 8601 format and milliseconds since epoch
     */
    private boolean validateDate(String value) {
        if (value == null) return false;
        
        // Check if it's a milliseconds timestamp
        try {
            Long.parseLong(value.trim());
            return true;
        } catch (NumberFormatException e) {
            // Not a milliseconds timestamp
        }
        
        // TODO: Implement proper ISO 8601 date validation
        // For now, just check if it looks like a date format
        return value.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }
    
    /**
     * Validates if a string is valid JSON
     */
    private boolean validateJson(String value) {
        if (value == null) return false;
        
        // Basic JSON validation - check for balanced braces and brackets
        int curlyBraceCount = 0;
        int squareBracketCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : value.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    curlyBraceCount++;
                } else if (c == '}') {
                    curlyBraceCount--;
                } else if (c == '[') {
                    squareBracketCount++;
                } else if (c == ']') {
                    squareBracketCount--;
                }
                
                if (curlyBraceCount < 0 || squareBracketCount < 0) {
                    return false;
                }
            }
        }
        
        return !inString && curlyBraceCount == 0 && squareBracketCount == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreferenceEntity that = (UserPreferenceEntity) o;
        
        // If both entities have IDs, compare by ID
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        
        // Otherwise compare by natural keys
        return Objects.equals(user, that.user) &&
               Objects.equals(preferenceKey, that.preferenceKey) &&
               Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        // If entity has an ID, use it for hashCode
        if (id != null) {
            return Objects.hash(id);
        }
        
        // Otherwise use natural keys
        return Objects.hash(user, preferenceKey, category);
    }

    @Override
    public String toString() {
        return "UserPreferenceEntity{" +
                "id=" + id +
                ", user=" + (user != null ? user.getId() : null) +
                ", preferenceKey='" + preferenceKey + '\'' +
                ", category=" + category +
                ", dataType=" + dataType +
                '}';
    }
}