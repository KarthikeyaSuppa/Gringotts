package com.gringotts.banking.config;

import io.jsonwebtoken.Claims; // Represents the "Payload" (data inside the token)
import io.jsonwebtoken.Jwts;   // The main factory class for creating/parsing JWTs
import io.jsonwebtoken.SignatureAlgorithm; // Enum for algorithms (e.g., HS256)
import io.jsonwebtoken.security.Keys;      // Helper to generate secure keys
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component; // Tells Spring: "Manage this class"

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.Base64;

@Component
public class JwtUtil {
    // Read the secret from configuration
    @Value("${jwt.secret:change_this_to_a_very_long_secret_key_of_at_least_32_chars!}")
    private String jwtSecret;

    private Key SECRET_KEY;

    @PostConstruct
    public void init() {
        // Ensure the key is bytes of sufficient length; allow plain text or base64
        byte[] keyBytes = jwtSecret.getBytes();
        // If the provided secret looks like base64 (contains =), try decode
        try {
            if (jwtSecret.contains("=") || jwtSecret.contains("/")) {
                keyBytes = Base64.getDecoder().decode(jwtSecret);
            }
        } catch (Exception e) {
            // fallback to raw bytes
            keyBytes = jwtSecret.getBytes();
        }
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    // 2. EXTRACT USERNAME
    // Takes a token, decodes it, and pulls out the "Subject" (Username).
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. EXTRACT EXPIRATION
    // Checks when this token dies.
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 4. GENERIC CLAIM EXTRACTOR
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 250 * 60 * 60))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}