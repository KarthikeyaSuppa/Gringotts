package com.gringotts.banking.transaction;

import com.gringotts.banking.account.Account;
import com.gringotts.banking.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Tells JUnit to use Mockito
class TransactionServiceTest {

    @Mock // Create a fake repository
    private AccountRepository accountRepository;

    @Mock // Create a fake transaction repository
    private TransactionRepository transactionRepository;

    @InjectMocks // Inject the fake repos into the real service
    private TransactionService transactionService;

    @Test
    void transferFunds_Success() {
        // 1. SETUP (The "Given")
        Account sender = new Account();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("100.00"));

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("50.00"));

        // Teach the Mock Repository what to do
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        // 2. EXECUTE (The "When")
        transactionService.transferFunds(1L, 2L, new BigDecimal("50.00"));

        // 3. ASSERT (The "Then")
        // Did the balances change correctly?
        assertEquals(new BigDecimal("50.00"), sender.getBalance()); // 100 - 50 = 50
        assertEquals(new BigDecimal("100.00"), receiver.getBalance()); // 50 + 50 = 100

        // Did we save the changes?
        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(receiver);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void transferFunds_InsufficientFunds_ShouldThrowException() {
        // 1. SETUP
        Account sender = new Account();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("10.00")); // Only has $10

        Account receiver = new Account();
        receiver.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        // 2. EXECUTE & ASSERT
        // Expect a RuntimeException when trying to send $50
        Exception exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transferFunds(1L, 2L, new BigDecimal("50.00"));
        });

        assertEquals("Insufficient funds", exception.getMessage());

        // Verify we NEVER saved any changes
        verify(accountRepository, never()).save(any());
    }
}