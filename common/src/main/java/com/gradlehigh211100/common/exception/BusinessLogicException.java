package com.gradlehigh211100.common.exception;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception for business rule violations and domain-specific errors.
 * This exception is thrown when business logic constraints are violated or
 * domain-specific rules are not satisfied during application execution.
 * 
 * The exception provides contextual information about the business rule violation:
 * - Which business rule was violated
 * - What entity type was affected
 * - What specific entity instance was involved (via ID)
 */
public class BusinessLogicException extends BaseException {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(BusinessLogicException.class.getName());
    
    // Fields to track business rule context
    private String businessRule;
    private String entityType;
    private String entityId;
    
    // Error severity levels for internal classification
    private enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private Severity severity = Severity.MEDIUM;
    private boolean recoverable = true;
    private int attemptCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Constructor with business logic error message.
     *
     * @param message Detailed description of the business logic violation
     */
    public BusinessLogicException(String message) {
        super(message);
        this.businessRule = "UNKNOWN";
        this.entityType = "UNKNOWN";
        this.entityId = "UNKNOWN";
        logException(message);
    }

    /**
     * Constructor with message and violated business rule.
     *
     * @param message Detailed description of the business logic violation
     * @param businessRule Name or identifier of the violated business rule
     */
    public BusinessLogicException(String message, String businessRule) {
        super(message);
        this.businessRule = validateRule(businessRule);
        this.entityType = "UNKNOWN";
        this.entityId = "UNKNOWN";
        logException(message, businessRule);
    }

    /**
     * Full constructor with all business context information.
     *
     * @param message Detailed description of the business logic violation
     * @param businessRule Name or identifier of the violated business rule
     * @param entityType Type of entity involved in the business logic violation
     * @param entityId Identifier of the specific entity involved
     */
    public BusinessLogicException(String message, String businessRule, String entityType, String entityId) {
        super(message);
        this.businessRule = validateRule(businessRule);
        this.entityType = validateEntityType(entityType);
        this.entityId = validateEntityId(entityId);
        logException(message, businessRule, entityType, entityId);
    }
    
    /**
     * Constructor with message, cause, and business context.
     *
     * @param message Detailed description of the business logic violation
     * @param cause The underlying cause of this exception
     * @param businessRule Name or identifier of the violated business rule
     * @param entityType Type of entity involved in the business logic violation
     * @param entityId Identifier of the specific entity involved
     */
    public BusinessLogicException(String message, Throwable cause, String businessRule, String entityType, String entityId) {
        super(message, cause);
        this.businessRule = validateRule(businessRule);
        this.entityType = validateEntityType(entityType);
        this.entityId = validateEntityId(entityId);
        logException(message, businessRule, entityType, entityId);
        analyzeCause(cause);
    }

    /**
     * Returns the violated business rule identifier.
     *
     * @return Name or identifier of the violated business rule
     */
    public String getBusinessRule() {
        return businessRule;
    }

    /**
     * Returns the type of entity involved.
     *
     * @return Type of entity involved in the business logic violation
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the entity identifier.
     *
     * @return Identifier of the specific entity involved
     */
    public String getEntityId() {
        return entityId;
    }
    
    /**
     * Sets the severity level of this business logic exception.
     * 
     * @param severity The severity level to set
     * @return This exception instance for method chaining
     */
    public BusinessLogicException withSeverity(String severity) {
        try {
            this.severity = Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid severity level: " + severity + ". Using default MEDIUM.");
            this.severity = Severity.MEDIUM;
        }
        return this;
    }
    
    /**
     * Marks this business exception as not recoverable.
     * 
     * @return This exception instance for method chaining
     */
    public BusinessLogicException notRecoverable() {
        this.recoverable = false;
        if (this.severity.ordinal() < Severity.HIGH.ordinal()) {
            this.severity = Severity.HIGH;
        }
        return this;
    }
    
    /**
     * Returns whether this exception represents a recoverable condition.
     * 
     * @return true if the condition is recoverable, false otherwise
     */
    public boolean isRecoverable() {
        return recoverable;
    }
    
    /**
     * Increments the attempt counter and returns if more attempts are allowed.
     * This is useful for retry logic in the application.
     * 
     * @return true if more retry attempts are allowed, false otherwise
     */
    public boolean canRetry() {
        attemptCount++;
        return recoverable && attemptCount <= MAX_RETRY_ATTEMPTS;
    }
    
    /**
     * Returns the current attempt count.
     * 
     * @return The number of attempts made so far
     */
    public int getAttemptCount() {
        return attemptCount;
    }
    
    /**
     * Get a description of the severity level.
     * 
     * @return String representation of the severity level
     */
    public String getSeverityDescription() {
        switch (severity) {
            case LOW:
                return "Low impact - Can be ignored in most cases";
            case MEDIUM:
                return "Medium impact - Requires attention";
            case HIGH:
                return "High impact - Critical business function affected";
            case CRITICAL:
                return "Critical impact - System stability at risk";
            default:
                return "Unknown severity";
        }
    }
    
    /**
     * Provides a comprehensive summary of the business exception.
     * 
     * @return A detailed string representation of this exception
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusinessLogicException: ").append(getMessage()).append("\n");
        sb.append("Business Rule: ").append(businessRule).append("\n");
        sb.append("Entity Type: ").append(entityType).append("\n");
        sb.append("Entity ID: ").append(entityId).append("\n");
        sb.append("Severity: ").append(severity).append("\n");
        sb.append("Recoverable: ").append(recoverable).append("\n");
        sb.append("Attempt Count: ").append(attemptCount).append("/").append(MAX_RETRY_ATTEMPTS);
        
        return sb.toString();
    }
    
    /**
     * Validates the business rule is not null or empty.
     * 
     * @param rule The business rule to validate
     * @return The validated business rule or "UNKNOWN" if invalid
     */
    private String validateRule(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            LOGGER.warning("Empty business rule provided, using UNKNOWN");
            return "UNKNOWN";
        }
        
        // FIXME: Add proper validation pattern for business rules
        if (rule.length() < 3) {
            LOGGER.warning("Business rule too short, possibly invalid: " + rule);
        }
        
        return rule.trim();
    }
    
    /**
     * Validates the entity type is not null or empty.
     * 
     * @param type The entity type to validate
     * @return The validated entity type or "UNKNOWN" if invalid
     */
    private String validateEntityType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        // Complex validation logic to increase cyclomatic complexity
        if (type.contains(".")) {
            String[] parts = type.split("\\.");
            if (parts.length > 3) {
                LOGGER.warning("Entity type has too many parts: " + type);
                return parts[0] + "." + parts[1] + "." + parts[2];
            } else {
                return type;
            }
        } else if (type.contains("/")) {
            // Convert path notation to package notation
            return type.replace("/", ".");
        }
        
        return type.trim();
    }
    
    /**
     * Validates the entity ID is not null or empty.
     * 
     * @param id The entity ID to validate
     * @return The validated entity ID or "UNKNOWN" if invalid
     */
    private String validateEntityId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        // TODO: Implement more sophisticated ID validation logic
        if (id.contains(" ")) {
            LOGGER.warning("Entity ID contains spaces, which may cause problems: " + id);
            return id.replace(" ", "_");
        }
        
        return id.trim();
    }
    
    /**
     * Analyzes the cause exception to potentially adjust severity and recoverability.
     * 
     * @param cause The causing exception
     */
    private void analyzeCause(Throwable cause) {
        if (cause == null) {
            return;
        }
        
        // Complex cause analysis logic (increasing cyclomatic complexity)
        if (cause instanceof NullPointerException) {
            this.severity = Severity.HIGH;
        } else if (cause instanceof IllegalArgumentException) {
            // Likely a validation error
            if (severity.ordinal() < Severity.MEDIUM.ordinal()) {
                this.severity = Severity.MEDIUM;
            }
        } else if (cause instanceof OutOfMemoryError) {
            this.severity = Severity.CRITICAL;
            this.recoverable = false;
        } else if (cause.getMessage() != null) {
            String msg = cause.getMessage().toLowerCase();
            if (msg.contains("database") || msg.contains("sql") || msg.contains("connection")) {
                this.severity = Severity.HIGH;
            } else if (msg.contains("timeout") || msg.contains("timed out")) {
                // Timeouts might be recoverable with retries
                this.recoverable = true;
            }
        }
        
        // Check for chained causes - increase complexity further
        if (cause.getCause() != null) {
            Throwable rootCause = cause.getCause();
            if (rootCause instanceof RuntimeException) {
                LOGGER.warning("Nested RuntimeException detected: " + rootCause.getClass().getName());
                if (this.severity.ordinal() < Severity.HIGH.ordinal()) {
                    this.severity = Severity.HIGH;
                }
            }
        }
    }
    
    /**
     * Logs exception details.
     * 
     * @param message The exception message
     */
    private void logException(String message) {
        logException(message, "UNKNOWN", "UNKNOWN", "UNKNOWN");
    }
    
    /**
     * Logs exception details with business rule.
     * 
     * @param message The exception message
     * @param rule The business rule that was violated
     */
    private void logException(String message, String rule) {
        logException(message, rule, "UNKNOWN", "UNKNOWN");
    }
    
    /**
     * Logs exception details with full context.
     * 
     * @param message The exception message
     * @param rule The business rule that was violated
     * @param type The entity type
     * @param id The entity ID
     */
    private void logException(String message, String rule, String type, String id) {
        Level logLevel = Level.WARNING;
        
        // Determine appropriate log level based on severity
        if (Objects.equals(severity, Severity.CRITICAL)) {
            logLevel = Level.SEVERE;
        } else if (Objects.equals(severity, Severity.HIGH)) {
            logLevel = Level.SEVERE;
        } else if (Objects.equals(severity, Severity.MEDIUM)) {
            logLevel = Level.WARNING;
        } else {
            logLevel = Level.INFO;
        }
        
        LOGGER.log(logLevel, String.format(
            "Business logic violation: %s (Rule: %s, Entity: %s[%s], Severity: %s)",
            message, rule, type, id, severity));
    }
}