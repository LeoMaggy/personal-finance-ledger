package com.mahesh.expenses.recurring;

import com.mahesh.expenses.account.Account;
import com.mahesh.expenses.audit.AuditAction;
import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.category.CategoryType;
import com.mahesh.expenses.common.BusinessRuleException;
import com.mahesh.expenses.common.EntityType;
import com.mahesh.expenses.common.NotFoundException;
import com.mahesh.expenses.recurring.RecurringDtos.*;
import com.mahesh.expenses.account.AccountService;
import com.mahesh.expenses.audit.AuditService;
import com.mahesh.expenses.category.CategoryService;
import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.transaction.TransactionDtos.*;
import com.mahesh.expenses.transaction.TransactionService;
import com.mahesh.expenses.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecurringService {

    private static final int DEFAULT_REMIND_DAYS = 3;

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuditService auditService;

    public RecurringService(RecurringExpenseRepository recurringExpenseRepository,
                            UserRepository userRepository,
                            CurrentUser currentUser,
                            CategoryService categoryService,
                            AccountService accountService,
                            TransactionService transactionService,
                            AuditService auditService) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.categoryService = categoryService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.auditService = auditService;
    }

    public List<RecurringResponse> listRecurringExpenses() {
        var user = userRepository.getReferenceById(currentUser.id());
        return recurringExpenseRepository.findByUserIdAndActiveTrueOrderByNextDueDateAsc(user.getId())
                .stream()
                .map(RecurringResponse::from)
                .toList();
    }

    @Transactional
    public RecurringResponse create(CreateRecurringRequest request) {
        Account account = accountService.getOwnedAccount(request.accountId());
        Category category = categoryService.getOwnedCategory(request.categoryId());
        if(category.getType() != CategoryType.EXPENSE) {
            throw new BusinessRuleException("'%s' is not an expense category".formatted(category.getName()));
        }
        int remindDays = request.remindDaysBefore() == null ? DEFAULT_REMIND_DAYS : request.remindDaysBefore();
        RecurringExpense saved = recurringExpenseRepository.save(new RecurringExpense(userRepository.getReferenceById(currentUser.id()),
                account, category, request.name().strip(), request.amount(), request.frequency(),
                request.firstDueDate(), remindDays));
        auditService.record(AuditAction.RECURRING_CREATED, EntityType.RECURRING.name(), saved.getId(),
                "%s %s %s, first due %s".formatted(saved.getName(), saved.getFrequency(), saved.getAmount(), saved.getNextDueDate()));
        return RecurringResponse.from(saved);
    }

    /**
     * Records the occurrence due on {@code request.dueDate()} as an expense, then moves to the next one.
     * The idempotency key is derived from the bill and the due date, so paying the same occurrence twice
     * (a double tap, or a retry after a timeout) replays the first payment instead of charging again.
     */
    @Transactional
    public PayRecurringResponse pay(Long id, PayRecurringRequest request) {
        RecurringExpense bill = getOwned(id);
        if (!bill.isActive()) {
            throw new BusinessRuleException("'%s' has been stopped".formatted(bill.getName()));
        }
        LocalDate next = bill.getNextDueDate();
        LocalDate due = request.dueDate();
        if (due.isAfter(next)) {
            throw new BusinessRuleException("The next unpaid occurrence of '%s' is due on %s".formatted(bill.getName(), next));
        }

        String key = "recurring-%d-%s".formatted(bill.getId(), due);
        TransactionResult result = transactionService.recordExpense(new TransactionRequest(
                bill.getAccount().getId(), bill.getCategory().getId(), bill.getAmount(),
                request.paidOn(), "%s (due %s)".formatted(bill.getName(), due)), key);

        if (due.isBefore(next) && !result.replayed()) {
            // An earlier occurrence with no record of payment: refuse, and roll back the expense just made
            throw new BusinessRuleException("The occurrence due on %s is already settled".formatted(due));
        }
        if (!result.replayed()) {
            bill.advance();
        }
        return new PayRecurringResponse(result.response(), bill.getNextDueDate(), result.replayed());
    }

    @Transactional
    public void stop(Long id) {
        RecurringExpense bill = getOwned(id);
        if(bill.isActive()) {
            bill.stop();
            auditService.record(AuditAction.RECURRING_STOPPED, "RECURRING", id, bill.getName());
        }
    }

    private RecurringExpense getOwned(Long id) {
        return recurringExpenseRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Recurring expense %d not found".formatted(id)));
    }
}
