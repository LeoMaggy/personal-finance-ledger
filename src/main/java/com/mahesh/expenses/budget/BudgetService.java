package com.mahesh.expenses.budget;

import com.mahesh.expenses.audit.AuditAction;
import com.mahesh.expenses.audit.AuditService;
import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.category.CategoryService;
import com.mahesh.expenses.category.CategoryType;
import com.mahesh.expenses.common.BusinessRuleException;
import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.common.EntityType;
import com.mahesh.expenses.common.NotFoundException;
import com.mahesh.expenses.transaction.CategorySpend;
import com.mahesh.expenses.transaction.Transaction;
import com.mahesh.expenses.transaction.TransactionRepository;
import com.mahesh.expenses.transaction.TransactionType;
import com.mahesh.expenses.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class BudgetService {

    static final List<Integer> THRESHOLDS = List.of(80, 100);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final BudgetRepository budgets;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository, CategoryService categoryService,
                         UserRepository userRepository, CurrentUser currentUser, AuditService auditService,
                         ApplicationEventPublisher eventPublisher, BudgetRepository budgets) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.budgets = budgets;
    }

    @Transactional
    public BudgetDtos.BudgetResponse set(Long categoryId, BudgetDtos.SetBudgetRequest request) {
        Category category = categoryService.getOwnedCategory(categoryId);
        if(null != category && category.getType() != null && category.getType() != CategoryType.EXPENSE) {
            throw new BusinessRuleException("Budget can only be set for expense categories");
        }

        Long userId = currentUser.id();
        Budget budget = budgetRepository.findByUserIdAndCategoryId(userId, categoryId)
                .map(existing -> {
                    existing.changeLimit(request.monthlyLimit());
                    return existing;
                })
                .orElseGet(() -> budgetRepository.save(new Budget(userRepository.getReferenceById(userId), category, request.monthlyLimit())));
        auditService.record(AuditAction.BUDGET_SET, EntityType.BUDGET.name(), budget.getId(),
                "'%s' monthly limit %s".formatted(category.getName(), request.monthlyLimit()));
        return new BudgetDtos.BudgetResponse(categoryId, category.getName(), budget.getMonthlyLimit());
    }

    @Transactional
    public void delete(Long categoryId) {
        Budget budget = budgetRepository.findByUserIdAndCategoryId(currentUser.id(), categoryId)
                .orElseThrow(() -> new NotFoundException("No budget set for category %s".formatted(categoryId)));
        budgetRepository.delete(budget);
        auditService.record(AuditAction.BUDGET_DELETED, EntityType.BUDGET.name(), budget.getId(),
                "'%s' monthly limit %s".formatted(budget.getCategory().getName(), budget.getMonthlyLimit()));
    }

    /** Spent, remaining and % used for every budget in one month: two queries in total. */
    public BudgetDtos.BudgetReport report(YearMonth month) {
        Long userId = currentUser.id();
        Map<Long, BigDecimal> spentByCategory = transactionRepository
                .sumByCategoryForPeriod(userId, TransactionType.EXPENSE, month.atDay(1), month.plusMonths(1).atDay(1))
                .stream()
                .collect(Collectors.toMap(CategorySpend::categoryId, CategorySpend::total));

        List<BudgetDtos.BudgetStatusResponse> rows = budgetRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(b -> {
                    BigDecimal limit = b.getMonthlyLimit();
                    BigDecimal spent = spentByCategory.getOrDefault(b.getCategory().getId(), BigDecimal.ZERO);
                    BigDecimal percent = percentUsed(spent, limit);
                    return new BudgetDtos.BudgetStatusResponse(b.getCategory().getId(), b.getCategory().getName(), limit,
                            spent, limit.subtract(spent), percent, statusFor(spent, limit));
                })
                .toList();
        log.info("Budget report for user {} in month {}: {}", userId, month, rows);
        return new BudgetDtos.BudgetReport(month, rows);
    }

    /** Thresholds (80, 100) that spending has reached, lowest first. */
    static List<Integer> thresholdsCrossed(BigDecimal spent, BigDecimal limit) {
        return THRESHOLDS.stream()
                .filter(t -> spent.multiply(HUNDRED).compareTo(limit.multiply(BigDecimal.valueOf(t))) >= 0)
                .toList();
    }

    static BigDecimal percentUsed(BigDecimal spent, BigDecimal limit) {
        return spent.multiply(HUNDRED).divide(limit, 1, RoundingMode.HALF_UP);
    }

    /** Decided on exact amounts, not the rounded percentage: 3999.99 of 5000 is still on track. */
    static BudgetDtos.BudgetStatus statusFor(BigDecimal spent, BigDecimal limit) {
        if (spent.compareTo(limit) > 0) {
            return BudgetDtos.BudgetStatus.EXCEEDED;
        }
        return thresholdsCrossed(spent, limit).isEmpty() ? BudgetDtos.BudgetStatus.ON_TRACK : BudgetDtos.BudgetStatus.WARNING;
    }

    /**
     * Called by TransactionService inside the expense's transaction. Records which thresholds were
     * newly crossed and publishes one event for the highest; the email goes out after commit.
     */
    @Transactional
    public void checkBudgetThresholdsAfterExpense(Transaction expense) {
        Long userId = currentUser.id();
        Long categoryId = expense.getCategory().getId();
        budgets.findByUserIdAndCategoryId(userId, categoryId).ifPresent(budget -> {
            YearMonth month = YearMonth.from(expense.getTransactionDate());
            BigDecimal spent = transactionRepository.sumByCategory(userId, categoryId, TransactionType.EXPENSE,
                    month.atDay(1), month.plusMonths(1).atDay(1));

            Integer highestNew = null;
            for (int threshold : thresholdsCrossed(spent, budget.getMonthlyLimit())) {
                if (budgets.recordAlert(budget.getId(), month.atDay(1), threshold) == 1) {
                    highestNew = threshold;
                }
            }
            if (highestNew != null) {
                String categoryName = budget.getCategory().getName();
                auditService.record(AuditAction.BUDGET_ALERT, "BUDGET", budget.getId(),
                        "%d%% of '%s' budget reached in %s: spent %s of %s"
                                .formatted(highestNew, categoryName, month, spent, budget.getMonthlyLimit()));
                String email = userRepository.findById(userId).orElseThrow().getEmail();
                eventPublisher.publishEvent(new BudgetThresholdReached(email, categoryName, highestNew,
                        spent, budget.getMonthlyLimit(), month));
            }
        });
    }

}
