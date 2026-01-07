package com.gringotts.banking.transaction;

import com.gringotts.banking.account.Account;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single financial event (Audit Log).
 * Maps to table: 'transactions'
 * Immutable: Transactions should never be updated, only created.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", unique = true, nullable = false)
    private String referenceId;

    // The account performing the action (Source of funds)
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // The target account (Receiver) - Nullable for Deposits/Withdrawals
    @ManyToOne
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    // Stores the text value (e.g., "TRANSFER") instead of a number
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    // --- CONSTRUCTORS ---

    public Transaction() {
    }

    // --- GETTERS AND SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Account getTargetAccount() { return targetAccount; }
    public void setTargetAccount(Account targetAccount) { this.targetAccount = targetAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}