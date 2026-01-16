package com.gringotts.banking.beneficiary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    @Autowired
    private BeneficiaryService beneficiaryService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Beneficiary>> getList(@PathVariable Long userId) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String name = (String) request.get("name");
            String accNum = (String) request.get("accountNumber");
            BigDecimal limit = new BigDecimal(request.get("transactionLimit").toString());

            return ResponseEntity.ok(beneficiaryService.addBeneficiary(userId, name, accNum, limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id);
        return ResponseEntity.ok("Deleted");
    }
}