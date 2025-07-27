package com.ecommerce.root.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Response containing authentication details after successful login
 */
public class AuthenticationResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String token;
    private String refreshToken;
    private Date tokenExpirationDate;
    private Long userId;
    private String username;
    private List<String> roles;
    private boolean mfaRequired;
    private String mfaToken;

    // Default constructor required for Jackson deserialization
    public AuthenticationResponse() {
    }

    // Constructor with minimal fields
    public AuthenticationResponse(String token, String refreshToken, Long userId, String username) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
    }

    // Constructor with all fields
    public AuthenticationResponse(String token, String refreshToken, Date tokenExpirationDate,
                                 Long userId, String username, List<String> roles,
                                 boolean mfaRequired, String mfaToken) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.tokenExpirationDate = tokenExpirationDate;
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.mfaRequired = mfaRequired;
        this.mfaToken = mfaToken;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Date getTokenExpirationDate() {
        return tokenExpirationDate;
    }

    public void setTokenExpirationDate(Date tokenExpirationDate) {
        this.tokenExpirationDate = tokenExpirationDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    /**
     * Check if the authentication token is expired
     * 
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired() {
        if (tokenExpirationDate == null) {
            return false;
        }
        return new Date().after(tokenExpirationDate);
    }

    @Override
    public String toString() {
        return "AuthenticationResponse{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", tokenExpirationDate=" + tokenExpirationDate +
                ", roles=" + roles +
                ", mfaRequired=" + mfaRequired +
                '}';
    }
}