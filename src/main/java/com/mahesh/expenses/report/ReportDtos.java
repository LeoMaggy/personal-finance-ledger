package com.mahesh.expenses.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record CategoryShare(String category, BigDecimal total, long transactions, BigDecimal percent) {
    }

    public record MonthlySummary(YearMonth month, BigDecimal income,
                                 BigDecimal expense, BigDecimal net,
                                 BigDecimal savingsRatePercent, List<CategoryShare> expenseByCategory) {

        public MonthlySummary {
            expenseByCategory = List.copyOf(expenseByCategory);
        }
    }

    public record MonthTrend(YearMonth month, BigDecimal income, BigDecimal expense, BigDecimal net) {

    }
}
