package com.gradlehigh211100.userservice.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToMany;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Table;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA entity representing user data with comprehensive user information, 
 * roles, and security attributes.
 * 
 * This entity stores detailed user information including authentication details,
 * personal information, and security related flags.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    /**
     * Default constructor required by JPA
     */
    public UserEntity() {
        // Required empty constructor
    }

    /**
     * Constructor with essential user information
     * 
     * @param username the unique username
     * @param email the user's email address
     * @param password the encoded password
     */
    public UserEntity(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * Full constructor with all fields
     * 
     * @param username the unique username
     * @param email the user's email address
     * @param password the encoded password
     * @param firstName the user's first name
     * @param lastName the user's last name
     */
    public UserEntity(String username, String email, String password, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Gets the user ID
     * 
     * @return the user's unique identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the user ID
     * 
     * @param id the user's unique identifier
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the username
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the user's email address
     * 
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address
     * 
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the encoded password
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the encoded password
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user's first name
     * 
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name
     * 
     * @param firstName the first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name
     * 
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name
     * 
     * @param lastName the last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the concatenated first name and last name
     * 
     * @return the user's full name
     */
    public String getFullName() {
        // Handle cases where first name or last name could be null
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        
        // If both first and last name are empty, return username as fallback
        if (fullName.length() == 0) {
            return username;
        }
        
        return fullName.toString();
    }

    /**
     * Gets the roles assigned to this user
     * 
     * @return set of roles
     */
    public Set<RoleEntity> getRoles() {
        return roles;
    }

    /**
     * Sets the roles for this user
     * 
     * @param roles the set of roles to assign
     */
    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

    /**
     * Checks if the user has the specified role
     * 
     * @param roleName the role name to check
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String roleName) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        
        // FIXME: This might cause performance issues for users with many roles
        return roles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Adds a role to the user's role collection
     * 
     * @param role the role to add
     */
    public void addRole(RoleEntity role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        
        if (roles == null) {
            roles = new HashSet<>();
        }
        
        // Check if role already exists to avoid duplicates
        if (roles.stream().noneMatch(r -> r.getName().equals(role.getName()))) {
            roles.add(role);
        }
    }

    /**
     * Removes a role from the user's role collection
     * 
     * @param role the role to remove
     */
    public void removeRole(RoleEntity role) {
        if (role == null || roles == null || roles.isEmpty()) {
            return;
        }
        
        // TODO: Consider removing by role name rather than object equality
        roles.remove(role);
    }

    /**
     * Gets the active status flag
     * 
     * @return true if the account is active, false otherwise
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the active status flag
     * 
     * @param active the active status to set
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Gets the email verification status
     * 
     * @return true if the email is verified, false otherwise
     */
    public Boolean getEmailVerified() {
        return emailVerified;
    }

    /**
     * Sets the email verification status
     * 
     * @param emailVerified the verification status to set
     */
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    /**
     * Gets the last login date
     * 
     * @return the timestamp of the last successful login
     */
    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    /**
     * Sets the last login date
     * 
     * @param lastLoginDate the timestamp to set
     */
    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    /**
     * Gets the count of failed login attempts
     * 
     * @return the number of consecutive failed login attempts
     */
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /**
     * Sets the count of failed login attempts
     * 
     * @param failedLoginAttempts the count to set
     */
    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    /**
     * Gets the account locked status
     * 
     * @return true if the account is locked, false otherwise
     */
    public Boolean getAccountLocked() {
        return accountLocked;
    }

    /**
     * Sets the account locked status
     * 
     * @param accountLocked the locked status to set
     */
    public void setAccountLocked(Boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    /**
     * Increments failed login attempts and locks account if threshold is exceeded
     * 
     * @param maxAttempts the threshold for account locking
     * @return true if the account is now locked, false otherwise
     */
    public boolean incrementFailedLoginAttempts(int maxAttempts) {
        this.failedLoginAttempts++;
        
        // Check if we need to lock the account
        if (this.failedLoginAttempts >= maxAttempts) {
            this.accountLocked = true;
            return true;
        }
        
        return false;
    }

    /**
     * Resets failed login attempts counter
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    /**
     * Gets a string representation of the user's roles
     * 
     * @return comma-separated list of role names
     */
    public String getRoleNames() {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        
        return roles.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserEntity that = (UserEntity) o;
        
        if (id != null) {
            return id.equals(that.id);
        }
        
        // If ID is null, compare by username and email as they should be unique
        return username.equals(that.username) && email.equals(that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : 0, username, email);
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", active=" + active +
                ", emailVerified=" + emailVerified +
                ", roles=[" + getRoleNames() + "]" +
                '}';
    }
}