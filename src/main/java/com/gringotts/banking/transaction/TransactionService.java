package com.gringotts.banking.transaction;

import com.gringotts.banking.account.Account;
import com.gringotts.banking.account.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Business Logic for Money Movement.
 * Handles Transfers, Withdrawals, and Audit Logging.
 */

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Executes a secure money transfer between two internal accounts.
     * Flow:
     * 1. Validate Input (Amount > 0, Sender != Receiver).
     * 2. Check Balance (Sender has enough money).
     * 3. Atomic Update: Deduct from Sender, Add to Receiver.
     * 4. Log Transaction.
     * * ACID Guarantee: If any step fails, the entire operation rolls back.
     */
    @Transactional
    public void transferFunds(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // 0. Self-Transfer Check
        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Cannot transfer funds to the same account");
        }

        // 1. Validate Amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be positive");
        }

        // 2. Fetch Accounts
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        // 3. Check Balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // 4. Perform the Transfer (In Memory)
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // 5. Save Changes to DB
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 6. Log the Transaction
        Transaction transaction = new Transaction();
        transaction.setReferenceId(UUID.randomUUID().toString());
        transaction.setAccount(fromAccount);
        transaction.setTargetAccount(toAccount);
        transaction.setAmount(amount);

        // ✅ CHANGED: Set using Enum
        transaction.setType(TransactionType.TRANSFER);

        transaction.setDescription("Transfer to " + toAccount.getAccountNumber());

        transactionRepository.save(transaction);
    }

    /**
     * Handles Withdrawals (e.g., Card Purchases).
     * Deducts money from one account without a target account.
     */
    // NEW METHOD: Handle Withdrawal / Card Purchase
    @Transactional
    public void withdraw(Long accountId, BigDecimal amount, String description, TransactionType type) {
        // 1. Validate Amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        // 2. Fetch Account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 3. Check Balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // 4. Deduct Money
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // 5. Log Transaction
        Transaction transaction = new Transaction();
        transaction.setReferenceId(UUID.randomUUID().toString());
        transaction.setAccount(account);
        transaction.setTargetAccount(null); // No target for purchases
        transaction.setAmount(amount.negate()); // Store as negative for easier math later?
        // OR store positive and rely on Type. Let's keep positive.
        transaction.setType(type);
        transaction.setDescription(description);

        transactionRepository.save(transaction);
    }
    /**
     * Retrieves transaction history for an account.
     * Supports Pagination to handle large datasets efficiently.
     */
    public Page<Transaction> getTransactionHistory(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }
}