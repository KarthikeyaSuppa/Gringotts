package com.gringotts.banking.card;

import com.gringotts.banking.account.Account;
import com.gringotts.banking.account.AccountRepository;
import com.gringotts.banking.account.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gringotts.banking.transaction.TransactionService;
import com.gringotts.banking.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    // Issue a new Debit Card
    public Card issueCard(Long accountId, String pin) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber(generateCardNumber());
        card.setCvv(generateCVV());
        card.setExpiryDate(LocalDate.now().plusYears(5)); // Valid for 5 years
        card.setPinHash(passwordEncoder.encode(pin)); // Hash the PIN!
        card.setCardType("DEBIT");
        card.setStatus("ACTIVE");

        return cardRepository.save(card);
    }

    // Helper: 16-digit Generator (Simple version)
    private String generateCardNumber() {
        // Prefix 4 (Visa) + 15 random digits
        long randomPart = ThreadLocalRandom.current().nextLong(100000000000000L, 999999999999999L);
        String cardNum = "4" + randomPart;

        if (cardRepository.existsByCardNumber(cardNum)) {
            return generateCardNumber();
        }
        return cardNum;
    }

    private String generateCVV() {
        int cvv = ThreadLocalRandom.current().nextInt(100, 999);
        return String.valueOf(cvv);
    }

    public Card getCardByNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Card not found"));
    }

    // NEW METHOD: Swipe the Card
    public void pay(String cardNumber, String cvv, String pin, BigDecimal amount) {
        // 1. Validate Card & PIN (Reusing logic via helper method is best)
        Card card = validateCardDetails(cardNumber, pin);

        // 2. Validate Security (CVV)
        if (!card.getCvv().equals(cvv)) {
            throw new RuntimeException("Invalid CVV");
        }

        // 3. Check Expiry Date
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Card has expired");
        }
        // 4. Delegate to Transaction Service to move money
        // We pass the Account ID linked to this card
        transactionService.withdraw(
                card.getAccount().getId(),
                amount,
                "Card Purchase: " + cardNumber.substring(12), // Masked number for privacy
                TransactionType.CARD_PURCHASE
        );
    }

    // NEW METHOD: Card DEPOSIT
    public void performDeposit(String cardNumber, String pin, BigDecimal amount) {
        // 1. Validate Card & PIN (Reusing logic via helper method is best)
        Card card = validateCardDetails(cardNumber, pin);

        // 2. Delegate to Account Service to deposit money
        // We pass the Account ID linked to this card
        accountService.deposit(card.getAccount().getId(), amount, TransactionType.CARD_DEPOSIT);
    }


    // Extract this private helper to clean up your code! (DRY Principle)
    private Card validateCardDetails(String cardNumber, String pin) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Invalid Card Number"));

        if (!"ACTIVE".equals(card.getStatus())) {
            throw new RuntimeException("Card is blocked");
        }

        // Note: In production, PIN check should be robust against timing attacks, but matches() is fine here.
        if (!passwordEncoder.matches(pin, card.getPinHash())) {
            throw new RuntimeException("Invalid PIN");
        }
        return card;
    }
}
