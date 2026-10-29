package com.mahesh.expenses.account;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 50) String name,
            @NotNull AccountType type,
            @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code, e.g. INR") String currency,
            @Digits(integer = 17, fraction = 2) BigDecimal openingBalance) {
    }

    public record AccountResponse(Long id, String name, AccountType type, String currency, BigDecimal balance) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getName(), a.getType(), a.getCurrency(), a.getBalance());
        }
    }
}
