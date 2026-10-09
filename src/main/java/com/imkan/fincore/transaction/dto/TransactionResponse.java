package com.imkan.fincore.transaction.dto;

import com.imkan.fincore.account.Account;
import com.imkan.fincore.transaction.Transaction;
import com.imkan.fincore.transaction.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TransactionResponse {
    private final String referenceId;
    private final String senderAccountNumber;
    private final String receiverAccountNumber;
    private final BigDecimal amount;
    private final TransactionStatus status;
    private final String failureReason;
    private final LocalDateTime createdAt;

    public static TransactionResponse build(Transaction t, Account s, Account r){
        return new TransactionResponse(
                t.getReferenceId(),
                s.getAccountNumber(),
                r.getAccountNumber(),
                t.getAmount(),
                t.getStatus(),
                t.getFailureReason(),
                t.getCreatedAt()
        );
    }
}
