package com.gradlehigh211100.userservice.repository;

import com.gradlehigh211100.userservice.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for user entity management.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * Find a user by username
     *
     * @param username Username to search for
     * @return Optional containing the user if found
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Find a user by email
     *
     * @param email Email to search for
     * @return Optional containing the user if found
     */
    Optional<UserEntity> findByEmail(String email);
}