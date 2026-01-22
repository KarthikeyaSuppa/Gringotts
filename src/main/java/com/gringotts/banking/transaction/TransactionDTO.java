package com.gringotts.banking.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTO {

    private Long id;
    private String referenceId;
    private String type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;

    // Nested DTOs to avoid circular reference and huge payloads
    private AccountSummary account;        // Sender
    private AccountSummary targetAccount;  // Receiver

    private BigDecimal sourceBalanceAfter;
    private BigDecimal targetBalanceAfter;

    // --- Inner Class for Account Summary ---
    public static class AccountSummary {
        private Long id;
        private String accountNumber;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public AccountSummary getAccount() { return account; }
    public void setAccount(AccountSummary account) { this.account = account; }

    public AccountSummary getTargetAccount() { return targetAccount; }
    public void setTargetAccount(AccountSummary targetAccount) { this.targetAccount = targetAccount; }

    public BigDecimal getSourceBalanceAfter() { return sourceBalanceAfter; }
    public void setSourceBalanceAfter(BigDecimal sourceBalanceAfter) { this.sourceBalanceAfter = sourceBalanceAfter; }

    public BigDecimal getTargetBalanceAfter() { return targetBalanceAfter; }
    public void setTargetBalanceAfter(BigDecimal targetBalanceAfter) { this.targetBalanceAfter = targetBalanceAfter; }
}