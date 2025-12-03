package com.gringotts.banking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            username = jwtUtil.extractUsername(jwt);// Decode it
        }

        // 2. Validate Token and set Security Context
        //3. If we found a username AND they aren't logged in yet...
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load the user from the Database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                // 5. Create the "Ticket" (Authentication Object)
                // This object tells Spring Security: "This guy is legit. Here are his roles."
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // This is where the user is officially "Logged In" for this request
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // 7. Continue the chain (Go to the next filter or the Controller)
        chain.doFilter(request, response);
    }
}