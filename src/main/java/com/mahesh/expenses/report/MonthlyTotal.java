package com.mahesh.expenses.report;

import com.mahesh.expenses.transaction.TransactionType;

import java.math.BigDecimal;

/**One row of the trend query: total EXPENSE or INCOME for a calendar month, for the logged-in user. */
public record MonthlyTotal(Integer year, Integer month, TransactionType type, BigDecimal total) {
}
