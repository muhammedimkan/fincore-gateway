package com.imkan.fincore.transaction;

import com.imkan.fincore.account.Account;
import com.imkan.fincore.account.AccountRepository;
import com.imkan.fincore.exception.AccountAccessDeniedException;
import com.imkan.fincore.exception.AccountNotFoundException;
import com.imkan.fincore.idempotency.IdempotencyKey;
import com.imkan.fincore.idempotency.IdempotencyKeyRepository;
import com.imkan.fincore.transaction.dto.TransactionResponse;
import com.imkan.fincore.transaction.dto.TransferRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TransferService {

    private static final String PATH = "/api/transfers";
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse transfer(Long userId, String idempotencyKey, TransferRequest request){

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKeyStringAndUserIdAndRequestPath(idempotencyKey, userId, PATH);
        if (existing.isPresent()) {
            return objectMapper.readValue(existing.get().getResponsePayload(), TransactionResponse.class);
        }

        boolean senderExists = accountRepository.existsByAccountNumber(request.getSenderAccountNumber());
        boolean hasAccess = accountRepository.existsByAccountNumberAndUserId(request.getSenderAccountNumber(), userId);
        if (!senderExists) {
            throw new AccountNotFoundException(request.getSenderAccountNumber());
        }
        else if (!hasAccess) {
            throw new AccountAccessDeniedException();
        }

        boolean receiverExists = accountRepository.existsByAccountNumber(request.getReceiverAccountNumber());
        if (!receiverExists) {
            throw new AccountNotFoundException(request.getReceiverAccountNumber());
        }

        String senderNo = request.getSenderAccountNumber();
        String receiverNo = request.getReceiverAccountNumber();

        String first = senderNo.compareTo(receiverNo) < 0 ? senderNo : receiverNo;
        String second = first.equals(senderNo) ? receiverNo : senderNo;

        Account firstAcc = accountRepository.findByAccountNumberForUpdate(first).orElseThrow(() -> new AccountNotFoundException(first));
        Account secondAcc = accountRepository.findByAccountNumberForUpdate(second).orElseThrow(() -> new AccountNotFoundException(second));

        Account sender = first.equals(senderNo)? firstAcc : secondAcc;
        Account receiver = first.equals(senderNo)? secondAcc : firstAcc;

        Transaction transaction =  new Transaction();
        transaction.setReferenceId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setSenderAccountId(sender.getId());
        transaction.setSenderAccountNumber(sender.getAccountNumber());
        transaction.setReceiverAccountId(receiver.getId());
        transaction.setReceiverAccountNumber(receiver.getAccountNumber());
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
            LedgerEntry sendEntry = new LedgerEntry();
            sendEntry.setTransactionId(transaction.getId());
            sendEntry.setAccountId(sender.getId());
            sendEntry.setAmount(request.getAmount());
            sendEntry.setEntryType(EntryType.DEBIT);
            ledgerEntryRepository.save(sendEntry);

            LedgerEntry receiveEntry = new LedgerEntry();
            receiveEntry.setTransactionId(transaction.getId());
            receiveEntry.setAccountId(receiver.getId());
            receiveEntry.setAmount(request.getAmount());
            receiveEntry.setEntryType(EntryType.CREDIT);
            ledgerEntryRepository.save(receiveEntry);
        }
        TransactionResponse response = TransactionResponse.build(transaction);

        IdempotencyKey record = new IdempotencyKey();
        record.setKeyString(idempotencyKey);
        record.setUserId(userId);
        record.setRequestPath(PATH);
        record.setResponsePayload(objectMapper.writeValueAsString(response));
        idempotencyKeyRepository.save(record);

        return response;
    }
}
