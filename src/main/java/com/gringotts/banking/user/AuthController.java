package com.gringotts.banking.user;

import com.gringotts.banking.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // Add this line!
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            // 'input' could be username OR email
            String input = request.get("username");
            String password = request.get("password");

            // 1. Authenticate (Uses CustomUserDetailsService -> findByUsernameOrEmail)
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(input, password));

            // 2. Fetch User Details (Use the same logic: find by username OR email)
            User user = userRepository.findByUsernameOrEmail(input, input).orElseThrow();

            // 3. Generate Token (Use the real username from the DB, not the email input)
            String token = jwtUtil.generateToken(user.getUsername());

            // Determine if profile is complete: firstName, lastName, phoneNumber, address, profileImageUrl
            boolean needsProfile = (
                    user.getFirstName() == null || user.getFirstName().isBlank()
                    || user.getLastName() == null || user.getLastName().isBlank()
                    || user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()
                    || user.getAddress() == null || user.getAddress().isBlank()
                    || user.getProfileImageUrl() == null || user.getProfileImageUrl().isBlank()
            );

            // 4. Return response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("needsProfile", needsProfile);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Credentials");
        }
    }

    // Simple logout endpoint (client should still remove their token). This is a placeholder if later you implement token revocation.
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // No server-side action performed (stateless JWT), but endpoint exists for client to call.
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}