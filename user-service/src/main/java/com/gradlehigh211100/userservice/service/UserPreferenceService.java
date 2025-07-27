package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.dto.UserPreferenceDto;
import com.gradlehigh211100.userservice.exception.UserPreferenceException;
import com.gradlehigh211100.userservice.model.UserPreference;
import com.gradlehigh211100.userservice.repository.UserPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing user preferences, personalization settings, and configuration with category-based organization
 */
@Service
public class UserPreferenceService {
    
    private static final Logger LOGGER = Logger.getLogger(UserPreferenceService.class.getName());
    private static final String DEFAULT_CATEGORY = "GENERAL";
    private static final int MAX_PREFERENCES_PER_USER = 100;
    private static final int MAX_KEY_LENGTH = 100;
    private static final int MAX_CATEGORY_LENGTH = 50;
    private static final Set<String> VALID_DATA_TYPES = new HashSet<>(Arrays.asList("STRING", "INTEGER", "LONG", "DOUBLE", "BOOLEAN", "DATE"));
    
    // Cache for frequently accessed preferences
    private final Map<String, UserPreferenceDto> preferenceCache = new ConcurrentHashMap<>();
    
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserService userService;
    
    @Autowired
    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository, UserService userService) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userService = userService;
    }
    
    /**
     * Retrieves all preferences for a specific user
     *
     * @param userId ID of the user
     * @return List of user preference DTOs
     * @throws UserPreferenceException if user does not exist or is not active
     */
    @Transactional(readOnly = true)
    public List<UserPreferenceDto> getUserPreferences(Long userId) {
        validateUserId(userId);
        
        try {
            LOGGER.info("Retrieving all preferences for user: " + userId);
            List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
            
            if (preferences.isEmpty()) {
                LOGGER.info("No preferences found for user: " + userId);
                return Collections.emptyList();
            }
            
            // Cache frequently accessed preferences
            List<UserPreferenceDto> dtos = preferences.stream()
                    .map(this::mapToDto)
                    .peek(this::updateCache)
                    .collect(Collectors.toList());
            
            LOGGER.info("Retrieved " + dtos.size() + " preferences for user: " + userId);
            return dtos;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving preferences for user: " + userId, e);
            throw new UserPreferenceException("Failed to retrieve user preferences", e);
        }
    }
    
    /**
     * Retrieves a specific preference by user and key
     *
     * @param userId ID of the user
     * @param preferenceKey the preference key to retrieve
     * @return Optional containing preference DTO if found
     * @throws UserPreferenceException if user does not exist or is not active
     */
    @Transactional(readOnly = true)
    public Optional<UserPreferenceDto> getUserPreferenceByKey(Long userId, String preferenceKey) {
        validateUserId(userId);
        validateKey(preferenceKey);
        
        // Check cache first
        String cacheKey = buildCacheKey(userId, preferenceKey);
        UserPreferenceDto cachedDto = preferenceCache.get(cacheKey);
        if (cachedDto != null) {
            LOGGER.info("Cache hit for user preference: " + userId + ":" + preferenceKey);
            return Optional.of(cachedDto);
        }
        
        try {
            LOGGER.info("Retrieving preference " + preferenceKey + " for user: " + userId);
            Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndPreferenceKey(userId, preferenceKey);
            
            if (!preference.isPresent()) {
                LOGGER.info("Preference " + preferenceKey + " not found for user: " + userId);
                return Optional.empty();
            }
            
            UserPreferenceDto dto = mapToDto(preference.get());
            updateCache(dto);
            return Optional.of(dto);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving preference " + preferenceKey + " for user: " + userId, e);
            throw new UserPreferenceException("Failed to retrieve user preference: " + preferenceKey, e);
        }
    }
    
    /**
     * Sets or updates a user preference
     *
     * @param userId ID of the user
     * @param preferenceDto the preference data to save
     * @return Updated preference DTO
     * @throws UserPreferenceException if user does not exist, is not active, or lacks permission
     */
    @Transactional
    public UserPreferenceDto setUserPreference(Long userId, UserPreferenceDto preferenceDto) {
        validateUserId(userId);
        validateUserCanModify(userId);
        validatePreferenceDto(preferenceDto);
        
        try {
            LOGGER.info("Setting preference " + preferenceDto.getPreferenceKey() + " for user: " + userId);
            
            // Check if user already has too many preferences
            if (!userPreferenceRepository.existsByUserIdAndPreferenceKey(userId, preferenceDto.getPreferenceKey())) {
                long userPreferenceCount = userPreferenceRepository.countByUserIdAndCategory(
                        userId, preferenceDto.getCategory() != null ? preferenceDto.getCategory() : DEFAULT_CATEGORY);
                
                if (userPreferenceCount >= MAX_PREFERENCES_PER_USER) {
                    throw new UserPreferenceException("User has reached maximum allowed preferences: " + MAX_PREFERENCES_PER_USER);
                }
            }
            
            // Find existing or create new
            UserPreference userPreference;
            Optional<UserPreference> existingPreference = userPreferenceRepository.findByUserIdAndPreferenceKey(
                    userId, preferenceDto.getPreferenceKey());
            
            if (existingPreference.isPresent()) {
                userPreference = existingPreference.get();
                // Only update fields that can be changed
                userPreference.setPreferenceValue(preferenceDto.getPreferenceValue());
                if (preferenceDto.getDataType() != null && VALID_DATA_TYPES.contains(preferenceDto.getDataType())) {
                    userPreference.setDataType(preferenceDto.getDataType());
                }
                if (preferenceDto.getCategory() != null) {
                    userPreference.setCategory(validateCategory(preferenceDto.getCategory()));
                }
                userPreference.setEncrypted(preferenceDto.isEncrypted());
            } else {
                userPreference = new UserPreference();
                userPreference.setUserId(userId);
                userPreference.setPreferenceKey(preferenceDto.getPreferenceKey());
                userPreference.setPreferenceValue(preferenceDto.getPreferenceValue());
                userPreference.setDataType(validateDataType(preferenceDto.getDataType()));
                userPreference.setCategory(validateCategory(preferenceDto.getCategory()));
                userPreference.setEncrypted(preferenceDto.isEncrypted());
                userPreference.setLastModified(new Date());
            }
            
            // Apply data type specific validations and transformations
            processDataTypeSpecificRules(userPreference);
            
            // Save the preference
            userPreference = userPreferenceRepository.save(userPreference);
            UserPreferenceDto savedDto = mapToDto(userPreference);
            
            // Update the cache
            updateCache(savedDto);
            
            LOGGER.info("Successfully saved preference " + preferenceDto.getPreferenceKey() + " for user: " + userId);
            return savedDto;
        } catch (UserPreferenceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving preference for user: " + userId, e);
            throw new UserPreferenceException("Failed to save user preference", e);
        }
    }
    
    /**
     * Deletes a specific user preference
     *
     * @param userId ID of the user
     * @param preferenceKey the preference key to delete
     * @throws UserPreferenceException if user does not exist, is not active, or lacks permission
     */
    @Transactional
    public void deleteUserPreference(Long userId, String preferenceKey) {
        validateUserId(userId);
        validateUserCanModify(userId);
        validateKey(preferenceKey);
        
        try {
            LOGGER.info("Deleting preference " + preferenceKey + " for user: " + userId);
            
            // Check if preference exists before deleting
            if (!userPreferenceRepository.existsByUserIdAndPreferenceKey(userId, preferenceKey)) {
                LOGGER.warning("Preference " + preferenceKey + " does not exist for user: " + userId);
                return; // No need to throw exception, already doesn't exist
            }
            
            userPreferenceRepository.deleteByUserIdAndPreferenceKey(userId, preferenceKey);
            
            // Remove from cache
            preferenceCache.remove(buildCacheKey(userId, preferenceKey));
            
            LOGGER.info("Successfully deleted preference " + preferenceKey + " for user: " + userId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting preference " + preferenceKey + " for user: " + userId, e);
            throw new UserPreferenceException("Failed to delete user preference", e);
        }
    }
    
    /**
     * Retrieves preferences by category for a user
     *
     * @param userId ID of the user
     * @param category the preference category
     * @return List of user preference DTOs in the specified category
     * @throws UserPreferenceException if user does not exist or is not active
     */
    @Transactional(readOnly = true)
    public List<UserPreferenceDto> getUserPreferencesByCategory(Long userId, String category) {
        validateUserId(userId);
        validateCategory(category);
        
        try {
            LOGGER.info("Retrieving preferences for user: " + userId + " in category: " + category);
            List<UserPreference> preferences = userPreferenceRepository.findByUserIdAndCategory(userId, category);
            
            if (preferences.isEmpty()) {
                LOGGER.info("No preferences found for user: " + userId + " in category: " + category);
                return Collections.emptyList();
            }
            
            List<UserPreferenceDto> dtos = preferences.stream()
                    .map(this::mapToDto)
                    .peek(this::updateCache)
                    .collect(Collectors.toList());
            
            LOGGER.info("Retrieved " + dtos.size() + " preferences for user: " + userId + " in category: " + category);
            return dtos;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving preferences for user: " + userId + " in category: " + category, e);
            throw new UserPreferenceException("Failed to retrieve user preferences by category", e);
        }
    }
    
    /**
     * Resets all preferences in a category to defaults
     *
     * @param userId ID of the user
     * @param category the preference category to reset
     * @throws UserPreferenceException if user does not exist, is not active, or lacks permission
     */
    @Transactional
    public void resetUserPreferences(Long userId, String category) {
        validateUserId(userId);
        validateUserCanModify(userId);
        validateCategory(category);
        
        try {
            LOGGER.info("Resetting preferences for user: " + userId + " in category: " + category);
            
            // Get all preferences in the category before deleting
            List<UserPreference> preferencesToReset = userPreferenceRepository.findByUserIdAndCategory(userId, category);
            
            // Delete all preferences in the category
            userPreferenceRepository.deleteByUserIdAndCategory(userId, category);
            
            // Remove from cache
            for (UserPreference preference : preferencesToReset) {
                preferenceCache.remove(buildCacheKey(userId, preference.getPreferenceKey()));
            }
            
            // Apply default values based on category
            applyDefaultPreferences(userId, category);
            
            LOGGER.info("Successfully reset preferences for user: " + userId + " in category: " + category);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resetting preferences for user: " + userId + " in category: " + category, e);
            throw new UserPreferenceException("Failed to reset user preferences", e);
        }
    }
    
    // ========================= Helper methods =========================
    
    /**
     * Validates that a user exists and is active
     * 
     * @param userId ID of the user to validate
     * @throws UserPreferenceException if validation fails
     */
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new UserPreferenceException("User ID cannot be null");
        }
        
        if (!userService.validateUser(userId)) {
            throw new UserPreferenceException("User does not exist or is not active: " + userId);
        }
    }
    
    /**
     * Validates that a user can modify preferences
     * 
     * @param userId ID of the user
     * @throws UserPreferenceException if validation fails
     */
    private void validateUserCanModify(Long userId) {
        if (!userService.canModifyPreferences(userId)) {
            throw new UserPreferenceException("User does not have permission to modify preferences: " + userId);
        }
    }
    
    /**
     * Validates a preference key
     * 
     * @param key the preference key to validate
     * @throws UserPreferenceException if validation fails
     */
    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new UserPreferenceException("Preference key cannot be null or empty");
        }
        
        if (key.length() > MAX_KEY_LENGTH) {
            throw new UserPreferenceException("Preference key exceeds maximum length of " + MAX_KEY_LENGTH);
        }
        
        // Check for invalid characters in key
        if (!key.matches("^[a-zA-Z0-9_.\\-]+$")) {
            throw new UserPreferenceException("Preference key contains invalid characters");
        }
    }
    
    /**
     * Validates a preference category
     * 
     * @param category the category to validate
     * @return the validated category or default if null
     * @throws UserPreferenceException if validation fails
     */
    private String validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return DEFAULT_CATEGORY;
        }
        
        if (category.length() > MAX_CATEGORY_LENGTH) {
            throw new UserPreferenceException("Category name exceeds maximum length of " + MAX_CATEGORY_LENGTH);
        }
        
        // Check for invalid characters in category
        if (!category.matches("^[a-zA-Z0-9_.\\-]+$")) {
            throw new UserPreferenceException("Category name contains invalid characters");
        }
        
        return category.toUpperCase();
    }
    
    /**
     * Validates a preference data type
     * 
     * @param dataType the data type to validate
     * @return the validated data type or default if null
     * @throws UserPreferenceException if validation fails
     */
    private String validateDataType(String dataType) {
        if (dataType == null || dataType.trim().isEmpty()) {
            return "STRING";
        }
        
        String normalizedType = dataType.toUpperCase();
        if (!VALID_DATA_TYPES.contains(normalizedType)) {
            throw new UserPreferenceException("Invalid data type: " + dataType);
        }
        
        return normalizedType;
    }
    
    /**
     * Validates a preference DTO
     * 
     * @param preferenceDto the preference DTO to validate
     * @throws UserPreferenceException if validation fails
     */
    private void validatePreferenceDto(UserPreferenceDto preferenceDto) {
        if (preferenceDto == null) {
            throw new UserPreferenceException("Preference DTO cannot be null");
        }
        
        validateKey(preferenceDto.getPreferenceKey());
        
        // Validate value based on data type if present
        if (preferenceDto.getDataType() != null) {
            validateValueAgainstDataType(preferenceDto.getPreferenceValue(), preferenceDto.getDataType());
        }
    }
    
    /**
     * Validates a preference value against its data type
     * 
     * @param value the value to validate
     * @param dataType the data type to validate against
     * @throws UserPreferenceException if validation fails
     */
    private void validateValueAgainstDataType(String value, String dataType) {
        if (value == null) {
            return; // Null values are allowed
        }
        
        try {
            switch (dataType.toUpperCase()) {
                case "INTEGER":
                    Integer.parseInt(value);
                    break;
                case "LONG":
                    Long.parseLong(value);
                    break;
                case "DOUBLE":
                    Double.parseDouble(value);
                    break;
                case "BOOLEAN":
                    // Only accept true/false values
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new UserPreferenceException("Invalid boolean value: " + value);
                    }
                    break;
                case "DATE":
                    try {
                        // Try parsing as a timestamp
                        Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        // FIXME: Add proper date format validation here
                        // For now, just checking it's a valid number as a timestamp
                        throw new UserPreferenceException("Invalid date format: " + value);
                    }
                    break;
                case "STRING":
                default:
                    // No validation needed for strings
                    break;
            }
        } catch (NumberFormatException e) {
            throw new UserPreferenceException("Value does not match data type " + dataType + ": " + value);
        }
    }
    
    /**
     * Process data type specific rules for a preference
     * 
     * @param preference the preference to process
     */
    private void processDataTypeSpecificRules(UserPreference preference) {
        if (preference.getPreferenceValue() == null) {
            return;
        }
        
        switch (preference.getDataType()) {
            case "INTEGER":
                try {
                    int value = Integer.parseInt(preference.getPreferenceValue());
                    // Apply range limits based on category if needed
                    if ("PAGINATION".equals(preference.getCategory()) && value < 1) {
                        preference.setPreferenceValue("1");
                    } else if ("TIMEOUT".equals(preference.getCategory()) && value < 0) {
                        preference.setPreferenceValue("0");
                    }
                } catch (NumberFormatException e) {
                    // Fall back to default for the category
                    preference.setPreferenceValue(getDefaultValueForCategory(preference.getCategory(), "INTEGER"));
                }
                break;
            case "BOOLEAN":
                // Normalize boolean values
                if (preference.getPreferenceValue().equalsIgnoreCase("true") || 
                        preference.getPreferenceValue().equals("1") || 
                        preference.getPreferenceValue().equalsIgnoreCase("yes") ||
                        preference.getPreferenceValue().equalsIgnoreCase("y")) {
                    preference.setPreferenceValue("true");
                } else {
                    preference.setPreferenceValue("false");
                }
                break;
            case "STRING":
                // For security, strip potentially dangerous content
                if (preference.getPreferenceValue().contains("<script") || 
                        preference.getPreferenceValue().contains("javascript:")) {
                    preference.setPreferenceValue(preference.getPreferenceValue()
                            .replaceAll("<script.*?>.*?</script>", "")
                            .replaceAll("javascript:", ""));
                }
                break;
            // Add other data type specific rules as needed
        }
    }
    
    /**
     * Maps a preference entity to a DTO
     * 
     * @param preference the entity to map
     * @return the mapped DTO
     */
    private UserPreferenceDto mapToDto(UserPreference preference) {
        UserPreferenceDto dto = new UserPreferenceDto();
        dto.setId(preference.getId());
        dto.setUserId(preference.getUserId());
        dto.setPreferenceKey(preference.getPreferenceKey());
        dto.setPreferenceValue(preference.getPreferenceValue());
        dto.setCategory(preference.getCategory());
        dto.setDataType(preference.getDataType());
        dto.setLastModified(preference.getLastModified());
        dto.setEncrypted(preference.isEncrypted());
        return dto;
    }
    
    /**
     * Updates the cache with a preference DTO
     * 
     * @param dto the DTO to cache
     */
    private void updateCache(UserPreferenceDto dto) {
        String cacheKey = buildCacheKey(dto.getUserId(), dto.getPreferenceKey());
        preferenceCache.put(cacheKey, dto);
    }
    
    /**
     * Builds a cache key from user ID and preference key
     * 
     * @param userId the user ID
     * @param preferenceKey the preference key
     * @return the cache key
     */
    private String buildCacheKey(Long userId, String preferenceKey) {
        return userId + ":" + preferenceKey;
    }
    
    /**
     * Applies default preferences based on category
     * 
     * @param userId the user ID
     * @param category the category
     */
    private void applyDefaultPreferences(Long userId, String category) {
        Map<String, String> defaults = getDefaultPreferences(category);
        
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setPreferenceKey(entry.getKey());
            preference.setPreferenceValue(entry.getValue());
            preference.setCategory(category);
            preference.setDataType(guessDataType(entry.getValue()));
            preference.setEncrypted(false);
            preference.setLastModified(new Date());
            
            userPreferenceRepository.save(preference);
            
            // Update cache
            updateCache(mapToDto(preference));
        }
    }
    
    /**
     * Gets default preferences for a category
     * 
     * @param category the category
     * @return map of default preferences
     */
    private Map<String, String> getDefaultPreferences(String category) {
        Map<String, String> defaults = new HashMap<>();
        
        switch (category) {
            case "UI":
                defaults.put("theme", "light");
                defaults.put("fontSize", "14");
                defaults.put("showNotifications", "true");
                break;
            case "NOTIFICATIONS":
                defaults.put("emailNotifications", "true");
                defaults.put("pushNotifications", "false");
                defaults.put("smsNotifications", "false");
                break;
            case "PAGINATION":
                defaults.put("itemsPerPage", "10");
                defaults.put("showPagination", "true");
                break;
            case "PRIVACY":
                defaults.put("shareData", "false");
                defaults.put("trackActivity", "true");
                break;
            case "GENERAL":
            default:
                defaults.put("language", "en");
                defaults.put("timezone", "UTC");
                break;
        }
        
        return defaults;
    }
    
    /**
     * Gets the default value for a category and data type
     * 
     * @param category the category
     * @param dataType the data type
     * @return the default value
     */
    private String getDefaultValueForCategory(String category, String dataType) {
        Map<String, Function<String, String>> defaultValueProviders = new HashMap<>();
        
        defaultValueProviders.put("INTEGER", cat -> {
            switch (cat) {
                case "PAGINATION": return "10";
                case "TIMEOUT": return "30";
                default: return "0";
            }
        });
        
        defaultValueProviders.put("BOOLEAN", cat -> "false");
        defaultValueProviders.put("STRING", cat -> "");
        defaultValueProviders.put("DOUBLE", cat -> "0.0");
        defaultValueProviders.put("LONG", cat -> "0");
        defaultValueProviders.put("DATE", cat -> String.valueOf(new Date().getTime()));
        
        Function<String, String> provider = defaultValueProviders.getOrDefault(dataType, c -> "");
        return provider.apply(category);
    }
    
    /**
     * Guesses the data type from a value
     * 
     * @param value the value
     * @return the guessed data type
     */
    private String guessDataType(String value) {
        if (value == null) {
            return "STRING";
        }
        
        value = value.trim();
        
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "BOOLEAN";
        }
        
        try {
            Integer.parseInt(value);
            return "INTEGER";
        } catch (NumberFormatException e) {
            // Not an integer, continue
        }
        
        try {
            Double.parseDouble(value);
            return "DOUBLE";
        } catch (NumberFormatException e) {
            // Not a double, continue
        }
        
        // Default to string for everything else
        return "STRING";
    }
}