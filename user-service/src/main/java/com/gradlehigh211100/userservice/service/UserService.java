package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.dto.UserDto;
import com.gradlehigh211100.userservice.dto.UserRegistrationDto;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for user management operations.
 * Provides methods for CRUD operations and user role management.
 */
public interface UserService {

    /**
     * Creates a new user from registration data.
     *
     * @param registrationDto The registration data
     * @return The created user DTO
     */
    UserDto createUser(UserRegistrationDto registrationDto);
    
    /**
     * Finds a user by ID.
     *
     * @param id The user ID
     * @return Optional containing the user if found
     */
    Optional<UserDto> findById(Long id);
    
    /**
     * Updates user information.
     *
     * @param id The user ID
     * @param userDto The updated user data
     * @return The updated user DTO
     */
    UserDto updateUser(Long id, UserDto userDto);
    
    /**
     * Deletes a user permanently.
     *
     * @param id The user ID
     */
    void deleteUser(Long id);
    
    /**
     * Deactivates a user (soft delete).
     *
     * @param id The user ID
     */
    void deactivateUser(Long id);
    
    /**
     * Checks if a user with the given ID exists.
     *
     * @param id The user ID
     * @return True if user exists
     */
    boolean existsById(Long id);
    
    /**
     * Checks if a user with the given email exists.
     *
     * @param email The email address
     * @return True if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Checks if a user with the given email exists and has a different ID.
     *
     * @param email The email address
     * @param id The user ID to exclude
     * @return True if email exists for a different user
     */
    boolean existsByEmailAndIdNot(String email, Long id);
    
    /**
     * Gets the type of user (e.g., FREE, PREMIUM).
     *
     * @param id The user ID
     * @return The user type
     */
    String getUserType(Long id);
    
    /**
     * Checks if user has active transactions.
     *
     * @param id The user ID
     * @return True if user has active transactions
     */
    boolean hasActiveTransactions(Long id);
    
    /**
     * Checks if user has been inactive longer than specified days.
     *
     * @param id The user ID
     * @param days The number of days
     * @return True if user has been inactive longer than specified days
     */
    boolean isInactiveLongerThan(Long id, int days);
    
    /**
     * Checks if user has requested data removal (GDPR).
     *
     * @param id The user ID
     * @return True if user has requested data removal
     */
    boolean hasRequestedDataRemoval(Long id);
    
    /**
     * Checks if user is a test account.
     *
     * @param id The user ID
     * @return True if user is a test account
     */
    boolean isTestAccount(Long id);
    
    /**
     * Gets a list of all available roles in the system.
     *
     * @return List of role names
     */
    List<String> getAvailableRoles();
    
    /**
     * Checks if user has a specific role.
     *
     * @param id The user ID
     * @param roleName The role name
     * @return True if user has the role
     */
    boolean userHasRole(Long id, String roleName);
    
    /**
     * Assigns a role to a user.
     *
     * @param id The user ID
     * @param roleName The role name
     */
    void assignRoleToUser(Long id, String roleName);
}