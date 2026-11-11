package com.mahesh.expenses.budget;

import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="budgets")
@Getter
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Budget() {
    }

    public Budget(User user, Category category, BigDecimal monthlyLimit) {
        this.user = user;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.createdAt = Instant.now();
    }

    public void changeLimit(BigDecimal newLimit) {
        this.monthlyLimit = newLimit;
    }
}
