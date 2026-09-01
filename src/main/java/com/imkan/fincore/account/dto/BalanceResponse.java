package com.imkan.fincore.account.dto;

import java.math.BigDecimal;

public class BalanceResponse {

    private final BigDecimal balance;

    public BalanceResponse(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
