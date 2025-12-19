package com.gringotts.banking.card;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    // POST /api/cards/issue
    // Body: { "accountId": 1, "pin": "1234" }
    @PostMapping("/issue")
    public ResponseEntity<?> issueCard(@RequestBody Map<String, String> request) { //object trying for
        try {
            Long accountId = Long.valueOf(request.get("accountId"));
            String pin = request.get("pin");

            // Validate PIN is 4 digits
            if (pin == null || !pin.matches("\\d{4}")) {
                return ResponseEntity.badRequest().body("PIN must be 4 digits");
            }

            Card card = cardService.issueCard(accountId, pin);
            return ResponseEntity.ok(card);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/cards/pay
    // Body: { "cardNumber": "...", "cvv": "...", "pin": "...", "amount": 20.00 }
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody Map<String, Object> request) {
        try {
            String cardNumber = (String) request.get("cardNumber");
            String cvv = (String) request.get("cvv");
            String pin = (String) request.get("pin");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            cardService.pay(cardNumber, cvv, pin, amount);
            return ResponseEntity.ok("Payment Successful");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/cards/deposit
    // Body: { "cardNumber": "...", "pin": "...", "amount": 20.00 }
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        try {
            String cardNumber = (String) request.get("cardNumber");
            String cvv = (String) request.get("cvv");
            String pin = (String) request.get("pin");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            cardService.performDeposit(cardNumber, pin, amount);

            return ResponseEntity.ok("ATM Deposit Successful");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NEW: POST /api/cards -> create card and return CardResponse (with tempPin)
    @PostMapping("")
    public ResponseEntity<?> createCard(@RequestBody Map<String, Object> req) {
        try {
            Long accountId = Long.valueOf(req.get("accountId").toString());
            CardResponse resp = cardService.createCardForAccount(accountId);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NEW: PUT /api/cards/{id}/pin -> change existing card PIN
    @PutMapping("/{id}/pin")
    public ResponseEntity<?> changePin(@PathVariable("id") Long cardId, @RequestBody Map<String, String> req) {
        try {
            String oldPin = req.get("oldPin");
            String newPin = req.get("newPin");
            cardService.changePin(cardId, oldPin, newPin);
            return ResponseEntity.ok("PIN changed");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/cards/by-user/{userId}
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getCardsByUser(@PathVariable Long userId) {
        try {
            // Delegate to service to assemble cards per user's accounts
            return ResponseEntity.ok(cardService.getCardsByUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}