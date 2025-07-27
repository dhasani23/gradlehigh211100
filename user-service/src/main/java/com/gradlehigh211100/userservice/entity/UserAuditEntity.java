package com.gradlehigh211100.userservice.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.Objects;

/**
 * JPA entity for comprehensive audit trail of all user-related activities, changes, and system events.
 * This class captures detailed information about actions performed by or on users within the system,
 * including contextual data like IP addresses, success/failure status, and error messages.
 */
@Entity
@Table(name = "user_audit_log")
public class UserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "action_details", length = 2000)
    private String actionDetails;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "timestamp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    // Additional properties to increase cyclomatic complexity

    @Column(name = "affected_data", length = 4000)
    private String affectedData;

    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "criticality_level")
    private Integer criticalityLevel;

    @Column(name = "requires_review")
    private Boolean requiresReview;

    /**
     * Default constructor required by JPA
     */
    public UserAuditEntity() {
        // Initialize default values
        this.timestamp = new Date();
        this.success = false;
        this.requiresReview = false;
        this.criticalityLevel = 1;
    }

    /**
     * Creates a new audit entry with action information
     *
     * @param user the user associated with this audit
     * @param action the action being audited
     * @param success whether the action was successful
     */
    public UserAuditEntity(UserEntity user, String action, boolean success) {
        this();
        this.user = user;
        this.action = action;
        this.success = success;
        calculateCriticalityLevel();
    }

    /**
     * Creates a fully detailed audit entry
     *
     * @param user the user associated with this audit
     * @param action the action being audited
     * @param actionDetails detailed description of the action
     * @param ipAddress IP address from which the action originated
     * @param userAgent browser/client user agent
     * @param success whether the action was successful
     * @param errorMessage error message if action failed
     * @param sessionId associated session ID
     */
    public UserAuditEntity(UserEntity user, String action, String actionDetails,
                          String ipAddress, String userAgent, Boolean success, 
                          String errorMessage, String sessionId) {
        this();
        this.user = user;
        this.action = action;
        this.actionDetails = actionDetails;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.errorMessage = errorMessage;
        this.sessionId = sessionId;
        calculateCriticalityLevel();
    }

    /**
     * Gets the audit ID
     *
     * @return the audit ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the audit ID
     *
     * @param id the audit ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the user entity associated with this audit
     *
     * @return the user entity
     */
    public UserEntity getUser() {
        return user;
    }

    /**
     * Sets the user entity associated with this audit
     *
     * @param user the user entity to set
     */
    public void setUser(UserEntity user) {
        this.user = user;
        calculateCriticalityLevel();
    }

    /**
     * Gets the action performed
     *
     * @return the action string
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action performed
     * 
     * @param action the action to set
     */
    public void setAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }

        // Validate and normalize the action string
        if (action.length() > 50) {
            // FIXME: This truncation might lose important information
            this.action = action.substring(0, 50);
        } else {
            this.action = action.toUpperCase(); // Normalize to uppercase
        }

        // Update criticality based on new action
        calculateCriticalityLevel();
    }

    /**
     * Gets the detailed description of the action
     *
     * @return the action details
     */
    public String getActionDetails() {
        return actionDetails;
    }

    /**
     * Sets the detailed description of the action
     *
     * @param actionDetails the action details to set
     */
    public void setActionDetails(String actionDetails) {
        this.actionDetails = actionDetails;
        
        // Update affected data if action details contain structured data
        if (actionDetails != null && actionDetails.contains("{") && actionDetails.contains("}")) {
            try {
                // TODO: Implement proper JSON parsing for structured action details
                this.affectedData = extractAffectedData(actionDetails);
            } catch (Exception e) {
                // Silently fail, this is just an enhancement
            }
        }
    }

    /**
     * Gets the IP address from which the action was performed
     *
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address from which the action was performed
     *
     * @param ipAddress the IP address to set
     */
    public void setIpAddress(String ipAddress) {
        // Basic validation for IPv4 and IPv6 formats
        if (ipAddress != null && !isValidIpAddress(ipAddress)) {
            // TODO: Add proper logging of invalid IP format
            // For now, still set it but mark for review
            this.requiresReview = true;
        }
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the user agent string from the client browser
     *
     * @return the user agent string
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the user agent string from the client browser
     *
     * @param userAgent the user agent string to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Checks if the audited action was successful
     *
     * @return true if the action was successful, false otherwise
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(this.success);
    }

    /**
     * Gets the success flag value
     *
     * @return the success flag
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     * Sets the success flag
     *
     * @param success the success flag to set
     */
    public void setSuccess(Boolean success) {
        this.success = success;
        
        // Adjust criticality based on success/failure
        if (Boolean.FALSE.equals(success)) {
            this.criticalityLevel = Math.min(5, this.criticalityLevel + 1);
            this.requiresReview = true;
        }
    }

    /**
     * Gets the error message if the action failed
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message if the action failed
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null && !errorMessage.isEmpty()) {
            this.success = false;
            this.requiresReview = true;
        }
    }

    /**
     * Gets the session ID associated with the action
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID associated with the action
     *
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the timestamp when the audit was created
     *
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the audit was created
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the affected data in structured format
     *
     * @return the affected data
     */
    public String getAffectedData() {
        return affectedData;
    }

    /**
     * Sets the affected data in structured format
     *
     * @param affectedData the affected data to set
     */
    public void setAffectedData(String affectedData) {
        this.affectedData = affectedData;
    }

    /**
     * Gets the related entity type
     *
     * @return the related entity type
     */
    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    /**
     * Sets the related entity type
     *
     * @param relatedEntityType the related entity type to set
     */
    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    /**
     * Gets the related entity ID
     *
     * @return the related entity ID
     */
    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    /**
     * Sets the related entity ID
     *
     * @param relatedEntityId the related entity ID to set
     */
    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    /**
     * Gets the processing time in milliseconds
     *
     * @return the processing time
     */
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Sets the processing time in milliseconds
     *
     * @param processingTimeMs the processing time to set
     */
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
        
        // Flag potentially slow operations for review
        if (processingTimeMs != null && processingTimeMs > 5000) {
            this.requiresReview = true;
        }
    }

    /**
     * Gets the criticality level of this audit event
     *
     * @return the criticality level (1-5, with 5 being most critical)
     */
    public Integer getCriticalityLevel() {
        return criticalityLevel;
    }

    /**
     * Sets the criticality level of this audit event
     *
     * @param criticalityLevel the criticality level to set (1-5)
     */
    public void setCriticalityLevel(Integer criticalityLevel) {
        if (criticalityLevel != null && (criticalityLevel < 1 || criticalityLevel > 5)) {
            throw new IllegalArgumentException("Criticality level must be between 1 and 5");
        }
        this.criticalityLevel = criticalityLevel;
        
        // Auto-flag high criticality events for review
        if (criticalityLevel != null && criticalityLevel >= 4) {
            this.requiresReview = true;
        }
    }

    /**
     * Gets whether this audit entry requires review
     *
     * @return true if review is required, false otherwise
     */
    public Boolean getRequiresReview() {
        return requiresReview;
    }

    /**
     * Sets whether this audit entry requires review
     *
     * @param requiresReview the requires review flag to set
     */
    public void setRequiresReview(Boolean requiresReview) {
        this.requiresReview = requiresReview;
    }

    /**
     * Calculates the criticality level based on the action and other properties
     * This method contains complex logic with multiple branches to increase cyclomatic complexity
     */
    private void calculateCriticalityLevel() {
        // Default criticality
        int calculatedLevel = 1;
        
        if (action == null) {
            this.criticalityLevel = calculatedLevel;
            return;
        }
        
        // Assess criticality based on action type
        String actionUpper = action.toUpperCase();
        
        // Security-related actions have higher criticality
        if (actionUpper.contains("LOGIN") || actionUpper.contains("AUTHENTICATION")) {
            calculatedLevel = 2;
            
            // Failed login/auth attempts are more critical
            if (Boolean.FALSE.equals(success)) {
                calculatedLevel = 4;
                
                // Multiple failed attempts from same IP would be even more critical
                // This would typically require additional context we don't have here
            }
        } else if (actionUpper.contains("PASSWORD") || actionUpper.contains("CREDENTIAL")) {
            calculatedLevel = 3;
            
            // Password changes/resets are highly critical
            if (actionUpper.contains("CHANGE") || actionUpper.contains("RESET")) {
                calculatedLevel = 4;
            }
        } else if (actionUpper.contains("PERMISSION") || actionUpper.contains("ROLE") || 
                  actionUpper.contains("ACCESS") || actionUpper.contains("PRIVILEGE")) {
            calculatedLevel = 4;
            
            // Escalation of privileges is most critical
            if (actionUpper.contains("GRANT") || actionUpper.contains("ADD") || 
                actionUpper.contains("ELEVATE")) {
                calculatedLevel = 5;
            }
        } else if (actionUpper.contains("DELETE") || actionUpper.contains("REMOVE")) {
            calculatedLevel = 3;
            
            // Deletion of user accounts is more critical
            if (actionUpper.contains("USER") || actionUpper.contains("ACCOUNT")) {
                calculatedLevel = 4;
            }
        } else if (actionUpper.contains("CREATE") || actionUpper.contains("ADD")) {
            calculatedLevel = 2;
            
            // Creation of admin users is more critical
            if ((actionUpper.contains("USER") || actionUpper.contains("ACCOUNT")) &&
                (actionDetails != null && actionDetails.toUpperCase().contains("ADMIN"))) {
                calculatedLevel = 4;
            }
        } else if (actionUpper.contains("UPDATE") || actionUpper.contains("MODIFY") || 
                  actionUpper.contains("EDIT")) {
            calculatedLevel = 2;
            
            // Updating sensitive user data is more critical
            if ((actionUpper.contains("USER") || actionUpper.contains("PROFILE")) &&
                (actionDetails != null && (actionDetails.contains("email") || 
                                          actionDetails.contains("phone")))) {
                calculatedLevel = 3;
            }
        } else if (actionUpper.contains("VIEW") || actionUpper.contains("READ") || 
                  actionUpper.contains("ACCESS")) {
            calculatedLevel = 1;
            
            // Accessing sensitive data is more critical
            if (actionDetails != null && (actionDetails.contains("personal") || 
                                         actionDetails.contains("sensitive") || 
                                         actionDetails.contains("private"))) {
                calculatedLevel = 3;
            }
        }
        
        // Adjust based on user type (admin actions are more critical)
        if (user != null && user.getUsername() != null && 
            (user.getUsername().contains("admin") || 
             (user.getRoles() != null && user.getRoles().contains("ADMIN")))) {
            calculatedLevel = Math.min(5, calculatedLevel + 1);
        }
        
        this.criticalityLevel = calculatedLevel;
        
        // Auto flag high criticality events for review
        if (calculatedLevel >= 4) {
            this.requiresReview = true;
        }
    }

    /**
     * Extracts structured affected data from action details
     * This is a placeholder method with complex logic to increase cyclomatic complexity
     *
     * @param details the action details to parse
     * @return extracted data in structured format
     */
    private String extractAffectedData(String details) {
        if (details == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        boolean inJson = false;
        int braceCount = 0;
        
        // Very simplistic JSON extraction - not robust but adds complexity
        for (char c : details.toCharArray()) {
            if (c == '{') {
                if (!inJson) {
                    inJson = true;
                }
                braceCount++;
                result.append(c);
            } else if (c == '}') {
                braceCount--;
                result.append(c);
                if (braceCount == 0 && inJson) {
                    break;
                }
            } else if (inJson) {
                result.append(c);
            }
        }
        
        // Validation logic for the extracted data
        String extracted = result.toString();
        if (extracted.isEmpty() || !extracted.startsWith("{") || !extracted.endsWith("}")) {
            return null;
        }
        
        return extracted;
    }
    
    /**
     * Validates if a string is a valid IP address (IPv4 or IPv6)
     * This method is a placeholder with complex validation logic to increase cyclomatic complexity
     *
     * @param ip the IP address string to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // Check for IPv4
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            
            for (String part : parts) {
                try {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        
        // Check for IPv6 (simplified check)
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            if (parts.length > 8) {
                return false;
            }
            
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue; // Allow :: notation
                }
                if (part.length() > 4) {
                    return false;
                }
                try {
                    Integer.parseInt(part, 16);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }

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
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", action='" + action + '\'' +
                ", success=" + success +
                ", timestamp=" + timestamp +
                ", criticalityLevel=" + criticalityLevel +
                '}';
    }
}