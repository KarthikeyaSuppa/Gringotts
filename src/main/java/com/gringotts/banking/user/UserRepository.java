package com.gringotts.banking.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA automatically generates the SQL for these based on method names
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ✅ NEW: Find by Username OR Email
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Check phone uniqueness
    boolean existsByPhoneNumber(String phoneNumber);

    // Find user by phone number
    Optional<User> findByPhoneNumber(String phoneNumber);

}