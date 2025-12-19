package com.gringotts.banking.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // NEW ENDPOINT: Create Account
    // POST /api/accounts/{id}
    @PostMapping("/{userId}")
    public ResponseEntity<?> createAccount(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String type = request.getOrDefault("accountType", "SAVINGS");
            Account account = accountService.createAccount(userId, type);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NEW ENDPOINT: Deposit Money
    // POST /api/accounts/{id}/deposit
    // Body: { "amount": 500.00 }
    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        try {
            BigDecimal amount = request.get("amount");
            Account account = accountService.deposit(id, amount);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/accounts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok("Account deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/accounts/by-user/{userId}
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getAccountsByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(accountService.getAccountsByUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}