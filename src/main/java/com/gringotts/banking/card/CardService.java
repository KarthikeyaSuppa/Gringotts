package com.gringotts.banking.card;

import com.gringotts.banking.account.Account;
import com.gringotts.banking.account.AccountRepository;
import com.gringotts.banking.account.AccountService;
import com.gringotts.banking.transaction.TransactionService;
import com.gringotts.banking.transaction.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Business Logic for Card Operations.
 * Handles Issuance, PIN Validation, and Payments.
 */
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

    /**
     * Issues a new Debit Card linked to an Account.
     * Flow: Frontend -> CardController -> CardService -> DB.
     * Generates a random PAN, CVV, and Temporary PIN (hashed).
     *
     * @param accountId The ID of the account to link.
     * @return CardResponse containing the sensitive tempPin.
     */
    public CardResponse createCardForAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        String tempPin = generateTempPin(); // 4-digit random PIN

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber(generateCardNumber());
        card.setCvv(generateCVV());
        card.setExpiryDate(LocalDate.now().plusYears(5));
        card.setPinHash(passwordEncoder.encode(tempPin)); // Store Hash
        card.setCardType("DEBIT");
        card.setStatus("ACTIVE");

        cardRepository.save(card);

        // Convert to DTO
        CardResponse response = new CardResponse();
        response.setId(card.getId());
        response.setAccountId(account.getId());
        response.setCardNumber(card.getCardNumber());
        response.setCvv(card.getCvv());
        response.setExpiry(card.getExpiryDate().toString());
        response.setTempPin(tempPin); // CRITICAL: This is the only time user sees the PIN
        response.setCardType(card.getCardType());

        return response;
    }

    /**
     * Performs an ATM Deposit using Card credentials.
     * Flow: ATM -> CardService (Validate PIN) -> AccountService (Add Money).
     * Note: ATM deposits do not require CVV checks.
     */
    public void performDeposit(String cardNumber, String pin, BigDecimal amount) {
        Card card = validateCardDetails(cardNumber, pin);
        accountService.deposit(card.getAccount().getId(), amount, TransactionType.CARD_DEPOSIT);
    }

    /**
     * Performs a Point-of-Sale or Online Payment.
     * Flow: Merchant -> CardService (Validate PIN + CVV + Expiry) -> TransactionService (Deduct Money).
     */
    public void pay(String cardNumber, String cvv, String pin, BigDecimal amount) {
        Card card = validateCardDetails(cardNumber, pin);

        // Additional checks for Payments
        if (!card.getCvv().equals(cvv)) {
            throw new RuntimeException("Invalid CVV");
        }
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Card has expired");
        }

        transactionService.withdraw(
                card.getAccount().getId(),
                amount,
                "Card Purchase: " + cardNumber.substring(12), // Masked for privacy
                TransactionType.CARD_PURCHASE
        );
    }

    public List<CardResponse> getCardsByUser(Long userId) {
        // Placeholder for future implementation: Fetch all cards for a user
        return List.of();
    }

    // --- PRIVATE HELPERS ---

    private Card validateCardDetails(String cardNumber, String pin) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Invalid Card Number"));

        if (!"ACTIVE".equals(card.getStatus())) {
            throw new RuntimeException("Card is blocked");
        }
        if (!passwordEncoder.matches(pin, card.getPinHash())) {
            throw new RuntimeException("Invalid PIN");
        }
        return card;
    }

    private String generateTempPin() {
        int pin = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.valueOf(pin);
    }

    private String generateCardNumber() {
        long randomPart = ThreadLocalRandom.current().nextLong(100000000000000L, 999999999999999L);
        String cardNum = "4" + randomPart;
        return cardRepository.existsByCardNumber(cardNum) ? generateCardNumber() : cardNum;
    }

    private String generateCVV() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100, 999));
    }

    // Legacy support (if needed by older tests)
    public Card issueCard(Long accountId, String pin) { return null; }
}