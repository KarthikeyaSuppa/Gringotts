package com.gringotts.banking.user;

import com.gringotts.banking.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");


            // 1. THE AUTHENTICATION ATTEMPT
            // This one line does magic.
            // It calls AuthenticationManager -> DaoAuthenticationProvider -> CustomUserDetailsService -> loadUserByUsername
            // Then checks the Password Hash.
            // If password wrong -> Throws Exception.
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            // If successful, generate Token
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Username or Password");
        }
    }
}