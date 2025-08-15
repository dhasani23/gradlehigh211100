package com.gradlehigh211100.userservice.service;

import com.gradlehigh211100.userservice.dto.UserDto;
import com.gradlehigh211100.userservice.dto.UserRegistrationDto;
import com.gradlehigh211100.userservice.model.Role;
import com.gradlehigh211100.userservice.model.UserEntity;
import com.gradlehigh211100.userservice.repository.RoleRepository;
import com.gradlehigh211100.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of UserService interface that also serves as a UserDetailsService
 * for Spring Security authentication.
 */
@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security UserDetailsService method for loading user by username.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Create Spring Security User from our UserEntity
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().toUpperCase()));
        }
        
        return new User(user.getUsername(), user.getPassword(), 
                user.isActive(), user.isAccountNonExpired(), 
                user.isCredentialsNonExpired(), user.isAccountNonLocked(), 
                authorities);
    }

    /**
     * Creates a new user from registration data.
     */
    @Override
    @Transactional
    public UserDto createUser(UserRegistrationDto registrationDto) {
        // Check if username or email already exists
        if (userRepository.findByUsername(registrationDto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.findByEmail(registrationDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user entity
        UserEntity user = new UserEntity();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        
        // Assign default role
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRole(defaultRole);
        
        // Set default values
        user.setActive(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setCreatedDate(LocalDateTime.now());
        user.setLastPasswordChangeDate(LocalDateTime.now());
        
        // Save user
        UserEntity savedUser = userRepository.save(user);
        
        // Convert to DTO and return
        return convertToDto(savedUser);
    }

    /**
     * Finds a user by ID.
     */
    @Override
    public Optional<UserDto> findById(Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        return userOpt.map(this::convertToDto);
    }

    /**
     * Updates user information.
     */
    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if email already exists for another user
        Optional<UserEntity> existingUser = userRepository.findByEmail(userDto.getEmail());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
            throw new RuntimeException("Email already exists");
        }
        
        // Update user properties
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setDepartment(userDto.getDepartment());
        user.setPhone(userDto.getPhone());
        
        // Update user
        UserEntity updatedUser = userRepository.save(user);
        
        return convertToDto(updatedUser);
    }

    /**
     * Deletes a user permanently.
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        userRepository.delete(user);
    }

    /**
     * Deactivates a user (soft delete).
     */
    @Override
    @Transactional
    public void deactivateUser(Long id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Checks if a user with the given ID exists.
     */
    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    /**
     * Checks if a user with the given email exists.
     */
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Checks if a user with the given email exists and has a different ID.
     */
    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        Optional<UserEntity> user = userRepository.findByEmail(email);
        return user.isPresent() && !user.get().getId().equals(id);
    }

    /**
     * Gets the type of user (e.g., FREE, PREMIUM).
     */
    @Override
    public String getUserType(Long id) {
        // In a real application, this might come from a subscription service
        return "FREE";
    }

    /**
     * Checks if user has active transactions.
     */
    @Override
    public boolean hasActiveTransactions(Long id) {
        // In a real application, this would check the transaction service
        return false;
    }

    /**
     * Checks if user has been inactive longer than specified days.
     */
    @Override
    public boolean isInactiveLongerThan(Long id, int days) {
        // In a real application, this would check the last login date
        return false;
    }

    /**
     * Checks if user has requested data removal (GDPR).
     */
    @Override
    public boolean hasRequestedDataRemoval(Long id) {
        // In a real application, this would check a data removal request table
        return false;
    }

    /**
     * Checks if user is a test account.
     */
    @Override
    public boolean isTestAccount(Long id) {
        // In a real application, this might check for a specific flag or email domain
        return false;
    }

    /**
     * Gets a list of all available roles in the system.
     */
    @Override
    public List<String> getAvailableRoles() {
        return roleRepository.findAll()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    /**
     * Checks if user has a specific role.
     */
    @Override
    public boolean userHasRole(Long id, String roleName) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || userOpt.get().getRole() == null) {
            return false;
        }
        
        return userOpt.get().getRole().getName().equals(roleName);
    }

    /**
     * Assigns a role to a user.
     */
    @Override
    @Transactional
    public void assignRoleToUser(Long id, String roleName) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.setRole(role);
        userRepository.save(user);
    }

    /**
     * Converts a UserEntity to UserDto.
     */
    private UserDto convertToDto(UserEntity user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDepartment(user.getDepartment());
        dto.setPhone(user.getPhone());
        dto.setActive(user.isActive());
        
        if (user.getRole() != null) {
            dto.setRole(user.getRole().getName());
        }
        
        return dto;
    }
}