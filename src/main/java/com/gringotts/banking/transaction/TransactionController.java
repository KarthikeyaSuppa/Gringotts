package com.gringotts.banking.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.    domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletResponse;
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
     * Advanced Search & Filter Endpoint
     */
    @GetMapping("/search")
    public ResponseEntity<List<Transaction>> searchTransactions(
            @RequestParam Long accountId,
            @RequestParam(required = false) String startDate, // Format: YYYY-MM-DD
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String flow, // "SENT" or "RECEIVED"
            @RequestParam(required = false) TransactionType type) {

        // Note: For a real production app, use JPA Specifications.
        // For this project, we will fetch history and filter in Java (simplest for now)
        // or you can add a custom @Query in Repository.

        // Let's use the existing repository method and filter in stream for simplicity
        List<Transaction> all = transactionService.getTransactionHistory(accountId, Pageable.unpaged()).getContent();

        List<Transaction> filtered = all.stream()
                .filter(t -> {
                    boolean match = true;

                    // Date Filter
                    if (startDate != null && !startDate.isEmpty()) {
                        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
                        match = match && !t.getTimestamp().isBefore(start);
                    }
                    if (endDate != null && !endDate.isEmpty()) {
                        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
                        match = match && !t.getTimestamp().isAfter(end);
                    }

                    // Flow Filter (Sent vs Received)
                    if ("SENT".equalsIgnoreCase(flow)) {
                        match = match && t.getAccount().getId().equals(accountId);
                    } else if ("RECEIVED".equalsIgnoreCase(flow)) {
                        // It is received if Target is me
                        match = match && (t.getTargetAccount() != null && t.getTargetAccount().getId().equals(accountId));
                    }

                    // Type Filter
                    if (type != null) {
                        match = match && t.getType() == type;
                    }

                    return match;
                })
                .toList();

        return ResponseEntity.ok(filtered);
    }

    /**
     * CSV Download Endpoint
     */
    @GetMapping("/download")
    public void downloadCsv(
            HttpServletResponse response,
            @RequestParam Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String flow,
            @RequestParam(required = false) TransactionType type) throws Exception {

        // reuse search logic (or call the method above directly if refactored)
        ResponseEntity<List<Transaction>> res = searchTransactions(accountId, startDate, endDate, flow, type);
        List<Transaction> transactions = res.getBody();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Reference ID,Date,Type,Description,Amount,Status,Balance After");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Transaction t : transactions) {
            boolean isSent = t.getAccount().getId().equals(accountId);
            String flowType = isSent ? "DEBIT" : "CREDIT";
            String sign = isSent ? "-" : "+";

            BigDecimal balance = isSent ? t.getSourceBalanceAfter() : t.getTargetBalanceAfter();
            if(balance == null) balance = BigDecimal.ZERO; // Safety

            writer.printf("%s,%s,%s,%s,%s%s,%s,%s\n",
                    t.getReferenceId(),
                    t.getTimestamp().format(fmt),
                    t.getType(),
                    t.getDescription(),
                    sign, t.getAmount(),
                    flowType,
                    balance
            );
        }
    }

    /**
     * Transfers money between accounts.
     * Endpoint: POST /api/transactions/transfer
     * Body: { "fromAccountId": 1, "toAccountNumber": 2, "amount": 50.00 }
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> request) {
        try {
            // 1. Validate mandatory fields
            if (!request.containsKey("fromAccountId") ||
                    !request.containsKey("toAccountNumber") ||
                    !request.containsKey("amount")) {
                return ResponseEntity.badRequest().body("Missing required fields: fromAccountId, toAccountNumber, amount");
            }

            // 2. Parse Inputs
            // We use fromAccountId because the Frontend already knows the logged-in user's ID
            Long fromId = Long.valueOf(request.get("fromAccountId").toString());

            // We use toAccountNumber because that is what the user types in the UI
            String toAccountNumber = request.get("toAccountNumber").toString();

            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            // 3. Execute Transfer
            transactionService.transferFunds(fromId, toAccountNumber, amount);

            return ResponseEntity.ok("Transfer Successful");

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid number format");
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