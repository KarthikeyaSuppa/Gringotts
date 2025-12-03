package com.gringotts.banking.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find all transactions involving this account (Sent OR Received)
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId OR t.targetAccount.id = :accountId ORDER BY t.timestamp DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}