package com.mahesh.expenses.budget;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Published inside the expense transaction; handled only after that transaction commits. */
public record BudgetThresholdReached(String email, String categoryName, int threshold,
                                     BigDecimal spent, BigDecimal limit, YearMonth month) {
}
