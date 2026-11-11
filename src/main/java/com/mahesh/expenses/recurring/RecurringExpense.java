package com.mahesh.expenses.recurring;

import com.mahesh.expenses.account.Account;
import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Table(name = "recurring_expenses")
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequency frequency;

    @Column(name = "anchor_day", nullable = false)
    private int anchorDay;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "remind_days_before", nullable = false)
    private int remindDaysBefore;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_reminded_for")
    private LocalDate lastRemindedFor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecurringExpense() {
        // required by JPA
    }

    public RecurringExpense(User user, Account account, Category category, String name, BigDecimal amount,
                            Frequency frequency, LocalDate firstDueDate, int remindDaysBefore) {
        this.user = user;
        this.account = account;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.frequency = frequency;
        this.anchorDay = firstDueDate.getDayOfMonth();
        this.nextDueDate = firstDueDate;
        this.remindDaysBefore = remindDaysBefore;
        this.active = true;
        this.createdAt = Instant.now();
    }

    /** True if today falls inside the reminder window and we haven't emailed about this due date yet. */
    public boolean isReminderDue(LocalDate today) {
        return active
                && !today.isBefore(nextDueDate.minusDays(remindDaysBefore))
                && !nextDueDate.equals(lastRemindedFor);
    }

    public void markReminded() {
        this.lastRemindedFor = nextDueDate;
    }

    /** Called after paying this occurrence. */
    public void advance() {
        this.nextDueDate = frequency.next(nextDueDate, anchorDay);
    }

    public void stop() {
        this.active = false;
    }
}