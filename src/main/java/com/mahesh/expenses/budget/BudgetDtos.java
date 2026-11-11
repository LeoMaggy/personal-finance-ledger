package com.mahesh.expenses.budget;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.YearMonth;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    public record SetBudgetRequest(@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal monthlyLimit) {
    }

    public record BudgetResponse(Long categoryId, String categoryName, BigDecimal monthlyLimit) {
    }

    public enum BudgetStatus { ON_TRACK, WARNING, EXCEEDED }

    public record BudgetStatusResponse(Long categoryId, String categoryName, BigDecimal monthlyLimit,
                                       BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed,
                                       BudgetStatus status) {
    }

    public record BudgetReport(YearMonth month, java.util.List<BudgetStatusResponse> budgets) {
    }
}