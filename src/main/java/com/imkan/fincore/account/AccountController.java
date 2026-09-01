package com.imkan.fincore.account;

import com.imkan.fincore.account.dto.AccountResponse;
import com.imkan.fincore.account.dto.BalanceResponse;
import com.imkan.fincore.account.dto.CreateAccountRequest;
import com.imkan.fincore.exception.AccountAccessDeniedException;
import com.imkan.fincore.exception.AccountNotFoundException;
import com.imkan.fincore.exception.UserNotFoundException;
import com.imkan.fincore.user.User;
import com.imkan.fincore.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountController(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

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

}
