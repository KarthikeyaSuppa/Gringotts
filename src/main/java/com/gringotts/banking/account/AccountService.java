package com.gringotts.banking.account;

import com.gringotts.banking.transaction.Transaction;
import com.gringotts.banking.transaction.TransactionRepository;
import com.gringotts.banking.transaction.TransactionType;
import com.gringotts.banking.user.User;
import com.gringotts.banking.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Account deposit(Long accountId, BigDecimal amount) {
        return deposit(accountId, amount, TransactionType.CASH_DEPOSIT);
    }

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

    // Updated Method: Handles Deposit AND Tracking
    @Transactional // Ensures both save() calls happen or neither does
    public Account deposit(Long accountId, BigDecimal amount, TransactionType type) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        //1. Update Balance
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        Account savedAccount= accountRepository.save(account);

        // 2. Create Transaction Record
        Transaction transaction = new Transaction();
        transaction.setReferenceId(UUID.randomUUID().toString());
        transaction.setAccount(account);
        transaction.setTargetAccount(null); // No target for cash deposits
        transaction.setAmount(amount);
        transaction.setType(type); // Uses the passed type (CARD_DEPOSIT or CASH_DEPOSIT) Enum type
        transaction.setDescription("Deposit via " + (type == TransactionType.CARD_DEPOSIT ? "ATM" : "Branch"));

        transactionRepository.save(transaction);

        return savedAccount;

    }

    private String generateAccountNumber() {
        //Why not `new Random()`?** In a web server (like Tomcat), hundreds of users might try to open accounts at the same time (multi-threading).
        // The standard `Random` class has "contention" (threads fight over it), making it slow. `ThreadLocalRandom` gives each thread its own private generator.
        // It is much faster for high-concurrency apps like banking.
        long number = ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L);
        //Why? We store account numbers as Strings in the database (`VARCHAR`).
        // This preserves leading zeros (if we had them) and prevents the database from trying to do math on account numbers
        String accStr = String.valueOf(number);
        if (accountRepository.existsByAccountNumber(accStr)) {
            return generateAccountNumber();
        }
        return accStr;
    }
}