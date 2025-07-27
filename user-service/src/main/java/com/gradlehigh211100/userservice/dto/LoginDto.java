package com.gradlehigh211100.userservice.dto;

/**
 * Data Transfer Object for login credentials
 */
public class LoginDto {
    private String username;
    private String password;

    /**
     * Default constructor
     */
    public LoginDto() {
    }

    /**
     * Constructor with parameters
     * 
     * @param username username or email
     * @param password user password
     */
    public LoginDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Get username
     * 
     * @return username or email
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set username
     * 
     * @param username username or email
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get password
     * 
     * @return user password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set password
     * 
     * @param password user password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}