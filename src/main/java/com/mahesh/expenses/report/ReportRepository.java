package com.mahesh.expenses.report;

import com.mahesh.expenses.transaction.Transaction;
import com.mahesh.expenses.transaction.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Read-only aggregate queries. Extends the plain Repository marker, not JpaRepository,
 * so it exposes no save/delete: reports can't change data.
 * Transfers are left out on purpose: moving money between your own accounts is neither income nor spending.
 */
public interface ReportRepository extends Repository<Transaction, Long> {


    @Query("""
            select new com.mahesh.expenses.report.MonthlyTotal(year(t.transactionDate), month(t.transactionDate), t.type, sum(t.amount))
            from Transaction t
            where t.user.id = :userId and t.type in :types and t.transactionDate >= :from and t.transactionDate < :to
            group by year(t.transactionDate), month(t.transactionDate), t.type
            order by year(t.transactionDate), month(t.transactionDate)
            """)
    List<MonthlyTotal> getMonthlyTotals(@Param("userId") Long userId,
                                        @Param("types") Collection<TransactionType> types,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    @Query("""
            select new com.mahesh.expenses.report.CategoryTotal(c.name, sum(t.amount), count(t))
            from Transaction t join t.category c
            where t.user.id = :userId and t.type in :types and t.transactionDate >= :from and t.transactionDate < :to
            group by c.name
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> getCategoryTotals(@Param("userId") Long userId,
                                          @Param("types") TransactionType type,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);
}
