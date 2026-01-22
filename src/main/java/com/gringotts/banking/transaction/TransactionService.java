package com.gringotts.banking.transaction;

import com.gringotts.banking.account.Account;
import com.gringotts.banking.account.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest; // ✅ Added for Pagination
import org.springframework.stereotype.Service;
import com.gringotts.banking.card.CardRepository;

import java.util.Optional;

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

    @Autowired
    private CardRepository cardRepository;

    /**
     * NEW: Transfer using Account Number for the destination.
     * This looks up the account ID from the number, then calls the main logic.
     */
    @Transactional
    public void transferFunds(Long fromAccountId, String toAccountNumber, BigDecimal amount) {
        // 1. Find the Target Account ID by Number
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("Target Account Number not found"));

        // 2. Delegate to the main transfer logic
        transferFunds(fromAccountId, toAccount.getId(), amount);
    }

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

        // ✅ NEW: Status Checks
        if (!"ACTIVE".equals(fromAccount.getStatus())) {
            throw new RuntimeException("Sender account is CLOSED. Transaction denied.");
        }
        if (!"ACTIVE".equals(toAccount.getStatus())) {
            throw new RuntimeException("Target account is CLOSED. Transaction denied.");
        }

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
        // ✅ NEW: Save the running balances
        transaction.setSourceBalanceAfter(fromAccount.getBalance());
        transaction.setTargetBalanceAfter(toAccount.getBalance());
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

        // ✅ NEW: Status Check
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is CLOSED. Withdrawal denied.");
        }


        // ✅ LOGIC CHANGE: Handle Credit vs Debit
        if ("CREDIT".equals(account.getAccountType())) {
            // Find the card linked to this account to get the Limit
            // (Assuming 1 credit card per credit account for simplicity)
            var cards = cardRepository.findByAccountId(accountId);
            if(cards.isEmpty()) throw new RuntimeException("No card linked to this credit account");

            BigDecimal limit = cards.get(0).getCreditLimit();
            BigDecimal currentDebt = account.getBalance().abs(); // -100 balance = 100 debt

            // Check: (Current Debt + New Spend) > Limit?
            // Note: Since balance is negative, we add usage.
            // Better math: NewBalance would be (Current - Amount). If NewBalance < -Limit, fail.
            BigDecimal newBalance = account.getBalance().subtract(amount);

            if (newBalance.negate().compareTo(limit) > 0) {
                throw new RuntimeException("Transaction declined: Over Credit Limit");
            }
        } else {
            // Standard Savings/Checking Logic
            // 3. Check Balance
            if (account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
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
        // ✅ NEW: Save the running balance
        transaction.setSourceBalanceAfter(account.getBalance());
        transactionRepository.save(transaction);
    }
    /**
     * Retrieves transaction history for an account.
     * Supports Pagination to handle large datasets efficiently.
     */
    /**
     * ✅ NEW: Retrieves paginated transaction history for a User.
     * Uses the correct Repository method signature.
     */
    public Page<TransactionDTO> getTransactions(Long userId, int page, int size) {
        // 1. Get User's Account (Assuming 1 user = 1 primary account for this view)
        Account account = accountRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No account found for user"));

        // 2. Create Pageable
        Pageable pageable = PageRequest.of(page, size);

        // 3. Fetch Data using the updated Repository Method
        // We pass the ID twice because we want transactions where I am Sender OR Receiver
        Page<Transaction> txPage = transactionRepository.findByAccountIdOrTargetAccountIdOrderByTimestampDesc(
                account.getId(),
                account.getId(),
                pageable
        );

        // 4. Convert to DTO
        return txPage.map(this::convertToDTO);
    }

    // Helper to convert Entity -> DTO
    private TransactionDTO convertToDTO(Transaction tx) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(tx.getId());
        dto.setReferenceId(tx.getReferenceId());
        dto.setType(tx.getType().toString());
        dto.setAmount(tx.getAmount());
        dto.setDescription(tx.getDescription());
        dto.setTimestamp(tx.getTimestamp());

        // Pass account details for UI logic
        if (tx.getAccount() != null) {
            TransactionDTO.AccountSummary acc = new TransactionDTO.AccountSummary();
            acc.setId(tx.getAccount().getId());
            acc.setAccountNumber(tx.getAccount().getAccountNumber());
            dto.setAccount(acc);
        }
        if (tx.getTargetAccount() != null) {
            TransactionDTO.AccountSummary target = new TransactionDTO.AccountSummary();
            target.setId(tx.getTargetAccount().getId());
            target.setAccountNumber(tx.getTargetAccount().getAccountNumber());
            dto.setTargetAccount(target);
        }

        dto.setSourceBalanceAfter(tx.getSourceBalanceAfter());
        dto.setTargetBalanceAfter(tx.getTargetBalanceAfter());

        return dto;
    }

}