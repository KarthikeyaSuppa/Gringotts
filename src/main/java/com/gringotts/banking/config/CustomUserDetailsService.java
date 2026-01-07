package com.gringotts.banking.config;

import com.gringotts.banking.user.User;
import com.gringotts.banking.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Bridges the gap between our Custom Database 'User' and Spring Security's 'UserDetails'.
 * Used during the Login process to fetch user credentials.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads a user by Username OR Email.
     * Flow: AuthController -> AuthenticationManager -> This Method -> DB.
     *
     * @param input The username or email provided in the login form.
     * @return A Spring Security UserDetails object.
     * @throws UsernameNotFoundException if no user matches.
     */
    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // Check both Username AND Email columns
        // WHERE username = input OR email = input
        User user = userRepository.findByUsernameOrEmail(input, input)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + input));

        // Convert our User entity to a Spring Security compatible User
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // Always use the unique username as the principal
                user.getPassword(), // The hashed password from DB
                new ArrayList<>()   // Authorities/Roles (Empty for now)
        );
    }
}