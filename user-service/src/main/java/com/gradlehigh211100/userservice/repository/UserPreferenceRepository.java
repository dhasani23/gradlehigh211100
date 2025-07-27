package com.gradlehigh211100.userservice.repository;

import com.gradlehigh211100.userservice.entity.UserEntity;
import com.gradlehigh211100.userservice.entity.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing user preferences.
 * 
 * This repository provides functionality for user preference management with 
 * category-based and user-specific queries. It extends JpaRepository to leverage
 * Spring Data JPA capabilities.
 * 
 * @since 1.0
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, Long> {
    
    /**
     * Finds all preferences for a specific user.
     * 
     * @param user the user entity whose preferences to retrieve
     * @return list of preference entities belonging to the user
     */
    List<UserPreferenceEntity> findByUser(UserEntity user);
    
    /**
     * Finds a specific preference by user and key.
     * 
     * @param user the user entity
     * @param preferenceKey the preference key to search for
     * @return an Optional containing the preference entity if found
     */
    Optional<UserPreferenceEntity> findByUserAndPreferenceKey(UserEntity user, String preferenceKey);
    
    /**
     * Finds all preferences for a user in a specific category.
     * 
     * @param user the user entity
     * @param category the category to filter by
     * @return list of preference entities matching the user and category
     */
    List<UserPreferenceEntity> findByUserAndCategory(UserEntity user, String category);
    
    /**
     * Deletes a specific preference by user and key.
     * 
     * @param user the user entity
     * @param preferenceKey the preference key to delete
     */
    @Modifying
    @Transactional
    void deleteByUserAndPreferenceKey(UserEntity user, String preferenceKey);
    
    /**
     * Custom query to find preferences with partial key match.
     * 
     * @param user the user entity
     * @param keyPattern the pattern to match against preference keys
     * @return list of preference entities matching the pattern
     */
    @Query("SELECT p FROM UserPreferenceEntity p WHERE p.user = :user AND p.preferenceKey LIKE %:keyPattern%")
    List<UserPreferenceEntity> findByUserAndPartialKey(@Param("user") UserEntity user, @Param("keyPattern") String keyPattern);
    
    /**
     * Finds preferences by user and value containing a specific string.
     * 
     * @param user the user entity
     * @param valueFragment fragment to search for in preference values
     * @return list of preferences with values containing the fragment
     */
    @Query("SELECT p FROM UserPreferenceEntity p WHERE p.user = :user AND p.preferenceValue LIKE %:valueFragment%")
    List<UserPreferenceEntity> findByUserAndValueContaining(@Param("user") UserEntity user, @Param("valueFragment") String valueFragment);
    
    /**
     * Counts preferences by category for a specific user.
     * 
     * @param user the user entity
     * @return list of categories with their respective counts
     */
    @Query("SELECT p.category, COUNT(p) FROM UserPreferenceEntity p WHERE p.user = :user GROUP BY p.category")
    List<Object[]> countPreferencesByCategory(@Param("user") UserEntity user);
    
    /**
     * Bulk updates preferences in a specific category.
     * 
     * @param user the user entity
     * @param category the category to update
     * @param active the new active status
     * @return number of records updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserPreferenceEntity p SET p.active = :active WHERE p.user = :user AND p.category = :category")
    int bulkUpdateActiveStatusByCategory(@Param("user") UserEntity user, @Param("category") String category, @Param("active") boolean active);
    
    /**
     * Deletes all preferences for a user in a specific category.
     * 
     * @param user the user entity
     * @param category the category to delete preferences from
     * @return number of records deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserPreferenceEntity p WHERE p.user = :user AND p.category = :category")
    int deleteAllByUserAndCategory(@Param("user") UserEntity user, @Param("category") String category);
    
    /**
     * Finds all distinct categories for a specific user.
     * 
     * @param user the user entity
     * @return list of distinct category names
     */
    @Query("SELECT DISTINCT p.category FROM UserPreferenceEntity p WHERE p.user = :user")
    List<String> findDistinctCategoriesByUser(@Param("user") UserEntity user);
    
    /**
     * Checks if a user has any preferences in a specific category.
     * 
     * @param user the user entity
     * @param category the category to check
     * @return true if at least one preference exists, false otherwise
     */
    boolean existsByUserAndCategory(UserEntity user, String category);
}