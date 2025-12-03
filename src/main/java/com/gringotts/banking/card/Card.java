package com.gringotts.banking.card;

import com.gringotts.banking.account.Account;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // ✅ STRICT LIMIT: 16 Digits
    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber;

    // ✅ STRICT LIMIT: 3 Digits
    @Column(nullable = false, length = 3)
    private String cvv;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    // Note: No length limit here because hashes are long (60 chars)
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(name = "card_type", nullable = false)
    private String cardType = "DEBIT";

    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}