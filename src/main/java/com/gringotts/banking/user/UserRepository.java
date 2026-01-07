package com.gringotts.banking.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Layer for Users.
 * Handles database operations for the 'users' table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     * Flow: Service -> DB (Select * from users where username=?)
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a username already exists.
     * Used during registration to prevent duplicates.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email already exists.
     * Used during registration to prevent duplicates.
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by either Username OR Email.
     * Critical for the Login feature allowing users to type either identifier.
     *
     * @param username The input string treated as a username.
     * @param email    The same input string treated as an email.
     * @return User if found in either column.
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Checks if a phone number is already registered.
     * Used during KYC / Profile update.
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Finds a user by phone number.
     * Useful for password recovery or lookup features.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
}