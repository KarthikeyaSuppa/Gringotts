package com.gringotts.banking.config;

import io.jsonwebtoken.Claims; // Represents the "Payload" (data inside the token)
import io.jsonwebtoken.Jwts;   // The main factory class for creating/parsing JWTs
import io.jsonwebtoken.SignatureAlgorithm; // Enum for algorithms (e.g., HS256)
import io.jsonwebtoken.security.Keys;      // Helper to generate secure keys
import org.springframework.stereotype.Component; // Tells Spring: "Manage this class"

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    // 1. THE SECRET KEY
    // This is the most important line. We generate a secure 256-bit key.
    // If anyone steals this key, they can forge tokens and hack everyone.
    // Use a strong secret key (In production, store this in env variables!)
    private final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

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
    // A fancy helper method. It takes a token and a "Resolver Function".
    // It extracts all data (Claims) and then applies the function to get just one piece.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 5. THE PARSER (The Decoder)
    // This is where the validation happens.
    // .setSigningKey(SECRET_KEY): "Use my secret key to check the signature."
    // .parseClaimsJws(token): "Read the token." If the signature is wrong, this crashes.
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 6. CHECK EXPIRY
    // Returns TRUE if the token's date is in the past.
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 7. GENERATE TOKEN (Public)
    // Called by AuthController. Take a username and create a new token.
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>(); // Empty map for extra data (like roles)
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims) // Add extra data
                .setSubject(subject) // Add Username
                .setIssuedAt(new Date(System.currentTimeMillis())) // Valid From: NOW
                .setExpiration(new Date(System.currentTimeMillis() + 250 * 60 * 60)) //(250 * 60 * 60) 15 mins    - (1000 * 60 * 60 * 10) 10 Hours validity
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) // Sign it!
                .compact(); // Convert to String
    }

    // 9. VALIDATE TOKEN
    // Called by JwtFilter.
    // It checks two things:
    // A) Does the username in the token match the user in the DB?
    // B) Is the token expired?
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}