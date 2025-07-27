package com.gradlehigh211100.userservice.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.CollectionTable;
import javax.persistence.JoinColumn;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * JPA entity representing user roles (customer, admin, merchant) with permissions and hierarchical role management.
 * This entity manages the role definitions within the system and their associated permissions.
 * 
 * Roles are organized in a hierarchical structure where higher level roles inherit permissions from lower levels.
 * Each role contains a set of explicit permissions that define what actions users with this role can perform.
 */
@Entity
@Table(name = "roles")
public class RoleEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Role name cannot be blank")
    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @NotNull(message = "Role level cannot be null")
    @Column(name = "level", nullable = false)
    private Integer level;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    // Cache for permission checks to improve performance
    private transient Set<String> effectivePermissionCache;
    private transient boolean permissionCacheInitialized = false;
    
    /**
     * Default constructor required by JPA
     */
    public RoleEntity() {
        // Required by JPA
    }
    
    /**
     * Constructor with essential role properties
     * 
     * @param name Role name (e.g., CUSTOMER, ADMIN, MERCHANT)
     * @param level Hierarchical level of the role
     */
    public RoleEntity(String name, Integer level) {
        this.name = name;
        this.level = level;
        this.active = true;
    }
    
    /**
     * Full constructor for role entity
     * 
     * @param name Role name
     * @param description Role description
     * @param level Hierarchical level
     * @param permissions Initial set of permissions
     * @param active Whether the role is active
     */
    public RoleEntity(String name, String description, Integer level, Set<String> permissions, Boolean active) {
        this.name = name;
        this.description = description;
        this.level = level;
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
        this.active = active;
    }

    /**
     * Gets the role ID
     * 
     * @return The role ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the role ID
     * 
     * @param id The role ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the role name
     * 
     * @return The role name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the role name
     * 
     * @param name The role name to set
     */
    public void setName(String name) {
        this.name = name;
        invalidatePermissionCache();
    }
    
    /**
     * Gets the role description
     * 
     * @return The role description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the role description
     * 
     * @param description The role description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the hierarchical level of the role
     * 
     * @return The role level
     */
    public Integer getLevel() {
        return level;
    }
    
    /**
     * Sets the hierarchical level of the role
     * 
     * @param level The role level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
        invalidatePermissionCache();
    }
    
    /**
     * Gets the permissions associated with this role
     * 
     * @return Set of permission strings
     */
    public Set<String> getPermissions() {
        return new HashSet<>(permissions);
    }
    
    /**
     * Sets the permissions for this role
     * 
     * @param permissions The permissions to set
     */
    public void setPermissions(Set<String> permissions) {
        this.permissions.clear();
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
        invalidatePermissionCache();
    }
    
    /**
     * Gets the active status of the role
     * 
     * @return True if the role is active, false otherwise
     */
    public Boolean getActive() {
        return active;
    }
    
    /**
     * Sets the active status of the role
     * 
     * @param active The active status to set
     */
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    /**
     * Adds a permission to this role
     * 
     * @param permission The permission to add
     * @return True if the permission was added, false if it already existed
     */
    public boolean addPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }
        boolean added = permissions.add(permission.trim());
        if (added) {
            invalidatePermissionCache();
        }
        return added;
    }
    
    /**
     * Removes a permission from this role
     * 
     * @param permission The permission to remove
     * @return True if the permission was removed, false if it wasn't present
     */
    public boolean removePermission(String permission) {
        if (permission == null) {
            return false;
        }
        boolean removed = permissions.remove(permission);
        if (removed) {
            invalidatePermissionCache();
        }
        return removed;
    }
    
    /**
     * Checks if this role has the specified permission
     * 
     * @param permission The permission to check
     * @return True if the role has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        if (permission == null || !active) {
            return false;
        }
        
        // Use cached effective permissions if available
        if (!permissionCacheInitialized) {
            initializePermissionCache();
        }
        
        return effectivePermissionCache.contains(permission) || 
               // Check for wildcard permissions
               effectivePermissionCache.stream().anyMatch(p -> 
                   p.endsWith("*") && permission.startsWith(p.substring(0, p.length() - 1)));
    }
    
    /**
     * Checks if this role has any of the specified permissions
     * 
     * @param permissions Set of permissions to check
     * @return True if the role has any of the permissions, false otherwise
     */
    public boolean hasAnyPermission(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty() || !active) {
            return false;
        }
        
        return permissions.stream().anyMatch(this::hasPermission);
    }
    
    /**
     * Checks if this role has all of the specified permissions
     * 
     * @param permissions Set of permissions to check
     * @return True if the role has all the permissions, false otherwise
     */
    public boolean hasAllPermissions(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        
        if (!active) {
            return false;
        }
        
        return permissions.stream().allMatch(this::hasPermission);
    }
    
    /**
     * Initialize the permission cache for faster permission checking
     */
    private void initializePermissionCache() {
        effectivePermissionCache = new HashSet<>(permissions);
        
        // FIXME: In a real implementation, we would add inherited permissions from lower level roles
        // This would require injecting a service to fetch parent roles based on the level hierarchy
        // For now, we just use direct permissions
        
        permissionCacheInitialized = true;
    }
    
    /**
     * Invalidates the permission cache when permissions or hierarchy changes
     */
    private void invalidatePermissionCache() {
        permissionCacheInitialized = false;
        effectivePermissionCache = null;
    }
    
    /**
     * Determines if this role is higher in hierarchy than another role
     * 
     * @param otherRole The other role to compare with
     * @return True if this role has a higher level, false otherwise
     */
    public boolean isHigherThan(RoleEntity otherRole) {
        if (otherRole == null || otherRole.getLevel() == null) {
            return true;
        }
        return this.level != null && this.level > otherRole.getLevel();
    }
    
    /**
     * Checks if this role is of the specified type
     * 
     * @param roleName The role name to check against
     * @return True if this role has the specified name, false otherwise
     */
    public boolean isRole(String roleName) {
        if (roleName == null || name == null) {
            return false;
        }
        return name.equalsIgnoreCase(roleName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RoleEntity role = (RoleEntity) o;
        
        if (id != null) {
            return id.equals(role.id);
        }
        
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (name != null ? name.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "RoleEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", active=" + active +
                ", permissionCount=" + (permissions != null ? permissions.size() : 0) +
                '}';
    }
    
    // TODO: Add methods for role comparison based on hierarchy and permission inheritance
    // TODO: Implement proper permission inheritance from parent roles
}