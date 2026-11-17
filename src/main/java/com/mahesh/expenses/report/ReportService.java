package com.mahesh.expenses.report;

import com.mahesh.expenses.report.ReportDtos.*;
import com.mahesh.expenses.transaction.TransactionType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {

    public static final String MONTHLY_SUMMARY_QUERY = "monthlySummary";
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Cached per user AND month: the key "7:2026-11" can never serve user 7's numbers to user 8.
     * Evicted by ReportCacheEvictor after any change commits; the TTL is the safety net.
     */
    @Cacheable(cacheNames = MONTHLY_SUMMARY_QUERY, key = "#userId + ':' + #month")
    public MonthlySummary monthlySummary(Long userId, YearMonth month) {
        var from = month.atDay(1);
        var to = month.plusMonths(1).atDay(1);                      // exclusive: works for 28, 30 or 31 days

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (MonthlyTotal t : reportRepository.getMonthlyTotals(userId, EnumSet.of(TransactionType.EXPENSE, TransactionType.INCOME), from, to)) {
            if (t.type() == TransactionType.INCOME) {
                income = income.add(t.total());        // add, not =: correct even if a range spans months
            } else if (t.type() == TransactionType.EXPENSE) {
                expense = expense.add(t.total());
            }
        }
        BigDecimal totalExpense = expense;
        List<CategoryShare> shares = reportRepository.getCategoryTotals(userId, TransactionType.EXPENSE, from, to).stream()
                .map(c -> new CategoryShare(c.categoryName(), c.total(), c.transations(), percent(c.total(), totalExpense)))
                .toList();
        BigDecimal net = income.subtract(expense);
        return new MonthlySummary(month, income, expense, net, percent(net, income), shares);
    }

    /** Income vs expense for the last N months up to and including 'last'. Months with nothing show zeros. */
    public List<MonthTrend> trend(Long userId, YearMonth last, int months) {
        YearMonth first = last.minusMonths(months - 1L);
        Map<YearMonth, BigDecimal[]> byMonth = new HashMap<>();
        for (MonthlyTotal t : reportRepository.getMonthlyTotals(userId, EnumSet.of(TransactionType.EXPENSE, TransactionType.INCOME),
                first.atDay(1), last.plusMonths(1).atDay(1))) {
            BigDecimal[] pair = byMonth.computeIfAbsent(YearMonth.of(t.year(), t.month()),
                    m -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            pair[t.type() == TransactionType.INCOME ? 0 : 1] = t.total();
        }
        List<MonthTrend> result = new ArrayList<>(months);
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            BigDecimal[] pair = byMonth.getOrDefault(m, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            result.add(new MonthTrend(m, pair[0], pair[1], pair[0].subtract(pair[1])));
        }
        return result;
    }

    /** part / whole as a percentage with 1 decimal; 0 when whole is 0 (no division by zero). */
    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }
}
