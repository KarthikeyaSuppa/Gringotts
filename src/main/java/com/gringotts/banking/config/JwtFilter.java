package com.gringotts.banking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder; // The "VIP Lounge" storage
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // Ensures filter runs once per API call

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil; // To decode tokens

    @Autowired
    private CustomUserDetailsService userDetailsService; // To verify user exists in DB

    // This method runs for EVERY request (GET, POST, etc.)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 0. Get the Header
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 1. Check if Header exists and starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Remove "Bearer " prefix
            try {
                username = jwtUtil.extractUsername(jwt);// Decode it
            } catch (Exception e) {
                logger.warn("Failed to extract username from JWT: {} (request: {} {})", e.getMessage(), request.getMethod(), request.getRequestURI());
            }
        } else {
            logger.debug("No Authorization header or does not start with Bearer for request {} {}", request.getMethod(), request.getRequestURI());
        }

        // 2. Validate Token and set Security Context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Load the user from the Database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // This is where the user is officially "Logged In" for this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("JWT validated for user '{}' on {} {}", username, request.getMethod(), request.getRequestURI());
                } else {
                    logger.warn("JWT validation failed for user '{}' on {} {}", username, request.getMethod(), request.getRequestURI());
                }
            } catch (Exception ex) {
                logger.warn("Error validating JWT or loading user: {} (request: {} {})", ex.getMessage(), request.getMethod(), request.getRequestURI());
            }
        }
        // 7. Continue the chain (Go to the next filter or the Controller)
        chain.doFilter(request, response);
    }
}