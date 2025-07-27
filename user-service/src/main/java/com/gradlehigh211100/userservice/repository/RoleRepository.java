package com.gradlehigh211100.userservice.repository;

import com.gradlehigh211100.userservice.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for role entity operations.
 * Provides methods for role hierarchy management and permission queries.
 * 
 * This repository handles complex role-based operations including:
 * - Finding roles by name
 * - Retrieving active roles
 * - Managing role hierarchies through level-based queries
 * - Permission-based role lookups
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    /**
     * Finds a role by its unique name.
     * 
     * @param name the role name to search for
     * @return an Optional containing the role if found, empty otherwise
     */
    Optional<RoleEntity> findByName(String name);
    
    /**
     * Retrieves all active roles in the system.
     * 
     * @return a list of active roles
     */
    List<RoleEntity> findByActiveTrue();
    
    /**
     * Finds roles with level less than or equal to the specified value.
     * This method is used for hierarchical role management where lower levels
     * typically have more privileges.
     * 
     * @param level the maximum role level to filter by
     * @return list of roles at or below the specified level
     */
    List<RoleEntity> findByLevelLessThanEqual(Integer level);
    
    /**
     * Finds roles that contain a specific permission.
     * Uses a JPQL query with MEMBER OF clause to check permission existence.
     * 
     * @param permission the permission to search for
     * @return list of roles containing the specified permission
     */
    @Query("SELECT r FROM RoleEntity r WHERE :permission MEMBER OF r.permissions")
    List<RoleEntity> findByPermissionsContaining(@Param("permission") String permission);
    
    /**
     * Alternative implementation for finding roles with a specific permission.
     * This method uses a different query approach for databases that might not support MEMBER OF.
     * 
     * FIXME: Evaluate performance of both permission query methods and standardize on one approach
     */
    @Query("SELECT r FROM RoleEntity r JOIN r.permissions p WHERE p = :permission")
    List<RoleEntity> findByPermissionAlternative(@Param("permission") String permission);
    
    /**
     * Finds roles that have all permissions in the provided list.
     * 
     * @param permissions list of permissions to search for
     * @return list of roles that have all the specified permissions
     * @throws IllegalArgumentException if permissions list is null or empty
     */
    @Query("SELECT r FROM RoleEntity r WHERE r.permissions IN :permissions GROUP BY r HAVING COUNT(r) = :count")
    List<RoleEntity> findByAllPermissions(@Param("permissions") List<String> permissions, @Param("count") Long count);
    
    /**
     * Finds roles at a specific hierarchy level ordered by name.
     * 
     * @param level the exact role level to filter by
     * @return ordered list of roles at the specified level
     */
    List<RoleEntity> findByLevelOrderByNameAsc(Integer level);
    
    /**
     * TODO: Implement method to find roles by partial name match
     * This should use LIKE operator for case-insensitive partial matching
     */
    
    /**
     * Counts the number of roles that have the specified permission.
     * 
     * @param permission the permission to count roles for
     * @return the count of roles with the specified permission
     */
    @Query("SELECT COUNT(r) FROM RoleEntity r WHERE :permission MEMBER OF r.permissions")
    Long countRolesByPermission(@Param("permission") String permission);
    
    /**
     * Checks if any active roles exist with the given permission.
     * 
     * @param permission the permission to check for
     * @return true if at least one active role with the permission exists
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RoleEntity r " +
           "WHERE r.active = true AND :permission MEMBER OF r.permissions")
    boolean existsActiveRoleWithPermission(@Param("permission") String permission);
}