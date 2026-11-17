package com.mahesh.expenses.report;

import java.math.BigDecimal;

/**one row of total expenses or incomes for a category, for the logged-in user. */
public record CategoryTotal(String categoryName, BigDecimal total, Long transations) {
}
