package com.gringotts.banking.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public User registerUser(User user) {
        // 1. Validation Check
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // 2. Hash the password
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. Force the Role
        user.setRole("ROLE_USER");

        // 4. Save to Database
        return userRepository.save(user);
    }

    // Used to find the user profile to edit
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Used to find user by their login name
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Used to save changes (like new profile picture or address)
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // Check if a phone number exists (for uniqueness checks)
    public boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

}