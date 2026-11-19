package com.mahesh.expenses.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mahesh.expenses.report.ReportDtos.MonthTrend;
import com.mahesh.expenses.report.ReportDtos.MonthlySummary;
import com.mahesh.expenses.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    private final ReportRepository reports = mock(ReportRepository.class);
    private final ReportService service = new ReportService(reports);

    @Test
    void percentRoundsToOneDecimalAndNeverDividesByZero() {
        assertThat(ReportService.percent(new BigDecimal("42550"), new BigDecimal("85000"))).isEqualByComparingTo("50.1");
        assertThat(ReportService.percent(new BigDecimal("100"), BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    @Test
    void summaryComputesNetSavingsRateAndCategoryShares() {
        YearMonth nov = YearMonth.of(2026, 11);
        when(reports.getMonthlyTotals(eq(7L), any(), eq(nov.atDay(1)), eq(nov.plusMonths(1).atDay(1)))).thenReturn(List.of(
                new MonthlyTotal(2026, 11, TransactionType.INCOME, new BigDecimal("85000.00")),
                new MonthlyTotal(2026, 11, TransactionType.EXPENSE, new BigDecimal("42450.00"))));
        when(reports.getCategoryTotals(eq(7L), eq(TransactionType.EXPENSE), any(), any())).thenReturn(List.of(
                new CategoryTotal("Rent", new BigDecimal("25000.00"), 1L),
                new CategoryTotal("Food", new BigDecimal("17450.00"), 14L)));

        MonthlySummary s = service.monthlySummary(7L, nov);

        assertThat(s.net()).isEqualByComparingTo("42550");
        assertThat(s.savingsRatePercent()).isEqualByComparingTo("50.1");
        assertThat(s.expenseByCategory()).extracting(ReportDtos.CategoryShare::percent)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("58.9"), new BigDecimal("41.1"));
    }

    @Test
    void trendFillsEmptyMonthsWithZeros() {
        when(reports.getMonthlyTotals(eq(7L), any(), any(), any())).thenReturn(List.of(
                new MonthlyTotal(2026, 10, TransactionType.INCOME, new BigDecimal("85000")),
                new MonthlyTotal(2026, 10, TransactionType.EXPENSE, new BigDecimal("39800"))));

        List<MonthTrend> trend = service.trend(7L, YearMonth.of(2026, 11), 3);

        assertThat(trend).extracting(MonthTrend::month)
                .containsExactly(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11));
        assertThat(trend.get(0).income()).isEqualByComparingTo("0");
        assertThat(trend.get(1).net()).isEqualByComparingTo("45200");
        assertThat(trend.get(2).expense()).isEqualByComparingTo("0");
    }
}