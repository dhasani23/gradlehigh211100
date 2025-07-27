package com.gradlehigh211100.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradlehigh211100.common.model.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service responsible for tracking and persisting audit information
 * about entity changes, user actions, and system events.
 * 
 * This service provides comprehensive auditing capabilities for the application
 * with support for asynchronous audit operations to minimize performance impact.
 */
@Service
public class AuditTrail {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditTrail.class);
    
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final TaskExecutor asyncExecutor;
    
    // Maximum retries for failed audit operations
    private static final int MAX_RETRY_ATTEMPTS = 3;
    // Default delay between retry attempts in milliseconds
    private static final long RETRY_DELAY_MS = 500;
    
    @Autowired
    public AuditTrail(AuditRepository auditRepository, 
                     ObjectMapper objectMapper, 
                     TaskExecutor asyncExecutor) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
        this.asyncExecutor = asyncExecutor;
    }
    
    /**
     * Audits entity changes with action and user context.
     * 
     * @param entity The entity being audited
     * @param action The action performed (CREATE, UPDATE, DELETE, etc.)
     * @param userId The ID of the user who performed the action
     */
    public void auditEntityChange(BaseEntity entity, String action, String userId) {
        if (entity == null) {
            logger.warn("Cannot audit null entity. Action: {}, User: {}", action, userId);
            return;
        }
        
        try {
            // Extract entity type and ID
            String entityType = entity.getClass().getSimpleName();
            Long entityId = entity.getId();
            
            // Create a details map with entity state
            Map<String, Object> details = new HashMap<>();
            details.put("entityState", entity);
            
            // Additional metadata about the change
            if ("UPDATE".equals(action)) {
                // FIXME: Implement dirty checking to only record changed fields
                details.put("changedFields", "ALL");
            }
            
            // Create and persist audit record asynchronously
            AuditRecord record = createAuditRecord(action, entityType, entityId, userId, details);
            executeAsynchronously(() -> persistAuditRecord(record));
            
            logger.debug("Entity change audited. Type: {}, ID: {}, Action: {}, User: {}", 
                    entityType, entityId, action, userId);
        } catch (Exception e) {
            logger.error("Failed to audit entity change. Entity: {}, Action: {}, User: {}", 
                    entity.getClass().getSimpleName(), action, userId, e);
            // TODO: Consider implementing a fallback mechanism for critical audit failures
        }
    }
    
    /**
     * Audits user actions with detailed context.
     * 
     * @param userId The ID of the user performing the action
     * @param action The action being performed
     * @param details Additional details about the action
     */
    public void auditUserAction(String userId, String action, Map<String, Object> details) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("Attempting to audit user action with invalid user ID. Action: {}", action);
            userId = "SYSTEM"; // Default to system user if not provided
        }
        
        try {
            // Sanitize and validate inputs
            String sanitizedAction = action != null ? action : "UNKNOWN_ACTION";
            Map<String, Object> safeDetails = details != null ? details : Collections.emptyMap();
            
            // Enrich with contextual information
            Map<String, Object> enrichedDetails = new HashMap<>(safeDetails);
            enrichedDetails.put("timestamp", LocalDateTime.now().toString());
            enrichedDetails.put("actionType", "USER_ACTION");
            
            // Create audit record with no specific entity (user action may not be tied to entity)
            AuditRecord record = createAuditRecord(
                sanitizedAction, 
                "UserAction", 
                null,  // No entity ID for general user actions
                userId, 
                enrichedDetails
            );
            
            // For user actions, we persist synchronously for guaranteed tracking
            persistAuditRecord(record);
            
            logger.info("User action audited. User: {}, Action: {}", userId, sanitizedAction);
        } catch (Exception e) {
            logger.error("Failed to audit user action. User: {}, Action: {}", userId, action, e);
            
            // Retry mechanism for critical user action auditing
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    logger.info("Retrying audit of user action. Attempt: {}/{}", attempt, MAX_RETRY_ATTEMPTS);
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                    
                    // Create simplified record for retry
                    Map<String, Object> fallbackDetails = new HashMap<>();
                    fallbackDetails.put("action", action);
                    fallbackDetails.put("originalDetails", details);
                    fallbackDetails.put("retryAttempt", attempt);
                    
                    AuditRecord fallbackRecord = createAuditRecord(
                        "USER_ACTION_RETRY", 
                        "UserAction", 
                        null,
                        userId, 
                        fallbackDetails
                    );
                    
                    persistAuditRecord(fallbackRecord);
                    logger.info("Successfully retried audit of user action on attempt {}", attempt);
                    break;
                } catch (Exception retryException) {
                    logger.error("Retry attempt {} failed for user action audit", attempt, retryException);
                }
            }
        }
    }
    
    /**
     * Audits system-level events and changes.
     * 
     * @param eventType The type of system event
     * @param message A descriptive message about the event
     * @param metadata Additional metadata about the event
     */
    public void auditSystemEvent(String eventType, String message, Map<String, Object> metadata) {
        try {
            // Validate inputs with fallbacks for nulls
            String type = eventType != null ? eventType : "UNKNOWN_EVENT";
            String msg = message != null ? message : "No message provided";
            Map<String, Object> meta = metadata != null ? metadata : Collections.emptyMap();
            
            // Build comprehensive details including system context
            Map<String, Object> eventDetails = new HashMap<>(meta);
            eventDetails.put("message", msg);
            eventDetails.put("timestamp", LocalDateTime.now().toString());
            
            // Add system metrics if available
            try {
                Runtime runtime = Runtime.getRuntime();
                Map<String, Object> systemMetrics = new HashMap<>();
                systemMetrics.put("freeMemory", runtime.freeMemory());
                systemMetrics.put("totalMemory", runtime.totalMemory());
                systemMetrics.put("maxMemory", runtime.maxMemory());
                systemMetrics.put("availableProcessors", runtime.availableProcessors());
                eventDetails.put("systemMetrics", systemMetrics);
            } catch (Exception e) {
                logger.debug("Could not include system metrics in audit", e);
            }
            
            // Create system event audit record
            AuditRecord record = createAuditRecord(
                type,
                "SystemEvent",
                null, // No entity ID for system events
                "SYSTEM", // System is the actor
                eventDetails
            );
            
            // For high priority events, persist synchronously
            if (isHighPriorityEvent(type)) {
                persistAuditRecord(record);
            } else {
                executeAsynchronously(() -> persistAuditRecord(record));
            }
            
            logger.info("System event audited. Type: {}, Message: {}", type, msg);
        } catch (Exception e) {
            logger.error("Failed to audit system event. Type: {}, Message: {}", eventType, message, e);
            
            // For system events, log locally if repository fails
            Map<String, Object> fallbackLog = new HashMap<>();
            fallbackLog.put("eventType", eventType);
            fallbackLog.put("message", message);
            fallbackLog.put("metadata", metadata);
            fallbackLog.put("errorMessage", e.getMessage());
            
            try {
                String fallbackJson = objectMapper.writeValueAsString(fallbackLog);
                logger.warn("FALLBACK SYSTEM EVENT AUDIT: {}", fallbackJson);
            } catch (JsonProcessingException jpe) {
                logger.error("Complete failure of system event auditing", jpe);
            }
        }
    }
    
    /**
     * Retrieves audit history for specific entity.
     * 
     * @param entityId The ID of the entity
     * @param entityType The type of the entity
     * @return List of audit records for the entity
     */
    public List<AuditRecord> getAuditHistory(Long entityId, String entityType) {
        if (entityId == null || entityType == null) {
            logger.warn("Cannot fetch audit history with null entityId or entityType");
            return Collections.emptyList();
        }
        
        try {
            List<AuditRecord> records = auditRepository.findByEntityIdAndEntityTypeOrderByTimestampDesc(
                entityId, entityType);
            
            logger.debug("Retrieved {} audit records for entity {} with ID {}", 
                    records.size(), entityType, entityId);
                    
            return records;
        } catch (Exception e) {
            logger.error("Failed to retrieve audit history for entity {} with ID {}", 
                    entityType, entityId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Retrieves user activity log for a specific date range.
     * 
     * @param userId The ID of the user
     * @param fromDate Start date for the activity log
     * @param toDate End date for the activity log
     * @return List of audit records for the user in the specified date range
     */
    public List<AuditRecord> getUserActivityLog(String userId, LocalDateTime fromDate, LocalDateTime toDate) {
        if (userId == null || fromDate == null || toDate == null) {
            logger.warn("Cannot fetch user activity log with null parameters");
            return Collections.emptyList();
        }
        
        if (toDate.isBefore(fromDate)) {
            logger.warn("Invalid date range: toDate is before fromDate");
            return Collections.emptyList();
        }
        
        // If the date range is too large, limit it to prevent performance issues
        LocalDateTime limitedToDate = fromDate.plusDays(31);
        if (toDate.isAfter(limitedToDate)) {
            logger.warn("Date range exceeds 31 days; limiting result set");
            toDate = limitedToDate;
        }
        
        try {
            List<AuditRecord> records = auditRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
                userId, fromDate, toDate);
            
            logger.info("Retrieved {} audit records for user {} between {} and {}", 
                    records.size(), userId, fromDate, toDate);
                    
            return records;
        } catch (Exception e) {
            logger.error("Failed to retrieve activity log for user {}", userId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Creates standardized audit record from input parameters.
     * 
     * @param action The action performed
     * @param entityType The type of entity
     * @param entityId The ID of the entity (can be null)
     * @param userId The ID of the user who performed the action
     * @param details Additional details about the action
     * @return Newly created audit record
     */
    public AuditRecord createAuditRecord(String action, String entityType, Long entityId, 
                                         String userId, Object details) {
        AuditRecord record = new AuditRecord();
        record.setAction(action);
        record.setEntityType(entityType);
        record.setEntityId(entityId);
        record.setUserId(userId);
        record.setTimestamp(LocalDateTime.now());
        
        // Convert details to JSON string
        if (details != null) {
            try {
                String detailsJson = serializeAuditData(details);
                record.setDetails(detailsJson);
            } catch (Exception e) {
                logger.error("Failed to serialize audit details", e);
                record.setDetails("{\"error\": \"Failed to serialize details\"}");
                // Add error flag to indicate serialization failure
                record.setHasErrors(true);
            }
        }
        
        return record;
    }
    
    /**
     * Persists audit record to database.
     * 
     * @param record The audit record to persist
     */
    public void persistAuditRecord(AuditRecord record) {
        if (record == null) {
            logger.warn("Cannot persist null audit record");
            return;
        }
        
        try {
            // Apply validation before saving
            validateAuditRecord(record);
            
            // Apply compliance policies if needed
            applyCompliancePolicy(record);
            
            // Save to repository
            auditRepository.save(record);
            
            logger.debug("Persisted audit record: {}", record);
        } catch (Exception e) {
            logger.error("Failed to persist audit record: {}", record, e);
            
            // For critical audit records, try emergency backup
            if (isCriticalAuditRecord(record)) {
                try {
                    emergencyAuditBackup(record);
                } catch (Exception be) {
                    logger.error("Emergency audit backup also failed", be);
                }
            }
        }
    }
    
    /**
     * Serializes audit data to JSON string.
     * 
     * @param data The data to serialize
     * @return JSON string representation of the data
     * @throws JsonProcessingException If serialization fails
     */
    public String serializeAuditData(Object data) throws JsonProcessingException {
        if (data == null) {
            return "{}";
        }
        
        try {
            // Special handling for different data types
            if (data instanceof String) {
                // Check if already JSON
                String strData = (String) data;
                if (strData.trim().startsWith("{") && strData.trim().endsWith("}")) {
                    try {
                        // Validate JSON format
                        objectMapper.readTree(strData);
                        return strData;
                    } catch (Exception e) {
                        // Not valid JSON, wrap in a JSON object
                        return "{\"value\": " + objectMapper.writeValueAsString(strData) + "}";
                    }
                } else {
                    return "{\"value\": " + objectMapper.writeValueAsString(strData) + "}";
                }
            } else if (data instanceof Map) {
                // Direct serialization for maps
                return objectMapper.writeValueAsString(data);
            } else {
                // For complex objects, use reflection to extract properties
                return objectMapper.writeValueAsString(data);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing audit data: {}", data, e);
            throw e;
        }
    }
    
    /**
     * Determines if an event is high priority and should be processed synchronously.
     * 
     * @param eventType The type of event
     * @return True if high priority, false otherwise
     */
    private boolean isHighPriorityEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        
        // List of high-priority event types that require immediate persistence
        return eventType.toUpperCase().contains("ERROR") ||
               eventType.toUpperCase().contains("SECURITY") ||
               eventType.toUpperCase().contains("AUTHENTICATION") ||
               eventType.toUpperCase().contains("AUTHORIZATION") ||
               eventType.toUpperCase().contains("CRITICAL");
    }
    
    /**
     * Validates audit record before persistence.
     * 
     * @param record The audit record to validate
     * @throws IllegalArgumentException If validation fails
     */
    private void validateAuditRecord(AuditRecord record) throws IllegalArgumentException {
        // Basic validation
        if (record.getAction() == null || record.getAction().trim().isEmpty()) {
            throw new IllegalArgumentException("Audit record action cannot be empty");
        }
        
        if (record.getUserId() == null || record.getUserId().trim().isEmpty()) {
            logger.warn("Audit record has no userId, defaulting to SYSTEM");
            record.setUserId("SYSTEM");
        }
        
        // Enforce maximum field lengths
        if (record.getAction().length() > 50) {
            record.setAction(record.getAction().substring(0, 50));
            logger.warn("Action field truncated to 50 characters");
        }
        
        if (record.getEntityType() != null && record.getEntityType().length() > 100) {
            record.setEntityType(record.getEntityType().substring(0, 100));
            logger.warn("EntityType field truncated to 100 characters");
        }
        
        // Details JSON should be valid
        if (record.getDetails() != null) {
            try {
                objectMapper.readTree(record.getDetails());
            } catch (Exception e) {
                logger.warn("Invalid JSON in details field: {}", e.getMessage());
                record.setDetails("{\"error\": \"Invalid JSON in original details\"}");
                record.setHasErrors(true);
            }
        }
    }
    
    /**
     * Applies compliance policies to audit records if needed.
     * 
     * @param record The audit record to process
     */
    private void applyCompliancePolicy(AuditRecord record) {
        // TODO: Implement data retention policies
        // TODO: Implement sensitive data masking
        // TODO: Implement regulatory compliance checks
        
        // Example implementation for masking sensitive data in details
        if (record.getDetails() != null && record.getDetails().toLowerCase().contains("password")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = objectMapper.readValue(record.getDetails(), Map.class);
                maskSensitiveData(detailsMap);
                record.setDetails(objectMapper.writeValueAsString(detailsMap));
            } catch (Exception e) {
                logger.warn("Could not apply sensitive data masking", e);
            }
        }
    }
    
    /**
     * Recursively masks sensitive data in maps.
     * 
     * @param map The map to process
     */
    private void maskSensitiveData(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains("password") || key.contains("secret") || 
                key.contains("token") || key.contains("key")) {
                entry.setValue("*****");
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                maskSensitiveData(nestedMap);
            }
        }
    }
    
    /**
     * Determines if an audit record is critical.
     * 
     * @param record The audit record to check
     * @return True if critical, false otherwise
     */
    private boolean isCriticalAuditRecord(AuditRecord record) {
        if (record == null) {
            return false;
        }
        
        String action = record.getAction();
        String entityType = record.getEntityType();
        
        // Define critical audit types
        return (action != null && (action.toUpperCase().contains("DELETE") || 
                                  action.toUpperCase().contains("SECURITY") ||
                                  action.toUpperCase().contains("AUTH"))) ||
               (entityType != null && (entityType.toUpperCase().contains("USER") ||
                                      entityType.toUpperCase().contains("PERMISSION") ||
                                      entityType.toUpperCase().contains("ROLE")));
    }
    
    /**
     * Emergency backup for critical audit records when normal persistence fails.
     * 
     * @param record The audit record to backup
     */
    private void emergencyAuditBackup(AuditRecord record) {
        try {
            // Convert to simple string format
            String backupData = String.format(
                "EMERGENCY_AUDIT_BACKUP|%s|%s|%s|%s|%s|%s",
                record.getTimestamp(),
                record.getAction(),
                record.getEntityType(),
                record.getEntityId(),
                record.getUserId(),
                record.getDetails() != null ? record.getDetails() : "{}"
            );
            
            // Log to separate audit log file
            logger.error("EMERGENCY_AUDIT_BACKUP: {}", backupData);
            
            // TODO: Implement file-based backup or alternative storage
        } catch (Exception e) {
            logger.error("Complete audit failure. Could not create emergency backup", e);
        }
    }
    
    /**
     * Executes a task asynchronously with the configured executor.
     * 
     * @param task The task to execute
     */
    private void executeAsynchronously(Runnable task) {
        try {
            CompletableFuture.supplyAsync((Supplier<Void>) () -> {
                task.run();
                return null;
            }, asyncExecutor).exceptionally(e -> {
                logger.error("Async audit operation failed", e);
                return null;
            });
        } catch (Exception e) {
            logger.error("Could not schedule async audit task", e);
            // Fall back to synchronous execution
            try {
                task.run();
            } catch (Exception fallbackEx) {
                logger.error("Fallback synchronous audit execution also failed", fallbackEx);
            }
        }
    }
}