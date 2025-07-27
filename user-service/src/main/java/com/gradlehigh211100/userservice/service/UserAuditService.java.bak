package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.repository.UserAuditRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for comprehensive audit trail management, security event logging, and compliance reporting.
 * This service handles all aspects of user audit including:
 * - User action logging
 * - Security event monitoring
 * - Login attempt tracking
 * - Compliance reporting data
 */
@Service
public class UserAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserAuditService.class);
    
    private final UserAuditRepository userAuditRepository;
    
    // Constants for event types
    private static final String EVENT_LOGIN = "LOGIN";
    private static final String EVENT_LOGOUT = "LOGOUT";
    private static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    private static final String EVENT_ACCOUNT_LOCK = "ACCOUNT_LOCK";
    private static final String EVENT_SECURITY_VIOLATION = "SECURITY_VIOLATION";
    
    @Autowired
    public UserAuditService(UserAuditRepository userAuditRepository) {
        this.userAuditRepository = userAuditRepository;
    }
    
    /**
     * Logs a user action with full context information
     *
     * @param user User entity performing the action
     * @param action The type of action performed
     * @param details Additional details about the action
     * @param success Whether the action was successful
     * @param ipAddress IP address from which the action was performed
     */
    public void logUserAction(UserEntity user, String action, String details, boolean success, String ipAddress) {
        // Input validation
        if (user == null) {
            logger.error("Cannot log user action: User entity is null");
            return;
        }
        
        if (action == null || action.trim().isEmpty()) {
            logger.error("Cannot log user action: Action is null or empty");
            return;
        }
        
        try {
            UserAuditEntity auditEntity = new UserAuditEntity();
            auditEntity.setUserId(user.getId());
            auditEntity.setUsername(user.getUsername());
            auditEntity.setAction(action);
            auditEntity.setDetails(details);
            auditEntity.setSuccess(success);
            auditEntity.setIpAddress(ipAddress);
            auditEntity.setTimestamp(LocalDateTime.now());
            
            // Additional context information
            if (user.getDepartment() != null) {
                auditEntity.setDepartment(user.getDepartment());
            }
            
            if (user.getRole() != null) {
                auditEntity.setUserRole(user.getRole().getName());
            }
            
            // Add session ID if available
            // FIXME: Implement session tracking mechanism
            
            // Save audit entry
            userAuditRepository.save(auditEntity);
            
            // Additional security checks for suspicious activities
            if (!success && EVENT_LOGIN.equals(action)) {
                checkForBruteForceAttempts(user.getUsername(), ipAddress);
            }
            
            logger.debug("User action logged: {} by {} ({})", action, user.getUsername(), success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            logger.error("Failed to log user action: {}", e.getMessage(), e);
            // TODO: Implement fallback logging mechanism
        }
    }
    
    /**
     * Retrieves complete audit history for a user
     *
     * @param userId ID of the user
     * @return List of audit entries for the user
     */
    public List<UserAuditEntity> getUserAuditHistory(Long userId) {
        if (userId == null) {
            logger.error("Cannot retrieve audit history: User ID is null");
            return Collections.emptyList();
        }
        
        try {
            List<UserAuditEntity> auditEntries = userAuditRepository.findByUserIdOrderByTimestampDesc(userId);
            
            // Process audit entries if needed
            if (auditEntries.isEmpty()) {
                logger.info("No audit history found for user ID: {}", userId);
            } else {
                logger.debug("Retrieved {} audit entries for user ID: {}", auditEntries.size(), userId);
            }
            
            return auditEntries;
        } catch (Exception e) {
            logger.error("Failed to retrieve user audit history: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Retrieves security-related events within date range
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return List of security-related audit entries
     */
    public List<UserAuditEntity> getSecurityEvents(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            logger.error("Cannot retrieve security events: Date range is incomplete");
            return Collections.emptyList();
        }
        
        if (endDate.isBefore(startDate)) {
            logger.error("Cannot retrieve security events: End date is before start date");
            return Collections.emptyList();
        }
        
        try {
            List<UserAuditEntity> allEvents = userAuditRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
            
            // Filter for security-related events
            List<UserAuditEntity> securityEvents = allEvents.stream()
                .filter(this::isSecurityEvent)
                .collect(Collectors.toList());
            
            // Categorize events by severity
            List<UserAuditEntity> criticalEvents = new ArrayList<>();
            List<UserAuditEntity> warningEvents = new ArrayList<>();
            List<UserAuditEntity> infoEvents = new ArrayList<>();
            
            for (UserAuditEntity event : securityEvents) {
                String action = event.getAction();
                boolean success = event.isSuccess();
                
                if (EVENT_SECURITY_VIOLATION.equals(action) || 
                    (EVENT_LOGIN.equals(action) && !success) || 
                    EVENT_ACCOUNT_LOCK.equals(action)) {
                    criticalEvents.add(event);
                } else if (EVENT_PASSWORD_CHANGE.equals(action) || 
                          (EVENT_LOGOUT.equals(action) && !success)) {
                    warningEvents.add(event);
                } else {
                    infoEvents.add(event);
                }
            }
            
            // Log statistics
            logger.info("Security events in period {}-{}: {} critical, {} warnings, {} info", 
                startDate, endDate, criticalEvents.size(), warningEvents.size(), infoEvents.size());
            
            // TODO: Implement alerting for critical security events
            
            return securityEvents;
        } catch (Exception e) {
            logger.error("Failed to retrieve security events: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets failed login attempts from IP within time window
     *
     * @param ipAddress IP address to check
     * @param timeWindow Time window to check
     * @return List of failed login attempts
     */
    public List<UserAuditEntity> getFailedLoginAttempts(String ipAddress, LocalDateTime timeWindow) {
        if (StringUtils.isEmpty(ipAddress)) {
            logger.error("Cannot retrieve failed login attempts: IP address is null or empty");
            return Collections.emptyList();
        }
        
        if (timeWindow == null) {
            logger.error("Cannot retrieve failed login attempts: Time window is null");
            return Collections.emptyList();
        }
        
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            
            List<UserAuditEntity> failedAttempts = userAuditRepository.findByIpAddressAndTimestampAfterAndSuccessFalseAndActionOrderByTimestampDesc(
                ipAddress, timeWindow, EVENT_LOGIN);
            
            if (failedAttempts.size() > 3) {
                // If there are multiple failed attempts, check for patterns
                analyzeFailedLoginPattern(failedAttempts, ipAddress);
            }
            
            return failedAttempts;
        } catch (Exception e) {
            logger.error("Failed to retrieve failed login attempts: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Logs system-level security events
     *
     * @param eventType Type of security event
     * @param details Additional details about the event
     * @param ipAddress IP address related to the event
     */
    public void logSecurityEvent(String eventType, String details, String ipAddress) {
        if (eventType == null || eventType.trim().isEmpty()) {
            logger.error("Cannot log security event: Event type is null or empty");
            return;
        }
        
        try {
            UserAuditEntity auditEntity = new UserAuditEntity();
            auditEntity.setAction(eventType);
            auditEntity.setDetails(details);
            auditEntity.setIpAddress(ipAddress);
            auditEntity.setTimestamp(LocalDateTime.now());
            auditEntity.setSystemEvent(true);
            
            // Determine severity
            SecurityEventSeverity severity = determineSeverity(eventType, details);
            auditEntity.setSeverity(severity.name());
            
            // For high severity events, log additional context
            if (severity == SecurityEventSeverity.CRITICAL) {
                // FIXME: Add system environment information for forensics
                augmentWithSystemContext(auditEntity);
                
                // Trigger immediate notification
                // TODO: Implement notification system for critical security events
            }
            
            userAuditRepository.save(auditEntity);
            
            // Log based on severity
            switch (severity) {
                case CRITICAL:
                    logger.error("SECURITY EVENT [CRITICAL]: {} - {}", eventType, details);
                    break;
                case HIGH:
                    logger.warn("SECURITY EVENT [HIGH]: {} - {}", eventType, details);
                    break;
                case MEDIUM:
                    logger.warn("SECURITY EVENT [MEDIUM]: {} - {}", eventType, details);
                    break;
                case LOW:
                    logger.info("SECURITY EVENT [LOW]: {} - {}", eventType, details);
                    break;
                default:
                    logger.info("SECURITY EVENT: {} - {}", eventType, details);
            }
            
        } catch (Exception e) {
            logger.error("Failed to log security event: {}", e.getMessage(), e);
            // Attempt emergency logging
            logger.error("Emergency security log - Event: {}, Details: {}, IP: {}", eventType, details, ipAddress);
        }
    }
    
    /**
     * Analyzes patterns in failed login attempts to detect potential attacks
     *
     * @param failedAttempts List of failed login attempts
     * @param ipAddress IP address being analyzed
     */
    private void analyzeFailedLoginPattern(List<UserAuditEntity> failedAttempts, String ipAddress) {
        // Check for rapid succession of attempts (potential brute force)
        if (failedAttempts.size() >= 5) {
            long timeDiffMillis = failedAttempts.get(0).getTimestamp().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() -
                                 failedAttempts.get(4).getTimestamp().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            
            // If 5 attempts within 2 minutes
            if (timeDiffMillis < 120000) {
                logSecurityEvent(EVENT_SECURITY_VIOLATION, 
                               "Potential brute force attack detected: 5 failed logins within " + 
                               (timeDiffMillis / 1000) + " seconds", ipAddress);
                
                // TODO: Implement IP blocking mechanism
                // blockIpAddress(ipAddress, Duration.ofMinutes(10));
            }
        }
        
        // Check for attempts across multiple usernames (credential stuffing)
        List<String> uniqueUsernames = failedAttempts.stream()
            .map(UserAuditEntity::getUsername)
            .distinct()
            .collect(Collectors.toList());
        
        if (uniqueUsernames.size() >= 3) {
            logSecurityEvent(EVENT_SECURITY_VIOLATION, 
                           "Potential credential stuffing attack: Failed login attempts for " + 
                           uniqueUsernames.size() + " different users from same IP", ipAddress);
            
            // TODO: Implement advanced threat analysis
            // scheduleInDepthAnalysis(ipAddress);
        }
    }
    
    /**
     * Checks for potential brute force attempts
     *
     * @param username Username targeted
     * @param ipAddress IP address from which attempts are made
     */
    private void checkForBruteForceAttempts(String username, String ipAddress) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(10);
        
        // Check username-based attempts
        if (!StringUtils.isEmpty(username)) {
            List<UserAuditEntity> userFailedAttempts = userAuditRepository
                .findByUsernameAndTimestampAfterAndSuccessFalseAndActionOrderByTimestampDesc(
                    username, timeWindow, EVENT_LOGIN);
            
            if (userFailedAttempts.size() >= 5) {
                logSecurityEvent(EVENT_ACCOUNT_LOCK,
                               "Account temporarily locked due to 5+ failed login attempts: " + username,
                               ipAddress);
                
                // TODO: Implement account locking mechanism
                // userLockingService.lockAccount(username, Duration.ofMinutes(15));
            }
        }
        
        // Check IP-based attempts regardless of username
        List<UserAuditEntity> ipFailedAttempts = userAuditRepository
            .findByIpAddressAndTimestampAfterAndSuccessFalseAndActionOrderByTimestampDesc(
                ipAddress, timeWindow, EVENT_LOGIN);
        
        if (ipFailedAttempts.size() >= 10) {
            logSecurityEvent(EVENT_SECURITY_VIOLATION,
                           "Multiple failed login attempts (10+) from IP address: " + ipAddress,
                           ipAddress);
            
            // TODO: Implement IP throttling
            // securityService.throttleIpRequests(ipAddress, Duration.ofMinutes(15));
        }
    }
    
    /**
     * Determines if an audit entry represents a security event
     *
     * @param auditEntity The audit entry to check
     * @return true if it's a security event
     */
    private boolean isSecurityEvent(UserAuditEntity auditEntity) {
        if (auditEntity == null) {
            return false;
        }
        
        String action = auditEntity.getAction();
        if (action == null) {
            return false;
        }
        
        // Check if it's an explicitly defined security event
        if (EVENT_SECURITY_VIOLATION.equals(action) || 
            EVENT_ACCOUNT_LOCK.equals(action)) {
            return true;
        }
        
        // Failed logins are security events
        if (EVENT_LOGIN.equals(action) && !auditEntity.isSuccess()) {
            return true;
        }
        
        // Password changes are security events
        if (EVENT_PASSWORD_CHANGE.equals(action)) {
            return true;
        }
        
        // System events marked as security
        if (auditEntity.isSystemEvent() && 
            auditEntity.getSeverity() != null &&
            (auditEntity.getSeverity().equals(SecurityEventSeverity.CRITICAL.name()) ||
             auditEntity.getSeverity().equals(SecurityEventSeverity.HIGH.name()))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Determines the severity of a security event
     *
     * @param eventType Type of event
     * @param details Details of the event
     * @return Severity level
     */
    private SecurityEventSeverity determineSeverity(String eventType, String details) {
        if (EVENT_SECURITY_VIOLATION.equals(eventType)) {
            return SecurityEventSeverity.CRITICAL;
        }
        
        if (EVENT_ACCOUNT_LOCK.equals(eventType)) {
            return SecurityEventSeverity.HIGH;
        }
        
        if (EVENT_LOGIN.equals(eventType) && details != null && details.contains("failed")) {
            if (details.contains("admin") || details.contains("root")) {
                return SecurityEventSeverity.HIGH;
            }
            return SecurityEventSeverity.MEDIUM;
        }
        
        if (EVENT_PASSWORD_CHANGE.equals(eventType)) {
            if (details != null && 
                (details.contains("admin") || details.contains("forced") || details.contains("reset"))) {
                return SecurityEventSeverity.HIGH;
            }
            return SecurityEventSeverity.MEDIUM;
        }
        
        // Default
        return SecurityEventSeverity.LOW;
    }
    
    /**
     * Adds additional system context to a security event
     *
     * @param auditEntity The audit entity to augment
     */
    private void augmentWithSystemContext(UserAuditEntity auditEntity) {
        StringBuilder contextBuilder = new StringBuilder();
        
        // Append original details
        if (auditEntity.getDetails() != null) {
            contextBuilder.append(auditEntity.getDetails()).append(" | ");
        }
        
        contextBuilder.append("System context: ");
        
        // Add timestamp details
        contextBuilder.append("Time: ")
            .append(auditEntity.getTimestamp())
            .append(", ");
        
        // Add environment info if available
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            contextBuilder.append("OS: ").append(osName).append(" ").append(osVersion);
            
            // Add application version
            // TODO: Implement application version retrieval
            // contextBuilder.append(", App version: ").append(appVersion);
        } catch (Exception e) {
            logger.error("Failed to retrieve system properties", e);
            contextBuilder.append("System properties unavailable");
        }
        
        auditEntity.setDetails(contextBuilder.toString());
    }
    
    /**
     * Enum for security event severity levels
     */
    private enum SecurityEventSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}