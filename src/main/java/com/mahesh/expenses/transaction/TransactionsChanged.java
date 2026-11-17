package com.mahesh.expenses.transaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Published whenever a user's transactions change; the report cache listens and evicts those months. */
public record TransactionsChanged(Long userId, Set<YearMonth> months) {

    public static TransactionsChanged of(Long userId, LocalDate... dates) {
        return new TransactionsChanged(userId,
                Arrays.stream(dates).map(YearMonth::from).collect(Collectors.toSet()));
    }
}