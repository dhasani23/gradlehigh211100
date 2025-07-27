package com.gradlehigh211100.userservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gradlehigh211100.userservice.model.UserEntity;

/**
 * Repository interface for {@link UserEntity} that extends {@link JpaRepository}
 * Provides methods for user management, authentication, and complex filtering operations.
 * 
 * This repository handles all database operations related to users including:
 * - User authentication and lookup
 * - User status management (active, locked)
 * - Role-based user queries
 * - Email verification status
 * - Security-related queries (locked accounts, failed login attempts)
 * 
 * @author gradlehigh211100
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Find a user by their unique username
     * 
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Find a user by their email address
     * 
     * @param email the email to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Find a user by either username or email
     * This is commonly used for login functionality where users can authenticate with either
     * 
     * @param username the username to search for
     * @param email the email to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<UserEntity> findByUsernameOrEmail(String username, String email);
    
    /**
     * Find all users that are currently active
     * 
     * @return a List of all active users
     */
    List<UserEntity> findByActiveTrue();
    
    /**
     * Find all users that have a specific role
     * This assumes a many-to-many relationship between users and roles
     * 
     * @param roleName the name of the role to search for
     * @return a List of users that have the specified role
     */
    List<UserEntity> findByRoles_Name(String roleName);
    
    /**
     * Find all users with unverified email addresses
     * This is useful for administrative tasks or follow-up verification
     * 
     * @return a List of users with unverified emails
     */
    List<UserEntity> findByEmailVerifiedFalse();
    
    /**
     * Count the number of active users in the system
     * 
     * @return the count of active users
     */
    long countByActiveTrue();
    
    /**
     * Find locked accounts with failed login attempts above a threshold
     * This query can be used for security monitoring and administrative actions
     * 
     * @param attempts the threshold number of failed attempts
     * @return a List of locked user accounts exceeding the specified failed attempts
     */
    List<UserEntity> findByAccountLockedTrueAndFailedLoginAttemptsGreaterThan(int attempts);
    
    /**
     * Find users who have been inactive for a specified number of days
     * 
     * @param days number of days of inactivity
     * @return list of inactive users
     */
    @Query("SELECT u FROM UserEntity u WHERE u.lastLoginDate < CURRENT_DATE - :days")
    List<UserEntity> findInactiveUsers(@Param("days") int days);
    
    /**
     * Find users who registered but never verified their email within a time period
     * 
     * @param days number of days since registration
     * @return list of unverified users who registered within the specified time
     */
    @Query("SELECT u FROM UserEntity u WHERE u.emailVerified = false AND u.registrationDate < CURRENT_DATE - :days")
    List<UserEntity> findUnverifiedUsersByRegistrationDate(@Param("days") int days);
    
    /**
     * Find users by partial name match (case insensitive)
     * This performs a fuzzy search on user's name fields
     * 
     * @param namePattern pattern to match against user names
     * @return list of users matching the pattern
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :namePattern, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<UserEntity> findUsersByNamePattern(@Param("namePattern") String namePattern);
    
    /**
     * Complex query to find users based on multiple criteria
     * This demonstrates a more complex query with multiple conditions
     * 
     * @param active whether the user is active
     * @param locked whether the account is locked
     * @param roleNames collection of role names to match
     * @return list of users matching all criteria
     */
    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r " +
           "WHERE u.active = :active AND u.accountLocked = :locked " +
           "AND r.name IN :roleNames")
    List<UserEntity> findUsersByStatusAndRoles(
            @Param("active") boolean active,
            @Param("locked") boolean locked,
            @Param("roleNames") List<String> roleNames);
    
    /**
     * Find users whose password will expire within specified days
     * Used for sending password expiration notifications
     * 
     * @param days days until password expiration
     * @return list of users whose passwords will expire soon
     */
    @Query("SELECT u FROM UserEntity u WHERE u.passwordExpiryDate BETWEEN CURRENT_DATE AND CURRENT_DATE + :days")
    List<UserEntity> findUsersWithExpiringPasswords(@Param("days") int days);
    
    /**
     * Count users grouped by their registration month for the current year
     * Used for generating registration analytics
     * 
     * @return list of objects containing month and count
     */
    @Query(value = "SELECT MONTH(registration_date) as month, COUNT(*) as count " +
                   "FROM users " +
                   "WHERE YEAR(registration_date) = YEAR(CURRENT_DATE) " +
                   "GROUP BY MONTH(registration_date) " +
                   "ORDER BY month", 
           nativeQuery = true)
    List<Object[]> countUserRegistrationsByMonth();
}