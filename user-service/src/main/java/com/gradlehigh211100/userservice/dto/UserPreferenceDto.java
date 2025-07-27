package com.gradlehigh211100.userservice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for User Preferences.
 * Used for transferring user preference data between layers.
 */
public class UserPreferenceDto {
    
    private Long id;
    
    private Long userId;
    
    @NotBlank(message = "Preference key is required")
    private String key;
    
    @NotBlank(message = "Preference value is required")
    private String value;
    
    private String category;
    
    private boolean premiumOnly;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Default constructor
    public UserPreferenceDto() {
    }
    
    // Constructor with fields
    public UserPreferenceDto(Long id, Long userId, String key, String value, 
                           String category, boolean premiumOnly,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.key = key;
        this.value = value;
        this.category = category;
        this.premiumOnly = premiumOnly;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPremiumOnly() {
        return premiumOnly;
    }

    public void setPremiumOnly(boolean premiumOnly) {
        this.premiumOnly = premiumOnly;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserPreferenceDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", category='" + category + '\'' +
                ", premiumOnly=" + premiumOnly +
                '}';
    }
}