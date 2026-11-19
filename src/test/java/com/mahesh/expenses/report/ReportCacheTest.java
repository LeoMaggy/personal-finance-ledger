package com.mahesh.expenses.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahesh.expenses.transaction.TransactionsChanged;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class ReportCacheTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    @Autowired ReportService reportService;
    @Autowired ApplicationEventPublisher events;
    @Autowired TransactionTemplate tx;
    @Autowired CacheManager cacheManager;
    @MockitoBean ReportRepository reports;

    @BeforeEach
    void emptyCacheAndStubQueries() {
        cacheManager.getCache(ReportService.MONTHLY_SUMMARY_QUERY).clear();
        when(reports.getMonthlyTotals(any(), any(), any(), any())).thenReturn(List.of());
        when(reports.getCategoryTotals(any(), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void secondCallIsServedFromTheCache() {
        reportService.monthlySummary(1L, NOV);
        reportService.monthlySummary(1L, NOV);

        verify(reports, times(1)).getMonthlyTotals(eq(1L), any(), eq(NOV.atDay(1)), any());
    }

    @Test
    void usersNeverShareACacheEntry() {
        reportService.monthlySummary(1L, NOV);
        reportService.monthlySummary(2L, NOV);

        verify(reports, times(1)).getMonthlyTotals(eq(2L), any(), eq(NOV.atDay(1)), any());
    }

    @Test
    void aCommittedChangeEvictsOnlyItsMonth() {
        reportService.monthlySummary(1L, NOV);
        reportService.monthlySummary(1L, OCT);

        tx.executeWithoutResult(status -> events.publishEvent(new TransactionsChanged(1L, Set.of(NOV))));
        reportService.monthlySummary(1L, NOV);
        reportService.monthlySummary(1L, OCT);

        verify(reports, times(2)).getMonthlyTotals(eq(1L), any(), eq(NOV.atDay(1)), any());   // recomputed
        verify(reports, times(1)).getMonthlyTotals(eq(1L), any(), eq(OCT.atDay(1)), any());   // still cached
    }

    @Test
    void aRolledBackChangeEvictsNothing() {
        reportService.monthlySummary(1L, NOV);

        tx.executeWithoutResult(status -> {
            events.publishEvent(new TransactionsChanged(1L, Set.of(NOV)));
            status.setRollbackOnly();
        });
        reportService.monthlySummary(1L, NOV);

        verify(reports, times(1)).getMonthlyTotals(eq(1L), any(), eq(NOV.atDay(1)), any());
    }
}