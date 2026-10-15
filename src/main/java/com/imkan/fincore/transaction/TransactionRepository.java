package com.imkan.fincore.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
           SELECT t from Transaction t
           WHERE t.senderAccountId = :accountId
            OR (t.receiverAccountId = :accountId
                AND t.status = com.imkan.fincore.transaction.TransactionStatus.COMPLETED)
           """)
    Page<Transaction> findStatement(@Param("accountId") Long accountId, Pageable pageable);
}
