package com.gradlehigh211100.userservice.service;

import java.util.Optional;

/**
 * Service interface for user management
 */
public interface UserService {
    
    /**
     * Validates if a user exists and is active
     *
     * @param userId ID of the user to validate
     * @return true if the user exists and is active, false otherwise
     */
    boolean validateUser(Long userId);
    
    /**
     * Checks if a user has access to modify preferences
     *
     * @param userId ID of the user
     * @return true if the user has access, false otherwise
     */
    boolean canModifyPreferences(Long userId);
    
    /**
     * Gets the default user preference category
     *
     * @param userId ID of the user
     * @return the default category for the user
     */
    String getDefaultPreferenceCategory(Long userId);
}