package com.gradlehigh211100.userservice.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a user in the system.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "account_non_expired")
    private boolean accountNonExpired = true;

    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired = true;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuditEntity> auditTrail = new ArrayList<>();

    // Constructors
    
    public UserEntity() {
        // Default constructor required by JPA
    }

    public UserEntity(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdDate = LocalDateTime.now();
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public List<UserAuditEntity> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(List<UserAuditEntity> auditTrail) {
        this.auditTrail = auditTrail;
    }
    
    public void addAuditEntry(UserAuditEntity auditEntry) {
        auditTrail.add(auditEntry);
        auditEntry.setUser(this);
    }
    
    public void removeAuditEntry(UserAuditEntity auditEntry) {
        auditTrail.remove(auditEntry);
        auditEntry.setUser(null);
    }

    // Business methods
    
    /**
     * Increment the login attempts counter
     */
    public void incrementLoginAttempts() {
        this.loginAttempts = loginAttempts + 1;
        
        // Auto-lock account after threshold
        if (this.loginAttempts >= 5) {
            this.accountNonLocked = false;
        }
    }
    
    /**
     * Reset login attempts counter after successful login
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lastLoginDate = LocalDateTime.now();
    }
    
    /**
     * Check if this account requires password reset
     */
    public boolean requiresPasswordReset() {
        // If credentials expired, password reset is required
        return !this.credentialsNonExpired;
    }

    /**
     * Get user's full name
     */
    public String getFullName() {
        return (firstName != null && lastName != null) ? 
            firstName + " " + lastName : 
            username;
    }

    // Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserEntity that = (UserEntity) o;
        
        if (id != null) {
            return Objects.equals(id, that.id);
        }
        
        return Objects.equals(username, that.username) &&
               Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : Objects.hash(username, email);
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}