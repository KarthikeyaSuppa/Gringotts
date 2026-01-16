package com.gringotts.banking.beneficiary;

import com.gringotts.banking.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "transaction_limit")
    private BigDecimal transactionLimit;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getTransactionLimit() { return transactionLimit; }
    public void setTransactionLimit(BigDecimal transactionLimit) { this.transactionLimit = transactionLimit; }
}