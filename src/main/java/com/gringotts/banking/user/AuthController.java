package com.gringotts.banking.user;

import com.gringotts.banking.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for Authentication.
 * Handles Login and Token Generation.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * Authenticates a user and issues a JWT Token.
     * Endpoint: POST /api/auth/login
     * Flow:
     * 1. Check Credentials (Username/Email + Password) via AuthenticationManager.
     * 2. If valid, fetch full User entity.
     * 3. Generate JWT Token.
     * 4. Return Token + User Profile Info.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            // Input can be Username OR Email
            String input = request.get("username");
            String password = request.get("password");

            // 1. Authenticate (Checks DB hash vs Input)
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(input, password));

            // 2. Fetch User Details (Supports Username OR Email lookup)
            User user = userRepository.findByUsernameOrEmail(input, input).orElseThrow();

            // 3. Generate Token (Always use the canonical username for the token subject)
            String token = jwtUtil.generateToken(user.getUsername());

            // 4. Construct Response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());

            // Logic to determine if user needs to complete profile
            boolean needsProfile = (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty() ||
                    user.getAddress() == null || user.getAddress().isEmpty());
            response.put("needsProfile", needsProfile);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Credentials");
        }
    }

    /**
     * Handles Logout (Client-side mainly).
     * Since JWT is stateless, the server doesn't store session data.
     * This endpoint is mostly for frontend hooks or future blacklist implementation.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logged out successfully");
    }
}