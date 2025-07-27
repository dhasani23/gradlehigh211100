package com.ecommerce.root.model;

import java.io.Serializable;
import java.util.List;

/**
 * Request model for creating a new user
 */
public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private String confirmPassword;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<String> roles;
    private boolean sendActivationEmail;
    private UserDto.UserPreferences preferences;

    // Default constructor required for Jackson deserialization
    public CreateUserRequest() {
    }

    // Constructor with required fields
    public CreateUserRequest(String username, String password, String confirmPassword, String email) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.email = email;
    }

    // Constructor with all fields
    public CreateUserRequest(String username, String password, String confirmPassword,
                           String email, String firstName, String lastName, String phoneNumber,
                           List<String> roles, boolean sendActivationEmail, 
                           UserDto.UserPreferences preferences) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.roles = roles;
        this.sendActivationEmail = sendActivationEmail;
        this.preferences = preferences;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isSendActivationEmail() {
        return sendActivationEmail;
    }

    public void setSendActivationEmail(boolean sendActivationEmail) {
        this.sendActivationEmail = sendActivationEmail;
    }

    public UserDto.UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserDto.UserPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Validates if passwords match
     * 
     * @return true if passwords match, false otherwise
     */
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    @Override
    public String toString() {
        // Don't include password in toString for security reasons
        return "CreateUserRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", roles=" + roles +
                ", sendActivationEmail=" + sendActivationEmail +
                '}';
    }
}