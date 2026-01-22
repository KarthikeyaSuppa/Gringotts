package com.gringotts.banking.transaction;

import com.gringotts.banking.account.Account; // ✅ Added
import com.gringotts.banking.account.AccountRepository; // ✅ Added
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository; // ✅ Need this to find Account ID from User ID

    // ... [transfer endpoint remains the same] ...
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> request) {
        try {
            if (!request.containsKey("fromAccountId") ||
                    !request.containsKey("toAccountNumber") ||
                    !request.containsKey("amount")) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            Long fromId = Long.valueOf(request.get("fromAccountId").toString());
            String toAccountNumber = request.get("toAccountNumber").toString();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            transactionService.transferFunds(fromId, toAccountNumber, amount);
            return ResponseEntity.ok("Transfer Successful");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ... [getHistory endpoint remains the same] ...
    @GetMapping("/history/{userId}")
    public ResponseEntity<Page<TransactionDTO>> getHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getTransactions(userId, page, size));
    }

    /**
     * ✅ UPDATED: Fixed Logic for "Sent vs Received"
     */
    @GetMapping("/search")
    public ResponseEntity<List<TransactionDTO>> searchTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String flow,
            @RequestParam(required = false) String type) {

        // 1. Get the Account ID for this User
        Account myAccount = accountRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("User account not found"));
        Long myAccountId = myAccount.getId();

        List<TransactionDTO> all = transactionService.getTransactions(userId, 0, Integer.MAX_VALUE).getContent();

        List<TransactionDTO> filtered = all.stream()
                .filter(t -> {
                    boolean match = true;

                    if (startDate != null && !startDate.isEmpty()) {
                        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
                        match = match && !t.getTimestamp().isBefore(start);
                    }
                    if (endDate != null && !endDate.isEmpty()) {
                        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
                        match = match && !t.getTimestamp().isAfter(end);
                    }

                    // ✅ FIXED FLOW FILTER
                    if ("SENT".equalsIgnoreCase(flow)) {
                        // Check if MY account ID is the sender
                        match = match && t.getAccount().getId().equals(myAccountId);
                    } else if ("RECEIVED".equalsIgnoreCase(flow)) {
                        // Check if MY account ID is the target
                        match = match && (t.getTargetAccount() != null && t.getTargetAccount().getId().equals(myAccountId));
                    }

                    if (type != null && !type.isEmpty()) {
                        match = match && t.getType().equalsIgnoreCase(type);
                    }

                    return match;
                })
                .toList();

        return ResponseEntity.ok(filtered);
    }

    /**
     * ✅ UPDATED: Fixed CSV Logic
     */
    @GetMapping("/download")
    public void downloadCsv(
            HttpServletResponse response,
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String flow,
            @RequestParam(required = false) String type) throws Exception {

        // 1. Get Account ID to determine +/- sign
        Account myAccount = accountRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("User account not found"));
        Long myAccountId = myAccount.getId();

        ResponseEntity<List<TransactionDTO>> res = searchTransactions(userId, startDate, endDate, flow, type);
        List<TransactionDTO> transactions = res.getBody();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Reference ID,Date,Type,Description,Amount,Status,Balance After");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (TransactionDTO t : transactions) {
            // ✅ FIXED LOGIC: Compare Transaction Account vs My Account ID
            boolean isSent = t.getAccount().getId().equals(myAccountId);

            // If I sent it, it's a Debit (-). If I received it, it's a Credit (+).
            String sign = isSent ? "-" : "+";
            String flowType = isSent ? "DEBIT" : "CREDIT";

            BigDecimal balance = isSent ? t.getSourceBalanceAfter() : t.getTargetBalanceAfter();
            if(balance == null) balance = BigDecimal.ZERO;

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
}