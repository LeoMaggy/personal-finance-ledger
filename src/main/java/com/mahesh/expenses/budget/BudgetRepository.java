package com.mahesh.expenses.budget;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = {"category"})
    List<Budget> findByUserIdOrderByIdAsc(Long userId);

    @EntityGraph(attributePaths = {"category"})
    //List<Budget> findByUserIdAndCategoryIdInOrderByIdAsc(Long userId, List<Long> categoryIds);
    Optional<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);

    /** Returns 1 the first time an alert is recorded for this budget, month and threshold; 0 after that. */
    @Modifying
    @Query(value = """
            INSERT INTO budget_alerts (budget_id, month, threshold, sent_at)
            VALUES (:budgetId, :month, :threshold, now())
            ON CONFLICT (budget_id, month, threshold) DO NOTHING
            """, nativeQuery = true)
    int recordAlert(@Param("budgetId") Long budgetId, @Param("month") LocalDate month, @Param("threshold") int threshold);
}
