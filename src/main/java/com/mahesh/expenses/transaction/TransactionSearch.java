package com.mahesh.expenses.transaction;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Optional filters for GET /transactions and /transactions/export. Every field may be null (= no filter).
 * Spring binds query parameters to this record by name: ?from=2026-10-01&type=EXPENSE&q=swiggy
 */
public record TransactionSearch(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  //inclusive
    LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  //inclusive
    LocalDate to,
    TransactionType type,
    Long accountId,
    Long categoryId,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    String q){                                       // text in the note
}
