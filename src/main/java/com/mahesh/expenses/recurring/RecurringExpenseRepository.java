package com.mahesh.expenses.recurring;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByUserIdAndActiveTrueOrderByNextDueDateAsc(Long userId);

    @EntityGraph(attributePaths = {"account", "category"})
    Optional<RecurringExpense> findByIdAndUserId(Long id, Long userId);

    /** Every user's bills that might need a reminder, with user and account loaded in the same query. */
    @Query("""
            select r from RecurringExpense r join fetch r.user join fetch r.account
            where r.active = true and r.nextDueDate <= :horizon
              and (r.lastRemindedFor is null or r.lastRemindedFor <> r.nextDueDate)
            order by r.nextDueDate
            """)
    List<RecurringExpense> findReminderCandidates(@Param("horizon") LocalDate horizon);

    /**
     * PostgreSQL advisory lock, held until the current transaction ends. Only one app instance
     * gets true; the others skip the job. (ShedLock is the library that packages this idea.)
     */
    @Query(value = "SELECT pg_try_advisory_xact_lock(:lockId)", nativeQuery = true)
    boolean tryAdvisoryXactLock(@Param("lockId") long lockId);
}
