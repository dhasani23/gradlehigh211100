package com.ecommerce.root.model;

import java.io.Serializable;

/**
 * Encapsulates user login credentials
 */
public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private boolean rememberMe;
    private String deviceId;
    private String ipAddress;

    // Default constructor required for Jackson deserialization
    public LoginRequest() {
    }

    // Constructor with required fields
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Constructor with all fields
    public LoginRequest(String username, String password, boolean rememberMe, String deviceId, String ipAddress) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
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

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        // Don't include password in toString for security reasons
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", rememberMe=" + rememberMe +
                ", deviceId='" + deviceId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}