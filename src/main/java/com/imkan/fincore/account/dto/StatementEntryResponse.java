package com.imkan.fincore.account.dto;

import com.imkan.fincore.transaction.EntryType;
import com.imkan.fincore.transaction.Transaction;
import com.imkan.fincore.transaction.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StatementEntryResponse {
    private final String referenceId;
    private final EntryType type;
    private final String counterpartyAccountNumber;
    private final BigDecimal amount;
    private final TransactionStatus status;
    private final String failureReason;
    private final LocalDateTime createdAt;

    public static StatementEntryResponse build(Transaction t, Long accountId) {
        boolean sent = t.getSenderAccountId().equals(accountId);

        return new StatementEntryResponse(
                t.getReferenceId(),
                sent ? EntryType.DEBIT : EntryType.CREDIT,
                sent ? t.getReceiverAccountNumber() : t.getSenderAccountNumber(),
                t.getAmount(),
                t.getStatus(),
                t.getFailureReason(),
                t.getCreatedAt()
        );
    }
}
