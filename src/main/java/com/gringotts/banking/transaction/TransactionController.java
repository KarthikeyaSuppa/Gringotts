package com.gringotts.banking.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.    domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST API for Transactions.
 * Exposes endpoints to transfer money and view history.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * Transfers money between accounts.
     * Endpoint: POST /api/transactions/transfer
     * Body: { "fromAccountId": 1, "toAccountId": 2, "amount": 50.00 }
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> request) {
        try {
            // Parse Inputs
            Long fromId = Long.valueOf(request.get("fromAccountId").toString());
            Long toId = Long.valueOf(request.get("toAccountId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            transactionService.transferFunds(fromId, toId, amount);
            return ResponseEntity.ok("Transfer Successful");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Retrieves transaction history for an account.
     * Endpoint: GET /api/transactions/{accountId}?page=0&size=10
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<Page<Transaction>> getHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> history = transactionService.getTransactionHistory(accountId, pageable);
        return ResponseEntity.ok(history);
    }
}