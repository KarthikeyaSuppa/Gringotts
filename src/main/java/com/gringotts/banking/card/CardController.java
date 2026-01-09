package com.gringotts.banking.card;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;


/**
 * REST API for Card Operations.
 * Exposes endpoints for Issuing Cards, Payments, and ATM Deposits.
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    /**
     * Issues a new card for an account.
     * Endpoint: POST /api/cards
     * Body: { "accountId": 1 }
     */
    @PostMapping
    public ResponseEntity<?> createCard(@RequestBody Map<String, Object> request) {
        try {
            Long accountId = Long.valueOf(request.get("accountId").toString());
            CardResponse card = cardService.createCardForAccount(accountId);
            return ResponseEntity.ok(card);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Legacy Endpoint: Issues card with manual PIN (Testing only).
     * Endpoint: POST /api/cards/issue
     */
    @PostMapping("/issue")
    public ResponseEntity<?> issueCard(@RequestBody Map<String, Object> request) {
        try {
            Long accountId = Long.valueOf(request.get("accountId").toString());
            String pin = request.get("pin").toString();

            if (!pin.matches("\\d{4}")) {
                return ResponseEntity.badRequest().body("PIN must be 4 digits");
            }
            // For now, this just calls the standard create logic which generates a random PIN
            // To support manual PIN setting, CardService needs updating.
            // Returning error to encourage use of standard flow.
            return ResponseEntity.badRequest().body("Please use the standard /api/cards endpoint which auto-generates a secure PIN.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Simulation of an ATM Deposit.
     * Endpoint: POST /api/cards/deposit
     * Body: { "cardNumber": "...", "pin": "...", "amount": 100 }
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        try {
            String cardNumber = request.get("cardNumber").toString();
            String pin = request.get("pin").toString();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            cardService.performDeposit(cardNumber, pin, amount);
            return ResponseEntity.ok("ATM Deposit Successful");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Simulation of a Card Purchase (Swipe).
     * Endpoint: POST /api/cards/pay
     * Body: { "cardNumber": "...", "cvv": "...", "pin": "...", "amount": 50 }
     */
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody Map<String, Object> request) {
        try {
            String cardNumber = request.get("cardNumber").toString();
            String cvv = request.get("cvv").toString();
            String pin = request.get("pin").toString();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            cardService.pay(cardNumber, cvv, pin, amount);
            return ResponseEntity.ok("Payment Successful");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ ADD THIS NEW ENDPOINT
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<CardResponse>> getCardsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getCardsByUser(userId));
    }
}