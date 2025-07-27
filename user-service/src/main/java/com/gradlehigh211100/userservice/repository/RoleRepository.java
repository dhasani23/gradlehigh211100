package com.gradlehigh211100.userservice.repository;

import com.gradlehigh211100.userservice.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for role entity management.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find a role by name
     *
     * @param name Role name to search for
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);
}