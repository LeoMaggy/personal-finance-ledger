package com.mahesh.expenses.account;

public enum AccountType {
    BANK,
    CASH,
    CREDIT_CARD,
    WALLET;

    /** A credit card balance below zero means money owed; other accounts can't go negative. */
    public boolean isNegativeBalanceAllowed() {
        return this == CREDIT_CARD;
    }
}
