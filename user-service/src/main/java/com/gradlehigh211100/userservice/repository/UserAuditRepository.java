package com.gradlehigh211100.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gradlehigh211100.userservice.model.UserEntity;
import com.gradlehigh211100.userservice.model.UserAuditEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for user audit trail storage with comprehensive querying capabilities for security analysis.
 * This repository provides methods for tracking and analyzing user activity within the system,
 * with particular focus on security-related events and suspicious activities.
 * 
 * @author gradlehigh211100
 */
@Repository
public interface UserAuditRepository extends JpaRepository<UserAuditEntity, Long> {

    /**
     * Finds all audit records for a specific user
     *
     * @param user The user entity to find audit records for
     * @return List of audit records associated with the specified user
     */
    List<UserAuditEntity> findByUser(UserEntity user);
    
    /**
     * Finds audit records for a user and specific action
     *
     * @param user The user entity to find audit records for
     * @param action The specific action to filter by
     * @return List of audit records matching both user and action
     */
    List<UserAuditEntity> findByUserAndAction(UserEntity user, String action);
    
    /**
     * Finds audit records by action within a date range
     * Useful for analyzing specific actions during security incidents
     *
     * @param action The specific action to filter by
     * @param startDate Beginning of the date range
     * @param endDate End of the date range
     * @return List of audit records matching the criteria
     */
    List<UserAuditEntity> findByActionAndTimestampBetween(String action, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds all failed audit records for security analysis
     * This is particularly useful for detecting potential security breaches or brute force attempts
     *
     * @return List of failed audit records
     */
    List<UserAuditEntity> findBySuccessFalse();
    
    /**
     * Finds all audit records from a specific IP address
     * Critical for tracking suspicious activity from particular network locations
     *
     * @param ipAddress The IP address to find audit records for
     * @return List of audit records from the specified IP address
     */
    List<UserAuditEntity> findByIpAddress(String ipAddress);
    
    /**
     * Finds suspicious login attempts (multiple failed logins within a short time period)
     * Used to detect potential brute force attacks
     *
     * @param ipAddress The IP address to check
     * @param timeWindow The time window to consider for suspicious activity
     * @param failureThreshold The number of failures that constitutes suspicious activity
     * @return Count of failed login attempts matching the criteria
     */
    @Query("SELECT COUNT(ua) FROM UserAuditEntity ua WHERE ua.ipAddress = :ipAddress " +
           "AND ua.action = 'LOGIN' AND ua.success = false " +
           "AND ua.timestamp >= :timeWindow")
    long countFailedLoginAttempts(@Param("ipAddress") String ipAddress, 
                                 @Param("timeWindow") LocalDateTime timeWindow);
    
    /**
     * Find recent activity for a given user
     *
     * @param user The user to find recent activity for
     * @param limit Maximum number of records to return
     * @return List of most recent audit records for the user
     */
    @Query("SELECT ua FROM UserAuditEntity ua WHERE ua.user = :user " +
           "ORDER BY ua.timestamp DESC")
    List<UserAuditEntity> findRecentActivityByUser(@Param("user") UserEntity user, 
                                                 @Param("limit") int limit);
    
    /**
     * Find suspicious patterns of access across multiple accounts from the same IP
     *
     * @param ipAddress The IP address to investigate
     * @param timeWindow The time period to analyze
     * @return List of user audit entries showing potential account hopping
     */
    @Query("SELECT DISTINCT ua FROM UserAuditEntity ua " +
           "WHERE ua.ipAddress = :ipAddress AND ua.timestamp >= :timeWindow " +
           "ORDER BY ua.userId, ua.timestamp")
    List<UserAuditEntity> findPotentialAccountHopping(@Param("ipAddress") String ipAddress,
                                                    @Param("timeWindow") LocalDateTime timeWindow);
    
    /**
     * Find all actions performed on a specific resource
     * 
     * @param resourceId The identifier of the resource being accessed
     * @return List of audit records involving the specified resource
     */
    @Query("SELECT ua FROM UserAuditEntity ua WHERE ua.resourceId = :resourceId")
    List<UserAuditEntity> findActionsByResourceId(@Param("resourceId") String resourceId);
    
    /**
     * Find unusual access patterns by time of day
     * Useful for detecting off-hours access that might indicate compromised accounts
     *
     * @param user The user to analyze
     * @param startHour Beginning hour of the time range (0-23)
     * @param endHour Ending hour of the time range (0-23)
     * @return List of audit activities during unusual hours
     */
    @Query("SELECT ua FROM UserAuditEntity ua " +
           "WHERE ua.user = :user AND " +
           "(FUNCTION('HOUR', ua.timestamp) >= :startHour AND " +
           "FUNCTION('HOUR', ua.timestamp) <= :endHour)")
    List<UserAuditEntity> findAccessDuringHours(@Param("user") UserEntity user, 
                                              @Param("startHour") int startHour, 
                                              @Param("endHour") int endHour);
    /**
     * Find audit records by user ID ordered by timestamp
     *
     * @param userId The ID of the user
     * @return List of audit records ordered by timestamp desc
     */
    List<UserAuditEntity> findByUserIdOrderByTimestampDesc(Long userId);
    
    /**
     * Find audit records between two timestamps
     *
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of audit records ordered by timestamp desc
     */
    List<UserAuditEntity> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find failed login attempts from an IP address after a specific time
     *
     * @param ipAddress The IP address
     * @param timestamp Timestamp after which to look for attempts
     * @param action The action type (typically "LOGIN")
     * @return List of failed login attempts
     */
    List<UserAuditEntity> findByIpAddressAndTimestampAfterAndSuccessFalseAndActionOrderByTimestampDesc(
            String ipAddress, LocalDateTime timestamp, String action);
    
    /**
     * Find failed login attempts for a username after a specific time
     *
     * @param username The username
     * @param timestamp Timestamp after which to look for attempts
     * @param action The action type (typically "LOGIN")
     * @return List of failed login attempts
     */
    List<UserAuditEntity> findByUsernameAndTimestampAfterAndSuccessFalseAndActionOrderByTimestampDesc(
            String username, LocalDateTime timestamp, String action);
}