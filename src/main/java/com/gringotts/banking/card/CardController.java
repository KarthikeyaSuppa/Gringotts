package com.gringotts.banking.card;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gringotts.banking.card.CardService;

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

    // Note: CreditService was referenced but not present in the codebase.
    // We use CardService which already contains credit-related operations (payCreditBill, createCreditCard).

    // POST /api/cards/pay-bill
    // Body: { "userId": 1, "cardId": 5, "amount": 100, "sourceAccountId": 2 }
    @PostMapping("/pay-bill")
    public ResponseEntity<?> payBill(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            // The frontend should send the ACCOUNT ID associated with the credit card
            Long creditAccountId = Long.valueOf(request.get("creditAccountId").toString());

            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Long sourceAccountId = Long.valueOf(request.get("sourceAccountId").toString());

            // Route through CardService which implements payCreditBill
            cardService.payCreditBill(userId, creditAccountId, amount, sourceAccountId);
            return ResponseEntity.ok("Bill Paid Successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

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

    // ✅ NEW: Update Card Settings
    @PutMapping("/{cardId}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable Long cardId, @RequestBody Map<String, Object> request) {
        try {
            String newPin = request.containsKey("pin") ? request.get("pin").toString() : null;
            BigDecimal newLimit = request.containsKey("limit") ? new BigDecimal(request.get("limit").toString()) : null;

            cardService.updateCardSettings(cardId, newPin, newLimit);
            return ResponseEntity.ok("Card settings updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Apply for a Credit Card.
     * Endpoint: POST /api/cards/credit
     * Body: { "userId": 1 }
     */
    @PostMapping("/credit")
    public ResponseEntity<?> applyForCreditCard(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            CardResponse card = cardService.createCreditCard(userId);
            return ResponseEntity.ok(card);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{cardId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long cardId, @RequestParam String status) {
        cardService.toggleCardStatus(cardId, status);
        return ResponseEntity.ok("Card status updated");
    }
}