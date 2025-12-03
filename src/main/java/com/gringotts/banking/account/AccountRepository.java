package com.gringotts.banking.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Find all accounts belonging to a specific user
    List<Account> findByUserId(Long userId);

    // Check if account number exists (to avoid duplicates during generation)
    boolean existsByAccountNumber(String accountNumber);

    // Find by Account Number (Useful for transfers later)
    Optional<Account> findByAccountNumber(String accountNumber);
}