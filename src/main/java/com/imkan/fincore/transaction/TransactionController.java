package com.imkan.fincore.transaction;

import com.imkan.fincore.account.Account;
import com.imkan.fincore.account.AccountRepository;
import com.imkan.fincore.exception.AccountAccessDeniedException;
import com.imkan.fincore.exception.AccountNotFoundException;
import com.imkan.fincore.transaction.dto.TransactionResponse;
import com.imkan.fincore.transaction.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
public class TransactionController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @PostMapping("/api/transfers")
    public ResponseEntity<TransactionResponse> transfer(@RequestHeader("X-User-Id") Long userid, @Valid @RequestBody TransferRequest request) {
        Account sender = accountRepository.findByAccountNumber(request.getSenderAccountNumber()).orElseThrow(() -> new AccountNotFoundException(request.getSenderAccountNumber()));
        if (!sender.getUser().getId().equals(userid)) {
            throw new AccountAccessDeniedException();
        }
        Account receiver = accountRepository.findByAccountNumber(request.getReceiverAccountNumber()).orElseThrow(() -> new AccountNotFoundException(request.getReceiverAccountNumber()));

        Transaction transaction =  new Transaction();
        transaction.setReferenceId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setSenderAccountId(sender.getId());
        transaction.setReceiverAccountId(receiver.getId());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Insufficient funds");
        }
        else {
            sender.setBalance(sender.getBalance().subtract(request.getAmount()));
            receiver.setBalance(receiver.getBalance().add(request.getAmount()));
            transaction.setStatus(TransactionStatus.COMPLETED);
            accountRepository.save(sender);
            accountRepository.save(receiver);
        }
        transactionRepository.save(transaction);

        if (transaction.getStatus().equals(TransactionStatus.COMPLETED)) {
            LedgerEntry sendentry = new LedgerEntry();
            sendentry.setTransactionId(transaction.getId());
            sendentry.setAccountId(sender.getId());
            sendentry.setAmount(request.getAmount());
            sendentry.setEntryType(EntryType.DEBIT);
            ledgerEntryRepository.save(sendentry);

            LedgerEntry receiveentry = new LedgerEntry();
            receiveentry.setTransactionId(transaction.getId());
            receiveentry.setAccountId(receiver.getId());
            receiveentry.setAmount(request.getAmount());
            receiveentry.setEntryType(EntryType.CREDIT);
            ledgerEntryRepository.save(receiveentry);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.build(transaction, sender, receiver));
    }
}
