package com.gradlehigh211100.userservice.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing a user role in the system.
 * Roles are used for authorization and access control.
 */
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Role name cannot be empty")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @ManyToMany(mappedBy = "roles")
    private Set<UserEntity> users = new HashSet<>();

    /**
     * Default constructor required by JPA
     */
    public RoleEntity() {
        // Required empty constructor
    }

    /**
     * Constructor with role name
     *
     * @param name the name of the role
     */
    public RoleEntity(String name) {
        this.name = name;
    }

    /**
     * Constructor with role name and description
     *
     * @param name the name of the role
     * @param description the description of the role
     */
    public RoleEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the role ID
     * 
     * @return the role's unique identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the role ID
     * 
     * @param id the role's unique identifier
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the role name
     * 
     * @return the name of the role
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the role name
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the role description
     * 
     * @return the description of the role
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the role description
     * 
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the users associated with this role
     * 
     * @return set of users with this role
     */
    public Set<UserEntity> getUsers() {
        return users;
    }

    /**
     * Sets the users for this role
     * 
     * @param users the set of users to assign
     */
    public void setUsers(Set<UserEntity> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RoleEntity that = (RoleEntity) o;
        
        if (id != null) {
            return id.equals(that.id);
        }
        
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : 0, name);
    }

    @Override
    public String toString() {
        return "RoleEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}