package com.gradlehigh211100.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Component for user-specific security checks.
 * Provides methods to verify if a user has permission to access certain resources.
 */
@Component("userSecurity")
public class UserSecurity {

    /**
     * Checks if the authenticated user has permission to access a resource for a specific user ID.
     * 
     * @param authentication The current authentication object
     * @param userId The ID of the user whose resource is being accessed
     * @return true if access is allowed
     */
    public boolean isUserAllowed(Authentication authentication, Long userId) {
        // If not authenticated, no access
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Admin can access any user
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }
        
        // Support roles can access user data
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPPORT"))) {
            return true;
        }
        
        // If user is accessing their own data
        if (authentication.getName().equals(userId.toString())) {
            return true;
        }
        
        // Default to denied
        return false;
    }
}