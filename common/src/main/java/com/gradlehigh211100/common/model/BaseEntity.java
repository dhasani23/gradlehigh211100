package com.gradlehigh211100.common.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Base entity class that provides common fields and functionality for all entity classes.
 * This class implements basic persistence functionality and auditing features.
 */
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
    private Integer version;
    private boolean active = true;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Date getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
    
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(getId(), that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    /**
     * Pre-persist hook to set audit fields before entity is saved for the first time.
     */
    public void prePersist() {
        Date now = new Date();
        this.createdDate = now;
        this.lastModifiedDate = now;
        this.version = 0;
    }
    
    /**
     * Pre-update hook to update audit fields before entity is updated.
     */
    public void preUpdate() {
        this.lastModifiedDate = new Date();
        if (this.version != null) {
            this.version++;
        } else {
            this.version = 1;
        }
    }
}