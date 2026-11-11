package com.mahesh.expenses.transaction;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /** One query with joins instead of 1 + 3N: loads the accounts and category with each row. */
    @EntityGraph(attributePaths = {"account", "toAccount", "category"})
    Page<Transaction> findByUserIdOrderByTransactionDateDescIdDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "toAccount", "category"})
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /** Total spent in one category between two dates (from inclusive, to exclusive). */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.user.id = :userId and t.category.id = :categoryId and t.type = :type
              and t.transactionDate >= :from and t.transactionDate < :to
            """)
    BigDecimal sumByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId,
                             @Param("type") TransactionType type,
                             @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Totals for every category in one query (used by the budget report). */
    @Query("""
            select new com.mahesh.expenses.transaction.CategorySpend(t.category.id, sum(t.amount))
            from Transaction t
            where t.user.id = :userId and t.type = :type and t.transactionDate >= :from and t.transactionDate < :to
            group by t.category.id
            """)
    List<CategorySpend> sumByCategoryForPeriod(@Param("userId") Long userId, @Param("type") TransactionType type,
                                               @Param("from") LocalDate from, @Param("to") LocalDate to);
}