package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.repository.UserRepository;
import com.gradlehigh211100.userservice.repository.RoleRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.HashSet;
import java.time.LocalDateTime;

/**
 * Core business service for comprehensive user management including CRUD operations,
 * profile management, role assignment, and account lifecycle.
 * 
 * This service handles all user-related operations with appropriate validations,
 * security measures, and audit trail management.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserAuditService userAuditService;

    /**
     * Constructor for dependency injection.
     * 
     * @param userRepository Repository for user data access
     * @param roleRepository Repository for role data access
     * @param passwordEncoder Password encoder for secure password hashing
     * @param userAuditService Service for audit trail management
     */
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       UserAuditService userAuditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuditService = userAuditService;
    }

    /**
     * Creates a new user account with validation and default role assignment.
     * 
     * @param userRegistrationDto User registration data
     * @return Created user entity
     * @throws IllegalArgumentException if validation fails
     */
    public UserEntity createUser(UserRegistrationDto userRegistrationDto) {
        // Validate input data
        validateUserRegistrationData(userRegistrationDto);
        
        // Check if username already exists
        if (userRepository.findByUsername(userRegistrationDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + userRegistrationDto.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(userRegistrationDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + userRegistrationDto.getEmail());
        }
        
        // Create new user entity
        UserEntity user = new UserEntity();
        user.setUsername(userRegistrationDto.getUsername());
        user.setEmail(userRegistrationDto.getEmail());
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setLastName(userRegistrationDto.getLastName());
        user.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
        user.setCreatedAt(new Date());
        user.setActive(true);
        user.setLocked(false);
        
        // Assign default role (usually USER)
        RoleEntity defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default role not found"));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);
        
        // Save user to database
        UserEntity savedUser = userRepository.save(user);
        
        // Log user creation in audit trail
        userAuditService.logUserCreation(savedUser.getId(), userRegistrationDto.getUsername());
        
        return savedUser;
    }

    /**
     * Retrieves a user by their ID.
     * 
     * @param id User ID
     * @return Optional containing user if found, empty otherwise
     */
    public Optional<UserEntity> getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<UserEntity> user = userRepository.findById(id);
        
        // Log access attempt for audit purposes
        if (user.isPresent()) {
            userAuditService.logUserAccess(id, "RETRIEVE_BY_ID");
        } else {
            userAuditService.logFailedUserAccess(id, "RETRIEVE_BY_ID", "User not found");
        }
        
        return user;
    }

    /**
     * Retrieves a user by their username.
     * 
     * @param username Username to search for
     * @return Optional containing user if found, empty otherwise
     */
    public Optional<UserEntity> getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        Optional<UserEntity> user = userRepository.findByUsername(username);
        
        // Log access attempt for audit purposes
        if (user.isPresent()) {
            userAuditService.logUserAccess(user.get().getId(), "RETRIEVE_BY_USERNAME");
        } else {
            userAuditService.logFailedUserAccess(null, "RETRIEVE_BY_USERNAME", 
                "User not found with username: " + username);
        }
        
        return user;
    }

    /**
     * Updates user information with validation and audit logging.
     * 
     * @param id User ID to update
     * @param userDto Updated user information
     * @return Updated user entity
     * @throws IllegalArgumentException if validation fails
     * @throws javax.persistence.EntityNotFoundException if user not found
     */
    public UserEntity updateUser(Long id, UserDto userDto) {
        // Validate user exists
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + id));
        
        // Track changes for audit
        StringBuilder changes = new StringBuilder();
        
        // Update fields if provided
        if (userDto.getFirstName() != null && !userDto.getFirstName().equals(existingUser.getFirstName())) {
            changes.append("First name changed from '")
                   .append(existingUser.getFirstName())
                   .append("' to '")
                   .append(userDto.getFirstName())
                   .append("'. ");
            existingUser.setFirstName(userDto.getFirstName());
        }
        
        if (userDto.getLastName() != null && !userDto.getLastName().equals(existingUser.getLastName())) {
            changes.append("Last name changed from '")
                   .append(existingUser.getLastName())
                   .append("' to '")
                   .append(userDto.getLastName())
                   .append("'. ");
            existingUser.setLastName(userDto.getLastName());
        }
        
        if (userDto.getEmail() != null && !userDto.getEmail().equals(existingUser.getEmail())) {
            // Check if new email is already in use
            userRepository.findByEmail(userDto.getEmail()).ifPresent(user -> {
                if (!user.getId().equals(id)) {
                    throw new IllegalArgumentException("Email already in use: " + userDto.getEmail());
                }
            });
            
            changes.append("Email changed from '")
                   .append(existingUser.getEmail())
                   .append("' to '")
                   .append(userDto.getEmail())
                   .append("'. ");
            existingUser.setEmail(userDto.getEmail());
        }
        
        if (userDto.getPhoneNumber() != null && !userDto.getPhoneNumber().equals(existingUser.getPhoneNumber())) {
            changes.append("Phone number changed from '")
                   .append(existingUser.getPhoneNumber())
                   .append("' to '")
                   .append(userDto.getPhoneNumber())
                   .append("'. ");
            existingUser.setPhoneNumber(userDto.getPhoneNumber());
        }
        
        // Update optional fields if they exist in the DTO
        if (userDto.getAddress() != null) {
            changes.append("Address updated. ");
            existingUser.setAddress(userDto.getAddress());
        }
        
        if (userDto.getPreferences() != null) {
            changes.append("Preferences updated. ");
            existingUser.setPreferences(userDto.getPreferences());
        }
        
        existingUser.setUpdatedAt(new Date());
        
        // Save updated user
        UserEntity updatedUser = userRepository.save(existingUser);
        
        // Log the changes for audit purposes
        if (changes.length() > 0) {
            userAuditService.logUserUpdate(id, changes.toString());
        }
        
        return updatedUser;
    }

    /**
     * Soft deletes a user account by marking it as inactive.
     * 
     * @param id User ID to delete
     * @throws javax.persistence.EntityNotFoundException if user not found
     */
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + id));
        
        // Perform soft delete by marking user as inactive
        user.setActive(false);
        user.setDeletedAt(new Date());
        userRepository.save(user);
        
        // Log user deletion in audit trail
        userAuditService.logUserDeletion(id, user.getUsername());
        
        // FIXME: Consider implementing a cleanup job for completely removing inactive users after a certain period
    }

    /**
     * Assigns a role to a user.
     * 
     * @param userId User ID
     * @param roleName Role name to assign
     * @throws javax.persistence.EntityNotFoundException if user or role not found
     */
    public void assignRole(Long userId, String roleName) {
        // Validate input
        if (userId == null || roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and role name must not be null or empty");
        }
        
        // Get user and role
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + userId));
        
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("Role not found: " + roleName));
        
        // Check if user already has this role
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals(roleName))) {
            // User already has this role, no action needed
            return;
        }
        
        // Add role to user
        user.getRoles().add(role);
        userRepository.save(user);
        
        // Log role assignment
        userAuditService.logRoleAssignment(userId, roleName);
        
        // TODO: Consider sending notification to user about role change
    }

    /**
     * Removes a role from a user.
     * 
     * @param userId User ID
     * @param roleName Role name to remove
     * @throws javax.persistence.EntityNotFoundException if user not found
     * @throws IllegalStateException if trying to remove the last role
     */
    public void removeRole(Long userId, String roleName) {
        // Validate input
        if (userId == null || roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and role name must not be null or empty");
        }
        
        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + userId));
        
        // Find the role to remove
        Optional<RoleEntity> roleToRemove = user.getRoles().stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst();
        
        if (!roleToRemove.isPresent()) {
            // User doesn't have this role, no action needed
            return;
        }
        
        // Check if this is the last role
        if (user.getRoles().size() <= 1) {
            throw new IllegalStateException("Cannot remove the last role from user. Users must have at least one role.");
        }
        
        // Remove the role
        user.getRoles().remove(roleToRemove.get());
        userRepository.save(user);
        
        // Log role removal
        userAuditService.logRoleRemoval(userId, roleName);
    }

    /**
     * Changes user password with old password verification.
     * 
     * @param userId User ID
     * @param oldPassword Current password for verification
     * @param newPassword New password to set
     * @throws javax.persistence.EntityNotFoundException if user not found
     * @throws SecurityException if old password verification fails
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // Validate input
        if (userId == null || oldPassword == null || newPassword == null) {
            throw new IllegalArgumentException("User ID, old password, and new password must not be null");
        }
        
        // Password strength validation
        validatePasswordStrength(newPassword);
        
        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + userId));
        
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            // Log failed password change attempt
            userAuditService.logFailedPasswordChange(userId, "Invalid old password");
            throw new SecurityException("Old password verification failed");
        }
        
        // Check that new password is different from old
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(new Date());
        userRepository.save(user);
        
        // Log password change
        userAuditService.logPasswordChange(userId);
        
        // TODO: Consider notifying user via email about password change
    }

    /**
     * Locks a user account for security reasons.
     * 
     * @param userId User ID to lock
     * @param reason Reason for locking the account
     * @throws javax.persistence.EntityNotFoundException if user not found
     */
    public void lockAccount(Long userId, String reason) {
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + userId));
        
        // Check if already locked
        if (user.isLocked()) {
            return; // Already locked, no action needed
        }
        
        // Lock the account
        user.setLocked(true);
        user.setLockedReason(reason);
        user.setLockedAt(new Date());
        userRepository.save(user);
        
        // Log account lock
        userAuditService.logAccountLock(userId, reason);
        
        // TODO: Consider notifying user via email about account lock
    }

    /**
     * Unlocks a previously locked user account.
     * 
     * @param userId User ID to unlock
     * @throws javax.persistence.EntityNotFoundException if user not found
     */
    public void unlockAccount(Long userId) {
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException("User not found with ID: " + userId));
        
        // Check if already unlocked
        if (!user.isLocked()) {
            return; // Already unlocked, no action needed
        }
        
        // Unlock the account
        user.setLocked(false);
        user.setLockedReason(null);
        user.setLockedAt(null);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
        
        // Log account unlock
        userAuditService.logAccountUnlock(userId);
        
        // TODO: Consider notifying user via email about account unlock
    }
    
    /**
     * Validates user registration data.
     * 
     * @param userRegistrationDto User registration data to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUserRegistrationData(UserRegistrationDto userRegistrationDto) {
        if (userRegistrationDto == null) {
            throw new IllegalArgumentException("Registration data cannot be null");
        }
        
        // Validate username
        if (userRegistrationDto.getUsername() == null || userRegistrationDto.getUsername().trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        
        // Validate email format
        if (userRegistrationDto.getEmail() == null || !isValidEmail(userRegistrationDto.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Validate password strength
        validatePasswordStrength(userRegistrationDto.getPassword());
        
        // Additional validation logic as needed
        if (userRegistrationDto.getFirstName() == null || userRegistrationDto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        if (userRegistrationDto.getLastName() == null || userRegistrationDto.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }
    
    /**
     * Validates password strength against security requirements.
     * 
     * @param password Password to validate
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecialChar = true;
        }
        
        StringBuilder errorMsg = new StringBuilder("Password must contain");
        boolean hasError = false;
        
        if (!hasUppercase) {
            errorMsg.append(" at least one uppercase letter,");
            hasError = true;
        }
        
        if (!hasLowercase) {
            errorMsg.append(" at least one lowercase letter,");
            hasError = true;
        }
        
        if (!hasDigit) {
            errorMsg.append(" at least one digit,");
            hasError = true;
        }
        
        if (!hasSpecialChar) {
            errorMsg.append(" at least one special character,");
            hasError = true;
        }
        
        if (hasError) {
            errorMsg.deleteCharAt(errorMsg.length() - 1); // Remove the last comma
            throw new IllegalArgumentException(errorMsg.toString());
        }
    }
    
    /**
     * Validates email format using basic pattern matching.
     * 
     * @param email Email to validate
     * @return true if email format is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Very basic email validation - in a real application, consider using a robust library
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
}