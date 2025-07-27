package com.gradlehigh211100.userservice.controller;

import com.gradlehigh211100.userservice.service.UserService;
import com.gradlehigh211100.userservice.service.UserPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST controller providing comprehensive user management endpoints with full CRUD operations,
 * profile management, and role administration.
 * 
 * This controller handles all HTTP requests related to user management including:
 * - User creation, retrieval, update and deletion
 * - User preference management
 * - User role management
 *
 * @author GradleHigh211100 System
 * @version 1.0
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());
    
    private final UserService userService;
    private final UserPreferenceService userPreferenceService;

    /**
     * Constructor for dependency injection.
     *
     * @param userService The service handling user business operations
     * @param userPreferenceService The service handling user preferences
     */
    @Autowired
    public UserController(UserService userService, UserPreferenceService userPreferenceService) {
        this.userService = userService;
        this.userPreferenceService = userPreferenceService;
    }

    /**
     * Creates a new user account via REST API.
     *
     * @param userRegistrationDto The user registration data transfer object
     * @return ResponseEntity containing the created user details
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserRegistrationDto userRegistrationDto) {
        LOGGER.info("Creating new user: " + userRegistrationDto.getEmail());
        
        try {
            // Validate complex business rules that aren't covered by simple validation annotations
            if (userRegistrationDto.getPassword().length() < 8) {
                return ResponseEntity.badRequest().build();
            }
            
            // Check if email already exists
            if (userService.existsByEmail(userRegistrationDto.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            // Create the user
            UserDto createdUser = userService.createUser(userRegistrationDto);
            
            // If user preferences are provided during registration, create them as well
            if (userRegistrationDto.getPreferences() != null && !userRegistrationDto.getPreferences().isEmpty()) {
                userRegistrationDto.getPreferences().forEach(preference -> {
                    userPreferenceService.createUserPreference(createdUser.getId(), preference);
                });
            }
            
            // Return the created user
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            LOGGER.severe("Error creating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves user by ID via REST API.
     *
     * @param id The user ID
     * @return ResponseEntity containing the user details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        LOGGER.info("Fetching user with ID: " + id);
        
        try {
            Optional<UserDto> userOpt = userService.findById(id);
            
            // Add detailed error handling for various scenarios
            if (!userOpt.isPresent()) {
                LOGGER.warning("User with ID " + id + " not found");
                return ResponseEntity.notFound().build();
            }
            
            // Check authorization (users can see their own profiles, admins can see all)
            if (!isAuthorizedToView(id)) {
                LOGGER.warning("Unauthorized access attempt for user ID: " + id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(userOpt.get());
        } catch (Exception e) {
            LOGGER.severe("Error fetching user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Updates user information via REST API.
     *
     * @param id The user ID
     * @param userDto The user data transfer object with updated information
     * @return ResponseEntity containing the updated user details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        LOGGER.info("Updating user with ID: " + id);
        
        try {
            // Validate the ID in the path matches the ID in the body
            if (!id.equals(userDto.getId())) {
                LOGGER.warning("Path ID doesn't match body ID for update request");
                return ResponseEntity.badRequest().build();
            }
            
            // Check if user exists
            if (!userService.existsById(id)) {
                LOGGER.warning("Attempted to update non-existent user: " + id);
                return ResponseEntity.notFound().build();
            }
            
            // Check authorization (users can update their own profiles, admins can update all)
            if (!isAuthorizedToModify(id)) {
                LOGGER.warning("Unauthorized update attempt for user ID: " + id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Process specific business logic for updates
            if (userDto.getEmail() != null && userService.existsByEmailAndIdNot(userDto.getEmail(), id)) {
                LOGGER.warning("Email already in use: " + userDto.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            // Update the user
            UserDto updatedUser = userService.updateUser(id, userDto);
            
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            LOGGER.severe("Error updating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deletes user account via REST API.
     *
     * @param id The user ID
     * @return ResponseEntity with no content on successful deletion
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        LOGGER.info("Deleting user with ID: " + id);
        
        try {
            // Check if user exists
            if (!userService.existsById(id)) {
                LOGGER.warning("Attempted to delete non-existent user: " + id);
                return ResponseEntity.notFound().build();
            }
            
            // Complex business logic - check if user has any pending operations
            if (userService.hasActiveTransactions(id)) {
                LOGGER.warning("Cannot delete user " + id + " with active transactions");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Error-Reason", "User has active transactions")
                    .build();
            }
            
            // Soft delete or hard delete based on system policy
            boolean hardDelete = shouldPerformHardDelete(id);
            
            if (hardDelete) {
                // Remove all associated data first
                userPreferenceService.deleteAllByUserId(id);
                // Perform additional cleanup for other entities
                
                // Finally delete the user
                userService.deleteUser(id);
            } else {
                // Perform soft delete
                userService.deactivateUser(id);
            }
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.severe("Error deleting user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves user preferences via REST API.
     *
     * @param id The user ID
     * @return ResponseEntity containing list of user preferences
     */
    @GetMapping("/{id}/preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<UserPreferenceDto>> getUserPreferences(@PathVariable Long id) {
        LOGGER.info("Fetching preferences for user with ID: " + id);
        
        try {
            // Check if user exists
            if (!userService.existsById(id)) {
                LOGGER.warning("User with ID " + id + " not found when fetching preferences");
                return ResponseEntity.notFound().build();
            }
            
            // Check authorization
            if (!isAuthorizedToView(id)) {
                LOGGER.warning("Unauthorized access attempt for user preferences, ID: " + id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Different handling for different user types or statuses
            List<UserPreferenceDto> preferences = userPreferenceService.getUserPreferences(id);
            
            // Process preferences based on user subscription level
            String userType = userService.getUserType(id);
            if ("FREE".equals(userType)) {
                // Filter out premium preferences for free users
                preferences = preferences.stream()
                    .filter(p -> !p.isPremiumOnly())
                    .collect(Collectors.toList());
            }
            
            if (preferences.isEmpty()) {
                LOGGER.info("No preferences found for user: " + id);
                return ResponseEntity.noContent().build();
            }
            
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            LOGGER.severe("Error fetching user preferences: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Updates user preference via REST API.
     *
     * @param id The user ID
     * @param preferenceDto The preference data transfer object
     * @return ResponseEntity containing updated preference
     */
    @PutMapping("/{id}/preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserPreferenceDto> updateUserPreference(
            @PathVariable Long id, 
            @Valid @RequestBody UserPreferenceDto preferenceDto) {
        LOGGER.info("Updating preference for user ID: " + id);
        
        try {
            // Check if user exists
            if (!userService.existsById(id)) {
                LOGGER.warning("User with ID " + id + " not found when updating preference");
                return ResponseEntity.notFound().build();
            }
            
            // Check authorization
            if (!isAuthorizedToModify(id)) {
                LOGGER.warning("Unauthorized modification attempt for user preference, ID: " + id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Check if the preference exists
            if (preferenceDto.getId() != null && !userPreferenceService.preferenceExists(preferenceDto.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            // Validate preference based on user subscription
            String userType = userService.getUserType(id);
            if ("FREE".equals(userType) && preferenceDto.isPremiumOnly()) {
                LOGGER.warning("Free user attempted to set premium preference: User ID " + id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-Error-Reason", "Premium feature not available")
                    .build();
            }
            
            // Determine whether to create or update
            UserPreferenceDto result;
            if (preferenceDto.getId() == null) {
                // Creating new preference
                preferenceDto.setUserId(id);
                result = userPreferenceService.createUserPreference(id, preferenceDto);
            } else {
                // Updating existing preference
                result = userPreferenceService.updateUserPreference(id, preferenceDto);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.severe("Error updating user preference: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assigns role to user via REST API.
     *
     * @param id The user ID
     * @param roleName The role to assign
     * @return ResponseEntity with no content on successful role assignment
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRole(@PathVariable Long id, @RequestParam String roleName) {
        LOGGER.info("Assigning role " + roleName + " to user ID: " + id);
        
        try {
            // Check if user exists
            if (!userService.existsById(id)) {
                LOGGER.warning("User with ID " + id + " not found when assigning role");
                return ResponseEntity.notFound().build();
            }
            
            // Validate role name
            List<String> validRoles = userService.getAvailableRoles();
            if (!validRoles.contains(roleName)) {
                LOGGER.warning("Invalid role name provided: " + roleName);
                return ResponseEntity.badRequest().build();
            }
            
            // Check for special constraints
            if ("SUPER_ADMIN".equals(roleName) && !currentUserIsSuperAdmin()) {
                LOGGER.warning("Unauthorized attempt to assign SUPER_ADMIN role");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Check if user already has the role
            if (userService.userHasRole(id, roleName)) {
                LOGGER.info("User already has role " + roleName);
                return ResponseEntity.noContent().build();
            }
            
            // Assign the role
            userService.assignRoleToUser(id, roleName);
            
            // Log the role assignment for auditing
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("action", "ROLE_ASSIGNED");
            auditData.put("userId", id);
            auditData.put("role", roleName);
            auditData.put("assignedBy", getCurrentUserId());
            auditData.put("timestamp", System.currentTimeMillis());
            logAuditEvent(auditData);
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.severe("Error assigning role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Helper method to check if current user is authorized to view a user profile.
     * 
     * @param userId User ID to check
     * @return True if authorized
     */
    private boolean isAuthorizedToView(Long userId) {
        // FIXME: Replace with actual security implementation
        // Complexity: Check if current user is the user being viewed or has admin permissions
        try {
            Long currentUserId = getCurrentUserId();
            boolean isAdmin = hasAdminRole();
            
            return userId.equals(currentUserId) || isAdmin;
        } catch (Exception e) {
            LOGGER.severe("Error in authorization check: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to check if current user is authorized to modify a user.
     * 
     * @param userId User ID to check
     * @return True if authorized
     */
    private boolean isAuthorizedToModify(Long userId) {
        // Similar to isAuthorizedToView but with potentially stricter rules
        // TODO: Implement proper authorization logic
        return isAuthorizedToView(userId);
    }
    
    /**
     * Helper method to determine if hard delete should be performed.
     * 
     * @param userId User ID to check
     * @return True if hard delete should be performed
     */
    private boolean shouldPerformHardDelete(Long userId) {
        // Complex business logic to determine if hard delete is appropriate
        try {
            // Check if user has been inactive for a long time
            boolean longTermInactive = userService.isInactiveLongerThan(userId, 365);
            
            // Check if user has requested data removal (GDPR)
            boolean dataRemovalRequested = userService.hasRequestedDataRemoval(userId);
            
            // Check if user is test/temporary account
            boolean isTestAccount = userService.isTestAccount(userId);
            
            return longTermInactive || dataRemovalRequested || isTestAccount;
        } catch (Exception e) {
            LOGGER.warning("Error determining delete type: " + e.getMessage());
            // Default to soft delete as safer option
            return false;
        }
    }
    
    /**
     * Gets the current user ID from security context.
     * 
     * @return Current user ID
     */
    private Long getCurrentUserId() {
        // FIXME: Implement with Spring Security
        // This is a placeholder implementation
        return 1L;  // Default admin ID for development
    }
    
    /**
     * Checks if current user has admin role.
     * 
     * @return True if user has admin role
     */
    private boolean hasAdminRole() {
        // FIXME: Implement with Spring Security
        // This is a placeholder implementation
        return true;  // For development purposes
    }
    
    /**
     * Checks if current user is a super admin.
     * 
     * @return True if user is super admin
     */
    private boolean currentUserIsSuperAdmin() {
        // FIXME: Implement with Spring Security
        // This is a placeholder implementation
        return false;  // Generally restrictive default
    }
    
    /**
     * Logs an audit event for security tracking.
     * 
     * @param auditData Map containing audit information
     */
    private void logAuditEvent(Map<String, Object> auditData) {
        // TODO: Replace with actual audit logging implementation
        LOGGER.info("AUDIT: " + auditData);
    }
}