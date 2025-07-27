package com.gradlehigh211100.userservice.controller;

import com.gradlehigh211100.userservice.dto.UserDto;
import com.gradlehigh211100.userservice.dto.UserRegistrationDto;
import com.gradlehigh211100.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

/**
 * REST Controller for user management operations.
 * Provides endpoints for user registration, profile management, and administration.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user from registration data
     * 
     * @param userRegistrationDto The registration data
     * @return The created user DTO
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegistrationDto userRegistrationDto) {
        UserDto createdUser = userService.createUser(userRegistrationDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Gets a user by ID
     * 
     * @param id The user ID
     * @return The user DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("@userSecurity.isUserAllowed(authentication, #id)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        Optional<UserDto> userDto = userService.findById(id);
        return userDto.map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Updates a user by ID
     * 
     * @param id The user ID
     * @param userDto The updated user data
     * @return The updated user DTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("@userSecurity.isUserAllowed(authentication, #id)")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        if (!userService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        UserDto updatedUser = userService.updateUser(id, userDto);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    /**
     * Deletes a user by ID
     * 
     * @param id The user ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deactivates a user by ID
     * 
     * @param id The user ID
     * @return No content
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        userService.deactivateUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}