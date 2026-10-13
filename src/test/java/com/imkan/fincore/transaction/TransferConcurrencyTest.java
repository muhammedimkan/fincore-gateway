package com.imkan.fincore.transaction;

import com.imkan.fincore.account.Account;
import com.imkan.fincore.account.AccountRepository;
import com.imkan.fincore.account.AccountType;
import com.imkan.fincore.transaction.dto.TransactionResponse;
import com.imkan.fincore.transaction.dto.TransferRequest;
import com.imkan.fincore.user.User;
import com.imkan.fincore.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferConcurrencyTest {

    @Autowired private TransferService transferService;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;

    @Test
    void fiftyThreadsTransfer500From5000() throws Exception {
        // ---------- 1. SETUP ----------
        User user = new User();
        user.setEmail("test-" + UUID.randomUUID() + "@mail.com");
        user.setFullName("Concurrency Tester");
        user = userRepository.save(user);

        Account sender = createAccount(user, new BigDecimal("5000.0000"));
        Account receiver = createAccount(user, new BigDecimal("0.0000"));

        Long ownerId = user.getId();
        String senderNo = sender.getAccountNumber();
        String receiverNo = receiver.getAccountNumber();

        // ---------- 2. ACTION: 50 threads fire at once ----------
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<TransactionResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                TransferRequest request = new TransferRequest();
                request.setSenderAccountNumber(senderNo);
                request.setReceiverAccountNumber(receiverNo);
                request.setAmount(new BigDecimal("500"));

                ready.countDown();
                go.await();
                return transferService.transfer(ownerId, request);
            }));
        }

        ready.await();
        go.countDown();

        // ---------- 3. CHECK ----------
        int completed = 0;
        int failed = 0;
        for (Future<TransactionResponse> f : futures) {
            TransactionResponse response = f.get();
            if (response.getStatus() == TransactionStatus.COMPLETED) {
                completed++;
            } else if (response.getStatus() == TransactionStatus.FAILED) {
                failed++;
            }
        }
        pool.shutdown();

        assertThat(completed).isEqualTo(10);
        assertThat(failed).isEqualTo(40);

        Account senderAfter = accountRepository.findById(sender.getId()).get();
        Account receiverAfter = accountRepository.findById(receiver.getId()).get();

        assertThat(senderAfter.getBalance()).isEqualByComparingTo("0");
        assertThat(receiverAfter.getBalance()).isEqualByComparingTo("5000");
    }

    private Account createAccount(User owner, BigDecimal balance) {
        Account a = new Account();
        a.setUser(owner);
        a.setAccountNumber("TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setAccountType(AccountType.CHECKING);
        a.setBalance(balance);
        return accountRepository.save(a);
    }
}