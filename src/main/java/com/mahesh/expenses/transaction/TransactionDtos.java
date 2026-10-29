package com.mahesh.expenses.transaction;

import com.mahesh.expenses.account.Account;
import com.mahesh.expenses.category.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TransactionDtos {
    private TransactionDtos() {
    }

    public record TransactionRequest(
            @NotNull Long accountId,
            @NotNull Long categoryId,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotNull @PastOrPresent LocalDate date,
            @Size(max = 255) String note) {

    }

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotNull @PastOrPresent LocalDate date,
            @Size(max = 255) String note) {
    }

    public record TransactionResponse(
            Long id,
            TransactionType type,
            BigDecimal amount,
            Long accountId,
            String accountName,
            Long categoryId,
            String categoryName,
            LocalDate date,
            String note) {

        public static TransactionResponse from(Transaction transaction) {
            Account to = transaction.getToAccount();
            Category category = transaction.getCategory();
            return new TransactionResponse(
                    transaction.getId(),
                    transaction.getType(),
                    transaction.getAmount(),
                    to == null ? null : to.getId(),
                    to == null ? null : to.getName(),
                    category == null ? null : category.getId(),
                    category == null ? null : category.getName(),
                    transaction.getTransactionDate(),
                    transaction.getNote()
            );
        }
    }
}
