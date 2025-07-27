package com.ecommerce.root.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Data Transfer Object for User information
 */
public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Date createdDate;
    private Date lastLoginDate;
    private boolean active;
    private List<String> roles;
    private UserPreferences preferences;

    // Default constructor required for Jackson deserialization
    public UserDto() {
    }

    // Constructor with all fields
    public UserDto(Long id, String username, String email, String firstName, String lastName,
                  String phoneNumber, Date createdDate, Date lastLoginDate, boolean active, 
                  List<String> roles, UserPreferences preferences) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.createdDate = createdDate;
        this.lastLoginDate = lastLoginDate;
        this.active = active;
        this.roles = roles;
        this.preferences = preferences;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", active=" + active +
                ", roles=" + roles +
                '}';
    }

    /**
     * Inner class representing user preferences
     */
    public static class UserPreferences implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private boolean darkMode;
        private String language;
        private boolean emailNotifications;
        private boolean pushNotifications;

        public UserPreferences() {
        }

        public UserPreferences(boolean darkMode, String language, boolean emailNotifications, boolean pushNotifications) {
            this.darkMode = darkMode;
            this.language = language;
            this.emailNotifications = emailNotifications;
            this.pushNotifications = pushNotifications;
        }

        public boolean isDarkMode() {
            return darkMode;
        }

        public void setDarkMode(boolean darkMode) {
            this.darkMode = darkMode;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isEmailNotifications() {
            return emailNotifications;
        }

        public void setEmailNotifications(boolean emailNotifications) {
            this.emailNotifications = emailNotifications;
        }

        public boolean isPushNotifications() {
            return pushNotifications;
        }

        public void setPushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
        }
    }
}