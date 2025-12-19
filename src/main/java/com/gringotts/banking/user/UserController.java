package com.gringotts.banking.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.Optional;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;


@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173") // Add this line!
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // Make upload dir configurable; default to 'uploads'
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.ok(registeredUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file, Principal principal) {
        logger.debug("uploadProfileImage called for id={} by principal={} | SecurityContext principal={} ", id, principal != null ? principal.getName() : null, SecurityContextHolder.getContext().getAuthentication()!=null?SecurityContextHolder.getContext().getAuthentication().getName():null);
        try {
            Optional<User> optionalUser = userService.getUserById(id);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = optionalUser.get();

            // 1. Create unique filename
            String filename = "user_" + id + "_" + System.currentTimeMillis() + ".jpg";

            // 2. Save File to Disk (configurable folder)
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            Path path = dirPath.resolve(filename);
            Files.write(path, file.getBytes());

            // 3. Update Database with the Link (store filename only)
            user.setProfileImageUrl(filename);
            userService.saveUser(user); // Helper method to save user

            // Return the filename so frontend can build the URL
            return ResponseEntity.ok(Map.of("profileImageUrl", filename));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    // Add this to UserController.java to handle the text updates
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserDetails(@PathVariable Long id, @RequestBody User userDetails, Principal principal) {
        logger.debug("updateUserDetails called for id={} by principal={} | SecurityContext principal={} ", id, principal != null ? principal.getName() : null, SecurityContextHolder.getContext().getAuthentication()!=null?SecurityContextHolder.getContext().getAuthentication().getName():null);
        Optional<User> optionalUser = userService.getUserById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = optionalUser.get();
        // Validate phone uniqueness if changed
        String newPhone = userDetails.getPhoneNumber();
        if (newPhone != null && !newPhone.isBlank() && !newPhone.equals(user.getPhoneNumber())) {
            if (userService.existsByPhoneNumber(newPhone)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number already in use"));
            }
        }

        // Use Lombok-generated getters/setters (camel case)
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setAddress(userDetails.getAddress());
        try {
            User updated = userService.saveUser(user);
            return ResponseEntity.ok(updated);
        } catch (DataIntegrityViolationException dive) {
            // Likely unique constraint violation
            logger.warn("DataIntegrityViolation on updateUserDetails: {}", dive.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Duplicate value or invalid data"));
        }
    }

    // NEW: Get basic user info (used by frontend to display profile image)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<User> optionalUser = userService.getUserById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User u = optionalUser.get();
        // Return a safe map (do not return password)
        Map<String, Object> resp = Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "firstName", u.getFirstName(),
                "lastName", u.getLastName(),
                "email", u.getEmail(),
                "phoneNumber", u.getPhoneNumber(),
                "address", u.getAddress(),
                "profileImageUrl", u.getProfileImageUrl()
        );
        return ResponseEntity.ok(resp);
    }

    //
    @GetMapping("/profile") // Ends up as: GET /api/users/profile
    public ResponseEntity<UserDTO> getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username); // Ensure this method exists in Service

        UserDTO userDTO = new UserDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(), // Check if Entity uses getPhone() or getPhoneNumber()
                user.getEmail(),
                user.getAddress(),
                user.getDateOfBirth(), // Check Entity date getter
                user.getProfileImageUrl()
        );
        return ResponseEntity.ok(userDTO);
    }

}
