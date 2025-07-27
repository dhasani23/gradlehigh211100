package com.gradlehigh211100.userservice.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing a user preference in the system
 */
@Entity
@Table(name = "user_preferences", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "preference_key"}))
public class UserPreference implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "preference_key", nullable = false, length = 100)
    private String preferenceKey;
    
    @Column(name = "preference_value", columnDefinition = "TEXT")
    private String preferenceValue;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "data_type", length = 20)
    private String dataType;
    
    @Column(name = "last_modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    
    @Column(name = "is_encrypted")
    private boolean isEncrypted;
    
    // Default constructor required by JPA
    public UserPreference() {
    }
    
    // Full constructor
    public UserPreference(Long id, Long userId, String preferenceKey, String preferenceValue, 
                          String category, String dataType, Date lastModified, boolean isEncrypted) {
        this.id = id;
        this.userId = userId;
        this.preferenceKey = preferenceKey;
        this.preferenceValue = preferenceValue;
        this.category = category;
        this.dataType = dataType;
        this.lastModified = lastModified;
        this.isEncrypted = isEncrypted;
    }
    
    // Minimal constructor
    public UserPreference(Long userId, String preferenceKey, String preferenceValue, String category) {
        this.userId = userId;
        this.preferenceKey = preferenceKey;
        this.preferenceValue = preferenceValue;
        this.category = category;
        this.lastModified = new Date();
        this.dataType = "STRING";
        this.isEncrypted = false;
    }

    @PrePersist
    @PreUpdate
    public void updateLastModified() {
        this.lastModified = new Date();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getPreferenceKey() {
        return preferenceKey;
    }
    
    public void setPreferenceKey(String preferenceKey) {
        this.preferenceKey = preferenceKey;
    }
    
    public String getPreferenceValue() {
        return preferenceValue;
    }
    
    public void setPreferenceValue(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public Date getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    
    public boolean isEncrypted() {
        return isEncrypted;
    }
    
    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreference that = (UserPreference) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(preferenceKey, that.preferenceKey);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, preferenceKey);
    }
    
    @Override
    public String toString() {
        return "UserPreference{" +
                "id=" + id +
                ", userId=" + userId +
                ", preferenceKey='" + preferenceKey + '\'' +
                ", category='" + category + '\'' +
                ", dataType='" + dataType + '\'' +
                ", lastModified=" + lastModified +
                ", isEncrypted=" + isEncrypted +
                '}';
    }
}