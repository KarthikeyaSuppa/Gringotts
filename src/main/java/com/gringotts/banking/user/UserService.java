package com.gringotts.banking.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Business Logic for User Management.
 * Handles Registration, Profile Updates, and User Lookup.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor Injection (Best Practice)
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user in the system.
     * Flow: Controller -> UserService -> UserRepository -> DB
     * 1. Checks if username/email exists.
     * 2. Hashes the password using BCrypt.
     * 3. Sets default role.
     * 4. Saves to database.
     *
     * @param user The raw user object from the frontend
     * @return The saved User entity
     */
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setRole("ROLE_USER");

        return userRepository.save(user);
    }

    /**
     * Finds a user by ID.
     * Used by: Profile update logic.
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Finds a user by Username (Returns Optional).
     * Used by: Internal checks.
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Finds a user by Username (Returns Entity).
     * Used by: AuthController and Profile Endpoints.
     * Throws exception if not found.
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    /**
     * Updates an existing user.
     * Flow: Controller -> Service -> DB Update
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        return userRepository.existsByPhoneNumber(phoneNumber);
    }
}