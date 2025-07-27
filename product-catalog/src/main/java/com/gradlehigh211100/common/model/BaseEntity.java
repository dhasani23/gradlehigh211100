package com.gradlehigh211100.common.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Base entity class for all model entities.
 * Provides common fields and functionality for entity classes.
 */
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;
    
    public BaseEntity() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    /**
     * Get entity ID.
     * 
     * @return The entity ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Set entity ID.
     * 
     * @param id The entity ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Get creation date.
     * 
     * @return The creation date
     */
    public Date getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Set creation date.
     * 
     * @param createdAt The creation date to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Get last update date.
     * 
     * @return The last update date
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Set last update date.
     * 
     * @param updatedAt The last update date to set
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Get the user who created this entity.
     * 
     * @return The user who created this entity
     */
    public String getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Set the user who created this entity.
     * 
     * @param createdBy The user who created this entity
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * Get the user who last updated this entity.
     * 
     * @return The user who last updated this entity
     */
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    /**
     * Set the user who last updated this entity.
     * 
     * @param updatedBy The user who last updated this entity
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
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
                ", updatedAt=" + updatedAt +
                '}';
    }
}