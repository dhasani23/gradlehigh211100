package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.dto.UserPreferenceDto;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for user preference management operations.
 */
public interface UserPreferenceService {

    /**
     * Gets all preferences for a user.
     *
     * @param userId The user ID
     * @return List of user preferences
     */
    List<UserPreferenceDto> getUserPreferences(Long userId);
    
    /**
     * Creates a new user preference.
     *
     * @param userId The user ID
     * @param preferenceDto The preference data
     * @return The created preference DTO
     */
    UserPreferenceDto createUserPreference(Long userId, UserPreferenceDto preferenceDto);
    
    /**
     * Updates an existing user preference.
     *
     * @param userId The user ID
     * @param preferenceDto The updated preference data
     * @return The updated preference DTO
     */
    UserPreferenceDto updateUserPreference(Long userId, UserPreferenceDto preferenceDto);
    
    /**
     * Deletes a user preference.
     *
     * @param userId The user ID
     * @param preferenceId The preference ID
     */
    void deleteUserPreference(Long userId, Long preferenceId);
    
    /**
     * Deletes all preferences for a user.
     *
     * @param userId The user ID
     */
    void deleteAllByUserId(Long userId);
    
    /**
     * Checks if a preference exists.
     *
     * @param preferenceId The preference ID
     * @return True if preference exists
     */
    boolean preferenceExists(Long preferenceId);
    
    /**
     * Finds a preference by ID.
     *
     * @param preferenceId The preference ID
     * @return Optional containing the preference if found
     */
    Optional<UserPreferenceDto> findById(Long preferenceId);
}