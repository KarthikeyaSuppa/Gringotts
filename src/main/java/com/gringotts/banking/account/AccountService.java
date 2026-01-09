package com.gringotts.banking.account;

import com.gringotts.banking.transaction.Transaction;
import com.gringotts.banking.transaction.TransactionRepository;
import com.gringotts.banking.transaction.TransactionType;
import com.gringotts.banking.user.User;
import com.gringotts.banking.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages Bank Accounts.
 * Handles creation, balance updates, and linking transactions.
 */
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Creates a new bank account for a user.
     * Flow: User Profile Setup -> Controller -> Service -> DB.
     * Generates a unique 12-digit account number.
     *
     * @param userId      The owner of the account.
     * @param accountType "SAVINGS" or "CHECKING".
     * @return The created Account entity.
     */
    public Account createAccount(Long userId, String accountType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);

        return accountRepository.save(account);
    }

    /**
     * Deposits money into an account.
     * Flow: Controller/CardService -> Service -> DB.
     * Updates Balance AND Creates a Transaction Record atomically.
     *
     * @param accountId Target account.
     * @param amount    Amount to add.
     * @param type      Source (CASH_DEPOSIT vs CARD_DEPOSIT).
     * @return The updated Account entity.
     */
    @Transactional
    public Account deposit(Long accountId, BigDecimal amount, TransactionType type) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 1. Update Balance
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.save(account);

        // 2. Log Transaction
        Transaction transaction = new Transaction();
        transaction.setReferenceId(UUID.randomUUID().toString());
        transaction.setAccount(account);
        transaction.setTargetAccount(null);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription("Deposit via " + (type == TransactionType.CARD_DEPOSIT ? "ATM" : "Branch"));

        transactionRepository.save(transaction);

        return savedAccount;
    }

    /**
     * Overloaded method for default Cash Deposits.
     */
    public Account deposit(Long accountId, BigDecimal amount) {
        return deposit(accountId, amount, TransactionType.CASH_DEPOSIT);
    }

    /**
     * Fetches all accounts belonging to a specific user if only ACTIVE.
     */
    public List<Account> getAccountsByUser(Long userId) {
        // Return only ACTIVE accounts
        return accountRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    /**
     * Deletes an account by making the status as closed.
     */
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if already closed
        if ("CLOSED".equals(account.getStatus())) {
            throw new RuntimeException("Account is already closed");
        }

        // Soft Delete Logic
        account.setStatus("CLOSED");
        accountRepository.save(account);
    }

    /**
     * Helper: Generates a unique 12-digit number.
     * Uses recursion to ensure uniqueness.
     */
    private String generateAccountNumber() {
        long number = ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L);
        String accStr = String.valueOf(number);
        if (accountRepository.existsByAccountNumber(accStr)) {
            return generateAccountNumber();
        }
        return accStr;
    }
}