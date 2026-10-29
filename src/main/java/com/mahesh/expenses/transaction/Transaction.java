package com.mahesh.expenses.transaction;

import com.mahesh.expenses.account.Account;
import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.category.CategoryType;
import com.mahesh.expenses.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String note;

    @Column(name = "txn_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Transaction(User user, TransactionType type, BigDecimal amount, Account account,
                        Account toAccount, Category category, LocalDate txnDate, String note) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.account = account;
        this.toAccount = toAccount;
        this.category = category;
        this.transactionDate = txnDate;
        this.note = note;
        this.createdAt = Instant.now();
    }

    // Static factories make the three valid shapes explicit and impossible to mix up
    public static Transaction income(User user, BigDecimal amount, Account account, Category category, LocalDate txnDate, String note) {
        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException("Category type must be INCOME for income transactions.");
        }
        return new Transaction(user, TransactionType.INCOME, amount, account, null, category, txnDate, note);
    }

    public static Transaction expense(User user, BigDecimal amount, Account account, Category category, LocalDate txnDate, String note) {
        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Category type must be EXPENSE for expense transactions.");
        }
        return new Transaction(user, TransactionType.EXPENSE, amount, account, null, category, txnDate, note);
    }

    public static Transaction transfer(User user, BigDecimal amount, Account fromAccount, Account toAccount, LocalDate txnDate, String note) {
        return new Transaction(user, TransactionType.TRANSFER, amount, fromAccount, toAccount, null, txnDate, note);
    }
}
