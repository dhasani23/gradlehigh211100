package com.gradlehigh211100.userservice.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an audit trail entry for user actions within the system.
 * This entity is designed to track all security-relevant events performed by users.
 */
@Entity
@Table(name = "user_audit_trail", 
       indexes = {
           @Index(name = "idx_user_audit_user", columnList = "user_id"),
           @Index(name = "idx_user_audit_action", columnList = "action"),
           @Index(name = "idx_user_audit_success", columnList = "success"),
           @Index(name = "idx_user_audit_ip", columnList = "ip_address"),
           @Index(name = "idx_user_audit_date", columnList = "created_date")
       })
public class UserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "success")
    private boolean success;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "request_path", length = 255)
    private String requestPath;

    @Column(name = "request_method", length = 10)
    private String requestMethod;
    
    @Column(name = "request_params", length = 1000)
    private String requestParams;

    // Constructors
    
    public UserAuditEntity() {
        // Default constructor required by JPA
    }

    /**
     * Create a new audit entry with essential fields
     */
    public UserAuditEntity(UserEntity user, String action, String ipAddress, boolean success) {
        this.user = user;
        this.action = action;
        this.ipAddress = ipAddress;
        this.success = success;
        this.createdDate = LocalDateTime.now();
    }

    /**
     * Create a detailed audit entry for security-relevant events
     */
    public UserAuditEntity(UserEntity user, String action, String details, 
                          String ipAddress, String userAgent, boolean success) {
        this.user = user;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.createdDate = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    // Business methods
    
    /**
     * Create an audit trail entry for failed login attempt
     */
    public static UserAuditEntity createLoginFailureAudit(UserEntity user, String ipAddress, String userAgent, String failureReason) {
        UserAuditEntity audit = new UserAuditEntity(user, "LOGIN", ipAddress, false);
        audit.setUserAgent(userAgent);
        audit.setFailureReason(failureReason);
        audit.setDetails("Failed login attempt");
        return audit;
    }
    
    /**
     * Create an audit trail entry for successful login
     */
    public static UserAuditEntity createLoginSuccessAudit(UserEntity user, String ipAddress, String userAgent, String sessionId) {
        UserAuditEntity audit = new UserAuditEntity(user, "LOGIN", ipAddress, true);
        audit.setUserAgent(userAgent);
        audit.setSessionId(sessionId);
        audit.setDetails("Successful login");
        return audit;
    }
    
    /**
     * Create an audit trail entry for logout
     */
    public static UserAuditEntity createLogoutAudit(UserEntity user, String ipAddress, String sessionId) {
        UserAuditEntity audit = new UserAuditEntity(user, "LOGOUT", ipAddress, true);
        audit.setSessionId(sessionId);
        audit.setDetails("User logout");
        return audit;
    }
    
    /**
     * Create an audit trail entry for resource access
     */
    public static UserAuditEntity createResourceAccessAudit(UserEntity user, String resourceType, 
                                                           String resourceId, String ipAddress, 
                                                           boolean success) {
        UserAuditEntity audit = new UserAuditEntity(user, "RESOURCE_ACCESS", ipAddress, success);
        audit.setResourceType(resourceType);
        audit.setResourceId(resourceId);
        audit.setDetails("Access to " + resourceType + " resource with ID " + resourceId);
        return audit;
    }
    
    /**
     * Determine if this audit entry indicates a security concern
     */
    public boolean isSecurityConcern() {
        // Failed login attempts are always a security concern
        if ("LOGIN".equals(action) && !success) {
            return true;
        }
        
        // Resource access failures may indicate unauthorized access attempts
        if ("RESOURCE_ACCESS".equals(action) && !success) {
            return true;
        }
        
        // Multiple account actions from same IP with different users might be concerning
        return "ADMIN_ACTION".equals(action) && !success;
    }

    // Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserAuditEntity that = (UserAuditEntity) o;
        
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UserAuditEntity{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", action='" + action + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", success=" + success +
                ", createdDate=" + createdDate +
                '}';
    }
}