package com.imkan.fincore.account.dto;

import com.imkan.fincore.account.Account;
import com.imkan.fincore.account.AccountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AccountResponse {
    private final Long id;
    private final String accountNumber;
    private final AccountType accountType;
    private final BigDecimal balance;
    private final LocalDateTime createdAt;

    public AccountResponse(Long id, String accountNumber, AccountType accountType, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public static AccountResponse fromEntity(Account acc) {
        return new AccountResponse(
                acc.getId(),
                acc.getAccountNumber(),
                acc.getAccountType(),
                acc.getBalance(),
                acc.getCreatedAt()
        );
    }
}
