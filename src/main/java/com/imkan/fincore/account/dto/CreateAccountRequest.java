package com.imkan.fincore.account.dto;

import com.imkan.fincore.account.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.0", message = "deposit cannot be negative")
    private BigDecimal balance;
}
