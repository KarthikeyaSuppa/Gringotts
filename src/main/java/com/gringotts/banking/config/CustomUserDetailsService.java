package com.gringotts.banking.config;

import com.gringotts.banking.user.User;
import com.gringotts.banking.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // ✅ CHANGED: Check both Username AND Email columns
        // We pass 'input' to both parameters. SQL will look like:
        // WHERE username = input OR email = input
        User user = userRepository.findByUsernameOrEmail(input, input)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + input));


        // 2. Wrap it in a Spring Security "User" object
        // Why? Because Spring Security doesn't know what your "User" class looks like.
        // It needs this specific standard object.
        // Convert our User to Spring Security's UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>() // Authorities (Roles) - Empty for now
        );
    }
}