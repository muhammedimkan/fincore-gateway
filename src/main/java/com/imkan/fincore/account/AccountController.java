package com.imkan.fincore.account;

import com.imkan.fincore.account.dto.AccountResponse;
import com.imkan.fincore.account.dto.BalanceResponse;
import com.imkan.fincore.account.dto.CreateAccountRequest;
import com.imkan.fincore.account.dto.StatementEntryResponse;
import com.imkan.fincore.exception.AccountAccessDeniedException;
import com.imkan.fincore.exception.AccountNotFoundException;
import com.imkan.fincore.exception.UserNotFoundException;
import com.imkan.fincore.transaction.TransactionRepository;
import com.imkan.fincore.user.User;
import com.imkan.fincore.user.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @PostMapping("/api/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CreateAccountRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Account acc = new Account();
        acc.setUser(user);
        acc.setAccountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        acc.setAccountType(request.getAccountType());
        acc.setBalance(request.getBalance());
        Account newAcc = accountRepository.save(acc);

        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.fromEntity(newAcc));
    }

    @GetMapping("/api/accounts/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        Account acc = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        if (!acc.getUser().getId().equals(userId)) {
            throw  new AccountAccessDeniedException();
        }
        return ResponseEntity.ok(new BalanceResponse(acc.getBalance()));
    }

    @GetMapping("/api/accounts/{id}/statement")
    public Page<StatementEntryResponse> getStatement(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Account acc = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        if (!acc.getUser().getId().equals(userId)) {
            throw  new AccountAccessDeniedException();
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return transactionRepository.findStatement(id, pageable).map(t -> StatementEntryResponse.build(t, id));
    }

}
