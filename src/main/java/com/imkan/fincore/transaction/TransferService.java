package com.imkan.fincore.transaction;

import com.imkan.fincore.account.Account;
import com.imkan.fincore.account.AccountRepository;
import com.imkan.fincore.exception.AccountAccessDeniedException;
import com.imkan.fincore.exception.AccountNotFoundException;
import com.imkan.fincore.transaction.dto.TransactionResponse;
import com.imkan.fincore.transaction.dto.TransferRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public TransactionResponse transfer(Long userId, TransferRequest request){
        boolean senderExists = accountRepository.existsByAccountNumber(request.getSenderAccountNumber());
        boolean hasaccess = accountRepository.existsByAccountNumberAndUserId(request.getSenderAccountNumber(), userId);
        if (!senderExists) {
            throw new AccountNotFoundException(request.getSenderAccountNumber());
        }
        else if (!hasaccess) {
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
        return TransactionResponse.build(transaction, sender, receiver);
    }
}
