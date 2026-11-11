package com.mahesh.expenses.recurring;

import com.mahesh.expenses.transaction.TransactionDtos.TransactionResponse;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class RecurringDtos {

    private RecurringDtos() {
    }

    public record CreateRecurringRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Long accountId,
            @NotNull Long categoryId,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotNull Frequency frequency,
            @NotNull LocalDate firstDueDate,
            @Min(0) @Max(30) Integer remindDaysBefore) {        // optional; defaults to 3
    }

    /** dueDate says WHICH occurrence is being paid, so a retried "pay" can never pay next month's by mistake. */
    public record PayRecurringRequest(@NotNull LocalDate dueDate, @NotNull @PastOrPresent LocalDate paidOn) {
    }

    public record RecurringResponse(Long id, String name, BigDecimal amount, Frequency frequency,
                                    LocalDate nextDueDate, int remindDaysBefore,
                                    Long accountId, String accountName, Long categoryId, String categoryName) {

        public static RecurringResponse from(RecurringExpense r) {
            return new RecurringResponse(r.getId(), r.getName(), r.getAmount(), r.getFrequency(),
                    r.getNextDueDate(), r.getRemindDaysBefore(),
                    r.getAccount().getId(), r.getAccount().getName(),
                    r.getCategory().getId(), r.getCategory().getName());
        }
    }

    public record PayRecurringResponse(TransactionResponse transaction, LocalDate nextDueDate, boolean replayed) {
    }
}