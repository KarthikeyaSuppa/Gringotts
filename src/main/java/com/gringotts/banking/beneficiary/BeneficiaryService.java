package com.gringotts.banking.beneficiary;

import com.gringotts.banking.user.User;
import com.gringotts.banking.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class BeneficiaryService {

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Beneficiary> getBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserId(userId);
    }

    public Beneficiary addBeneficiary(Long userId, String name, String accNum, BigDecimal limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Beneficiary b = new Beneficiary();
        b.setUser(user);
        b.setName(name);
        b.setAccountNumber(accNum);
        b.setTransactionLimit(limit);

        return beneficiaryRepository.save(b);
    }

    public void deleteBeneficiary(Long id) {
        beneficiaryRepository.deleteById(id);
    }
}