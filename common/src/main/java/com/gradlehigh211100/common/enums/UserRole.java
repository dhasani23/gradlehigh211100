package com.gradlehigh211100.common.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumeration defining user roles and permissions levels in the system.
 * 
 * This enum implements a hierarchical permission system where higher-level roles
 * inherit all permissions from lower-level roles. Each role is associated with a
 * specific numeric permission level to facilitate comparison operations.
 * 
 * The permission system follows these principles:
 * - Higher permission level means more privileges
 * - A role can perform actions available to all roles with lower permission levels
 * - Role-specific permissions are implemented using specialized methods
 */
public enum UserRole {
    /**
     * Guest role for unauthenticated users.
     * Has minimal system access with read-only capabilities.
     */
    GUEST(0, "Guest"),
    
    /**
     * Customer role for regular authenticated users.
     * Has standard user privileges for personal account management.
     */
    CUSTOMER(10, "Customer"),
    
    /**
     * Merchant role for product sellers.
     * Has elevated privileges for product and inventory management.
     */
    MERCHANT(20, "Merchant"),
    
    /**
     * Moderator role with limited administrative privileges.
     * Can moderate user content and handle basic administrative tasks.
     */
    MODERATOR(30, "Moderator"),
    
    /**
     * Administrator role with full system access.
     * Has unrestricted access to all system functions and configurations.
     */
    ADMIN(100, "Administrator");
    
    // Permission level for role comparison
    private final int permissionLevel;
    
    // Human-readable display name
    private final String displayName;
    
    // Static lookup maps for improved performance in complex applications
    private static final Map<Integer, UserRole> LEVEL_LOOKUP = new HashMap<>();
    private static final Map<String, UserRole> NAME_LOOKUP = new HashMap<>();
    
    // Initialize lookup maps
    static {
        for (UserRole role : UserRole.values()) {
            LEVEL_LOOKUP.put(role.permissionLevel, role);
            NAME_LOOKUP.put(role.displayName.toUpperCase(), role);
        }
    }
    
    /**
     * Constructor for UserRole enum.
     * 
     * @param permissionLevel numeric level representing role's privileges
     * @param displayName human-readable name for the role
     */
    UserRole(int permissionLevel, String displayName) {
        this.permissionLevel = permissionLevel;
        this.displayName = displayName;
    }
    
    /**
     * Returns numeric permission level for role comparison.
     * Higher numbers indicate greater permissions.
     * 
     * @return the permission level as an integer
     */
    public int getPermissionLevel() {
        return this.permissionLevel;
    }
    
    /**
     * Returns human-readable role name for display purposes.
     * 
     * @return display name as a String
     */
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Checks if current role has permission level of the required role.
     * A role has permission if its level is equal to or higher than the required role.
     * 
     * @param requiredRole the minimum role level required for an operation
     * @return true if current role has sufficient permissions, false otherwise
     * @throws IllegalArgumentException if requiredRole is null
     */
    public boolean hasPermission(UserRole requiredRole) {
        // Validate input
        if (requiredRole == null) {
            throw new IllegalArgumentException("Required role cannot be null");
        }
        
        // Complex permission logic with multiple conditions for high cyclomatic complexity
        // Check if this is an admin (special case with all permissions)
        if (this == ADMIN) {
            return true;
        }
        
        // Check if this is the same role
        if (this == requiredRole) {
            return true;
        }
        
        // Check if this role has higher permission level than required
        if (this.permissionLevel > requiredRole.permissionLevel) {
            return true;
        }
        
        // Check for special case permissions between roles
        if (this == MERCHANT && requiredRole == CUSTOMER) {
            // Merchants always have customer permissions
            return true;
        }
        
        if (this == MODERATOR) {
            // Moderators have special permissions for customer and merchant roles
            if (requiredRole == CUSTOMER || requiredRole == MERCHANT) {
                return true;
            }
            
            // But moderators have limited permissions based on certain conditions
            // FIXME: This is a placeholder for complex business logic that would determine
            // if a moderator has permissions for specific operations normally reserved for merchants
            return false;
        }
        
        // Default case: insufficient permissions
        return false;
    }
    
    /**
     * Finds a UserRole by its permission level.
     * 
     * @param level the numeric permission level
     * @return Optional containing the role if found, empty Optional otherwise
     */
    public static Optional<UserRole> getByPermissionLevel(int level) {
        return Optional.ofNullable(LEVEL_LOOKUP.get(level));
    }
    
    /**
     * Finds a UserRole by its display name (case-insensitive).
     * 
     * @param name the display name to search for
     * @return Optional containing the role if found, empty Optional otherwise
     */
    public static Optional<UserRole> getByDisplayName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(NAME_LOOKUP.get(name.toUpperCase()));
    }
    
    /**
     * Determines if the given role can moderate content in the system.
     * Only MODERATOR and ADMIN roles can moderate content.
     * 
     * @param role the role to check
     * @return true if role has moderation privileges, false otherwise
     */
    public static boolean canModerateContent(UserRole role) {
        // TODO: Implement more sophisticated rules for content moderation
        if (role == null) {
            return false;
        }
        
        switch (role) {
            case ADMIN:
            case MODERATOR:
                return true;
            case MERCHANT:
                // Merchants might moderate content in their own stores only
                // This would require additional context not available in this method
                return false;
            case CUSTOMER:
            case GUEST:
            default:
                return false;
        }
    }
    
    /**
     * Evaluates complex permission scenarios requiring multiple role checks.
     * This demonstrates high cyclomatic complexity with nested conditions.
     * 
     * @param userRole the current user's role
     * @param targetSection the section of the application being accessed
     * @param isOwner whether the user owns the resource being accessed
     * @param isPublic whether the resource is publicly accessible
     * @return true if access should be granted, false otherwise
     */
    public static boolean evaluateComplexPermission(UserRole userRole, String targetSection, 
                                                   boolean isOwner, boolean isPublic) {
        // ADMIN bypasses all checks
        if (userRole == ADMIN) {
            return true;
        }
        
        // Public resources accessible by anyone if marked public
        if (isPublic) {
            // Except for certain administrative sections
            if (targetSection != null && 
                (targetSection.contains("admin") || targetSection.contains("system"))) {
                // Only ADMIN can access these sections even if marked public
                return userRole == ADMIN;
            }
            
            // Public resources are accessible to all authenticated users
            return userRole != GUEST;
        }
        
        // Owner has access to their own resources
        if (isOwner) {
            return userRole != GUEST; // Owner must be authenticated
        }
        
        // Section-specific permissions
        if (targetSection != null) {
            switch (targetSection.toLowerCase()) {
                case "user_management":
                    return userRole == ADMIN || userRole == MODERATOR;
                    
                case "product_management":
                    return userRole == ADMIN || userRole == MERCHANT || userRole == MODERATOR;
                    
                case "reports":
                    // Different roles can access different report levels
                    if (userRole == MERCHANT) {
                        // Merchants can only see their own reports
                        return isOwner;
                    }
                    return userRole == ADMIN || userRole == MODERATOR;
                    
                case "settings":
                    // Only owners and admins can change settings
                    return userRole == ADMIN || isOwner;
                    
                default:
                    // For undefined sections, default to strict permissions
                    return userRole == ADMIN;
            }
        }
        
        // Default deny for undefined scenarios
        return false;
    }
}