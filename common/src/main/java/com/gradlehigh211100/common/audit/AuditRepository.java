package com.gradlehigh211100.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing {@link AuditRecord} entities.
 * Provides methods for saving and retrieving audit records.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {
    
    /**
     * Finds audit records for a specific entity, ordered by timestamp descending.
     *
     * @param entityId The ID of the entity
     * @param entityType The type of the entity
     * @return List of audit records for the entity
     */
    List<AuditRecord> findByEntityIdAndEntityTypeOrderByTimestampDesc(Long entityId, String entityType);
    
    /**
     * Finds audit records for a specific user within a date range, ordered by timestamp descending.
     *
     * @param userId The ID of the user
     * @param fromDate Start date for the search
     * @param toDate End date for the search
     * @return List of audit records for the user in the specified date range
     */
    List<AuditRecord> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
            String userId, LocalDateTime fromDate, LocalDateTime toDate);
    
    /**
     * Finds all audit records for a specific action type.
     *
     * @param action The action to search for
     * @return List of audit records for the specified action
     */
    List<AuditRecord> findByAction(String action);
    
    /**
     * Finds the most recent audit records, limited by count.
     *
     * @param limit Maximum number of records to return
     * @return List of the most recent audit records
     */
    List<AuditRecord> findTop10ByOrderByTimestampDesc();
    
    /**
     * Finds audit records with errors.
     *
     * @return List of audit records that have errors
     */
    List<AuditRecord> findByHasErrorsTrue();
}