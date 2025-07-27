package com.gradlehigh211100.common.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Base class for all persistent entities in the system.
 * Provides common fields and functionality for all entities.
 */
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
    private Boolean active;
    
    /**
     * Default constructor
     */
    public BaseEntity() {
        this.createdAt = new Date();
        this.active = true;
    }
    
    /**
     * Constructor with ID
     * 
     * @param id the entity ID
     */
    public BaseEntity(Long id) {
        this();
        this.id = id;
    }

    /**
     * Gets the entity ID
     * 
     * @return the entity ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the entity ID
     * 
     * @param id the entity ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the creation timestamp
     * 
     * @return when the entity was created
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp
     * 
     * @param createdAt when the entity was created
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the user who created the entity
     * 
     * @return creator user identifier
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user who created the entity
     * 
     * @param createdBy creator user identifier
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the last update timestamp
     * 
     * @return when the entity was last updated
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp
     * 
     * @param updatedAt when the entity was last updated
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the user who last updated the entity
     * 
     * @return updater user identifier
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the user who last updated the entity
     * 
     * @param updatedBy updater user identifier
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Checks if the entity is active
     * 
     * @return true if active, false if inactive
     */
    public Boolean isActive() {
        return active;
    }

    /**
     * Sets the entity active state
     * 
     * @param active true to activate, false to deactivate
     */
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    /**
     * Pre-persist hook to set creation timestamp
     */
    public void prePersist() {
        this.createdAt = new Date();
    }
    
    /**
     * Pre-update hook to set update timestamp
     */
    public void preUpdate() {
        this.updatedAt = new Date();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;
        
        BaseEntity that = (BaseEntity) o;
        
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", active=" + active +
                '}';
    }
}