package com.gringotts.banking.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Layer for Cards.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Finds a card by its unique 16-digit PAN.
     */
    Optional<Card> findByCardNumber(String cardNumber);

    /**
     * Checks if a card number already exists (used during generation).
     */
    boolean existsByCardNumber(String cardNumber);

    /**
     * Finds all cards linked to a specific account.
     */
    List<Card> findByAccountId(Long accountId);
}