package com.gradlehigh211100.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gradlehigh211100.userservice.model.UserPreference;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and manipulating UserPreference data
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    /**
     * Find all preferences for a specific user
     * 
     * @param userId the ID of the user
     * @return list of user preferences
     */
    List<UserPreference> findByUserId(Long userId);
    
    /**
     * Find a specific preference by user ID and preference key
     * 
     * @param userId the ID of the user
     * @param preferenceKey the preference key
     * @return optional containing the user preference if found
     */
    Optional<UserPreference> findByUserIdAndPreferenceKey(Long userId, String preferenceKey);
    
    /**
     * Find preferences by user ID and category
     * 
     * @param userId the ID of the user
     * @param category the preference category
     * @return list of user preferences in the specified category
     */
    List<UserPreference> findByUserIdAndCategory(Long userId, String category);
    
    /**
     * Delete a specific preference by user ID and preference key
     * 
     * @param userId the ID of the user
     * @param preferenceKey the preference key
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.userId = :userId AND up.preferenceKey = :preferenceKey")
    void deleteByUserIdAndPreferenceKey(Long userId, String preferenceKey);
    
    /**
     * Delete all preferences for a user in a specific category
     * 
     * @param userId the ID of the user
     * @param category the preference category
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.userId = :userId AND up.category = :category")
    void deleteByUserIdAndCategory(Long userId, String category);
    
    /**
     * Check if a preference with the given key exists for the user
     * 
     * @param userId the ID of the user
     * @param preferenceKey the preference key
     * @return true if the preference exists, false otherwise
     */
    boolean existsByUserIdAndPreferenceKey(Long userId, String preferenceKey);
    
    /**
     * Count preferences for a user in a specific category
     * 
     * @param userId the ID of the user
     * @param category the preference category
     * @return count of preferences
     */
    long countByUserIdAndCategory(Long userId, String category);
}