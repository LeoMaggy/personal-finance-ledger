package com.mahesh.expenses.transaction;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /** One query with joins instead of 1 + 3N: loads the accounts and category with each row. */
    @EntityGraph(attributePaths = {"account", "toAccount", "category"})
    Page<Transaction> findByUserIdOrderByTransactionDateDescIdDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "toAccount", "category"})
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}