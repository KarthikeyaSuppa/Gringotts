package com.gringotts.banking.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Data Access Layer for Transactions.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds all transactions involving a specific account.
     * Logic: Returns rows where the account was the SENDER (account.id)
     * OR the RECEIVER (targetAccount.id).
     *
     * @param accountId The ID of the account to query history for.
     * @param pageable  Pagination information (page number, size, sort).
     * @return A Page of Transaction entities.
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId OR t.targetAccount.id = :accountId ORDER BY t.timestamp DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}