package com.gradlehigh211100.common.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Base abstract entity class that all entity models should extend.
 * Provides common fields and functionality for all entities.
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        // TODO: Implement user context to get current user
        createdBy = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        // TODO: Implement user context to get current user
        updatedBy = "SYSTEM";
    }

    /**
     * Gets the entity ID
     * 
     * @return the unique identifier of this entity
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the entity ID
     * 
     * @param id the unique identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the creation timestamp
     * 
     * @return the date when this entity was created
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp
     * 
     * @param createdAt the creation date to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last update timestamp
     * 
     * @return the date when this entity was last updated
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp
     * 
     * @param updatedAt the update date to set
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the user who created this entity
     * 
     * @return the username of the creator
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user who created this entity
     * 
     * @param createdBy the username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the user who last updated this entity
     * 
     * @return the username of the last updater
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the user who last updated this entity
     * 
     * @param updatedBy the username to set
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Gets the entity version (used for optimistic locking)
     * 
     * @return the version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the entity version
     * 
     * @param version the version number to set
     */
    public void setVersion(Long version) {
        this.version = version;
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
        return Objects.hash(id);
    }
}