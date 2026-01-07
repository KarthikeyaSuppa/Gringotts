package com.gringotts.banking.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * REST API for User Operations.
 * Handles Registration, Profile Management, and Image Uploads.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Configuration: Where to save images locally
    private static final String UPLOAD_DIR = "uploads/";

    /**
     * Registers a new user.
     * Endpoint: POST /api/users/register
     * Flow: Frontend -> Controller -> Service -> DB.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.ok(registeredUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Updates an existing user's profile.
     * Endpoint: PUT /api/users/{id}
     * Used during the Onboarding flow (adding Name, Address, Phone).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update fields if provided
            if (userDetails.getFirstName() != null) user.setFirstName(userDetails.getFirstName());
            if (userDetails.getLastName() != null) user.setLastName(userDetails.getLastName());
            if (userDetails.getPhoneNumber() != null) user.setPhoneNumber(userDetails.getPhoneNumber());
            if (userDetails.getAddress() != null) user.setAddress(userDetails.getAddress());

            userService.saveUser(user);

            // Return the updated user so frontend can update state
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Update failed: " + e.getMessage());
        }
    }

    /**
     * Fetches the logged-in user's profile.
     * Endpoint: GET /api/users/profile
     * Security: Uses the JWT Token (Authentication object) to identify the user.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        UserDTO userDTO = new UserDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAddress(),
                user.getDateOfBirth(),
                user.getProfileImageUrl()
        );

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Fetch user by ID (Internal/Admin use or specific frontend checks).
     * Endpoint: GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Uploads a profile picture.
     * Endpoint: POST /api/users/{id}/image
     * Logic: Saves file to 'uploads/' folder and updates DB with filename.
     */
    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 1. Create unique filename
            String filename = "user_" + id + "_" + System.currentTimeMillis() + ".jpg";

            // 2. Save File to Disk (Local Folder)
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.createDirectories(path.getParent()); // Create "uploads" folder if missing
            Files.write(path, file.getBytes());

            // 3. Update Database with the Link
            user.setProfileImageUrl(filename);
            userService.saveUser(user);

            // Return the filename so frontend can display it immediately
            return ResponseEntity.ok(Map.of("message", "Image Uploaded Successfully", "profileImageUrl", filename));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }
}