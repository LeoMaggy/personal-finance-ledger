package com.mahesh.expenses.transaction;

import java.math.BigDecimal;

/** One row of a "spend per category" query, built directly by JPQL: select new ...CategorySpend(...). */
public record CategorySpend(Long categoryId, BigDecimal total) {
}
