package com.mahesh.expenses.account;

import com.mahesh.expenses.common.BusinessRuleException;
import com.mahesh.expenses.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    private BigDecimal balance;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Account(User user, String name, String currency, AccountType type, BigDecimal openingBalance) {
        if(openingBalance.signum() < 0 && !type.isNegativeBalanceAllowed()){
            throw new BusinessRuleException("Only a Credit Card account can have a negative opening balance. Please check the account type and opening balance.");
        }
        this.user = user;
        this.name = name;
        this.currency = currency;
        this.type = type;
        this.balance = openingBalance;
        this.createdAt = Instant.now();
    }

    /** Money leaves this account. No setter for balance: it only changes through these two methods. */
    public void debit(BigDecimal amount) {
        requirePositive(amount);
        BigDecimal newBalance = this.balance.subtract(amount);
        if(newBalance.signum() < 0 && !this.type.isNegativeBalanceAllowed()){
            throw new BusinessRuleException("Insufficient balance in '%s': available %s, needed %s".formatted(this.name, this.balance, amount));
        }
        this.balance = newBalance;
    }

    /** Money arrives in this account. */
    public void credit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Amount must be positive.");
        }
    }


}
