package com.gradlehigh211100.common.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base entity class providing common audit fields and functionality for all domain entities.
 * This class contains common attributes and behaviors shared across all entities in the system.
 * It implements complex auditing functionality and supports optimistic locking through version tracking.
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "active")
    private boolean active = true;

    /**
     * JPA callback method executed before entity persistence.
     * Sets creation timestamp and marks entity as active by default.
     * This method is automatically called by the JPA provider when an entity is being persisted.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.active = true;
        
        // If system context is available, set the current user
        try {
            // FIXME: Replace with actual user context when security framework is implemented
            String currentUser = getCurrentUserFromContext();
            this.createdBy = currentUser;
            this.updatedBy = currentUser;
        } catch (Exception e) {
            // Fallback for system operations or when security context is not available
            this.createdBy = "SYSTEM";
            this.updatedBy = "SYSTEM";
        }
        
        // Perform additional validation if needed
        validateBeforePersist();
    }
    
    /**
     * Extension point for subclasses to implement custom validation logic before persistence.
     * Can be overridden by child classes to add specialized validation.
     * 
     * TODO: Consider making this method throw a custom validation exception
     */
    protected void validateBeforePersist() {
        // Default implementation does nothing
    }

    /**
     * JPA callback method executed before entity update.
     * Updates the lastUpdated timestamp and the user who performed the update.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        
        // Capture the user who is making this update
        try {
            // FIXME: Replace with actual user context when security framework is implemented
            this.updatedBy = getCurrentUserFromContext();
        } catch (Exception e) {
            // Fallback for system operations
            this.updatedBy = "SYSTEM";
        }
        
        // Validate entity state before update
        if (this.createdAt == null) {
            // This should never happen with proper JPA lifecycle
            this.createdAt = this.updatedAt;
            // FIXME: Log this anomaly when logging framework is in place
        }
        
        // Execute custom validation logic
        validateBeforeUpdate();
    }
    
    /**
     * Extension point for subclasses to implement custom validation logic before update.
     * Can be overridden by child classes to add specialized validation.
     */
    protected void validateBeforeUpdate() {
        // Default implementation does nothing
    }
    
    /**
     * Helper method to get the current user from security context.
     * 
     * @return the username of the currently authenticated user
     */
    private String getCurrentUserFromContext() {
        // TODO: Implement integration with security framework to get authenticated user
        return "UNKNOWN_USER";
    }

    /**
     * Returns the unique identifier of this entity.
     *
     * @return the entity ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this entity.
     * Caution: Changing the ID of a persisted entity may cause issues with JPA.
     *
     * @param id the entity ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the timestamp when this entity was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when this entity was last updated.
     *
     * @return last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Gets the user who created this entity.
     * 
     * @return the username of the creator
     */
    public String getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Gets the user who last updated this entity.
     * 
     * @return the username of the last updater
     */
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    /**
     * Gets the version number used for optimistic locking.
     * 
     * @return the version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Checks if this entity is active.
     * Inactive entities are considered soft-deleted.
     *
     * @return true if entity is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active status of this entity.
     *
     * @param active true to mark as active, false for inactive (soft-deleted)
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Performs a soft delete by marking this entity as inactive.
     * This is an alternative to physical deletion from the database.
     */
    public void softDelete() {
        this.active = false;
        
        // Additional soft delete logic
        this.updatedAt = LocalDateTime.now();
        
        try {
            // FIXME: Replace with actual user context when security framework is implemented
            this.updatedBy = getCurrentUserFromContext();
        } catch (Exception e) {
            this.updatedBy = "SYSTEM";
        }
        
        // TODO: Consider adding a deletedAt timestamp and deletedBy fields
    }

    /**
     * Compares this entity with another object for equality.
     * Entities are considered equal if they have the same non-null ID.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        BaseEntity other = (BaseEntity) obj;
        
        // If both entities have null IDs, they are considered different
        if (id == null && other.id == null) {
            return false;
        }
        
        return Objects.equals(id, other.id);
    }

    /**
     * Returns a hash code value for this entity based on its ID.
     *
     * @return hash code value
     */
    @Override
    public int hashCode() {
        // Use a prime number for better hash distribution
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
    
    /**
     * Returns a string representation of this entity, including its ID and active status.
     * 
     * @return string representation of this entity
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + 
               "[id=" + id + 
               ", active=" + active + 
               ", version=" + version + 
               "]";
    }
}