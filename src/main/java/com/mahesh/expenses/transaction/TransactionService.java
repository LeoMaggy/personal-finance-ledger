package com.mahesh.expenses.transaction;

import com.mahesh.expenses.account.Account;
import com.mahesh.expenses.audit.AuditAction;
import com.mahesh.expenses.audit.AuditService;
import com.mahesh.expenses.budget.BudgetService;
import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.category.CategoryType;
import com.mahesh.expenses.common.BusinessRuleException;
import com.mahesh.expenses.idempotency.IdempotencyService;
import com.mahesh.expenses.transaction.TransactionDtos.*;
import com.mahesh.expenses.account.AccountService;
import com.mahesh.expenses.category.CategoryService;
import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.common.PageResponse;
import com.mahesh.expenses.user.User;
import com.mahesh.expenses.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mahesh.expenses.common.EntityType;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final UserRepository users;
    private final CurrentUser currentUser;
    private final TransactionRepository transactions;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final BudgetService budgetService;

public TransactionService(UserRepository users, CurrentUser currentUser, TransactionRepository transactions,
                          CategoryService categoryService, AccountService accountService, AuditService auditService, IdempotencyService idempotencyService, BudgetService budgetService) {
        this.users = users;
        this.currentUser = currentUser;
        this.transactions = transactions;
        this.categoryService = categoryService;
        this.accountService = accountService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.budgetService = budgetService;
    }

    public PageResponse<TransactionResponse> list(Pageable pageable) {
        return PageResponse.from(transactions.findByUserIdOrderByTransactionDateDescIdDesc(currentUser.id(), pageable)
                .map(TransactionResponse::from));
    }

    @Transactional
    public TransactionResult recordIncome(TransactionRequest request, String idempotencyKey) {
        return toResult(idempotencyService.execute(idempotencyKey, fingerprint(TransactionType.INCOME, request),
                () -> recordTransaction(TransactionType.INCOME, request)));
    }

    @Transactional
    public TransactionResult recordExpense(TransactionRequest request, String idempotencyKey) {
        return toResult(idempotencyService.execute(idempotencyKey, fingerprint(TransactionType.EXPENSE, request),
                () -> recordTransaction(TransactionType.EXPENSE, request)));
    }

    // Private and called from inside this class: it runs in the transaction already
    // started by recordExpense/recordIncome. A @Transactional here would be ignored.
    private Transaction recordTransaction(TransactionType transactionType, TransactionRequest request) {
        Account account = accountService.getOwnedAccount(request.accountId());
        Category category = categoryService.getOwnedCategory(request.categoryId());
        CategoryType expectedCategoryType = transactionType == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        if(category.getType() != expectedCategoryType) {
            throw new BusinessRuleException("'%s' is not an %s category".formatted(category.getName(), expectedCategoryType.name().toLowerCase()));
        }
        User user = users.getReferenceById(currentUser.id());
        Transaction transaction;
        if(transactionType == TransactionType.EXPENSE) {
            account.debit(request.amount());
            transaction = Transaction.expense(user, request.amount(), account, category, request.date(), request.note());
        }else{
            account.credit(request.amount());
            transaction = Transaction.income(user, request.amount(), account, category, request.date(), request.note());
        }
        Transaction transactionSaved = transactions.save(transaction);
        auditService.record(transactionType == TransactionType.EXPENSE ? AuditAction.EXPENSE_RECORDED : AuditAction.INCOME_RECORDED,
                EntityType.TRANSACTION.name(), transactionSaved.getId(), "%s %s, account %d, category '%s'".formatted(transactionType, request.amount(), account.getId(), category.getName()));
        if(transactionType == TransactionType.EXPENSE) {
            budgetService.checkBudgetThresholdsAfterExpense(transactionSaved);
        }
        return transactionSaved;
    }

    /**
     * Debit one account, credit the other and record the transfer: all three commit together
     * or not at all. @Version on Account turns a concurrent update into a 409, not a lost update.
     */
    @Transactional
    public TransactionResult recordTransfer(TransferRequest request, String idempotencyKey) {
        String fingerprint = String.join("|", "TRANSFER", String.valueOf(request.fromAccountId()),
                String.valueOf(request.toAccountId()), canonical(request.amount()),
                String.valueOf(request.date()), String.valueOf(request.note()));
        return toResult(idempotencyService.execute(idempotencyKey, fingerprint, () -> doTransfer(request)));
    }

    private Transaction doTransfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BusinessRuleException("Choose two different accounts for a transfer");
        }
        Account from = accountService.getOwnedAccount(request.fromAccountId());
        Account to = accountService.getOwnedAccount(request.toAccountId());
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new BusinessRuleException("Transfers between different currencies aren't supported yet");
        }

        from.debit(request.amount());              // throws before anything is saved if funds are short
        to.credit(request.amount());

        Transaction saved = transactions.save(Transaction.transfer(users.getReferenceById(currentUser.id()),
                request.amount(), from, to, request.date(), request.note()));
        auditService.record(AuditAction.TRANSFER_MADE, "TRANSACTION", saved.getId(),
                "%s from account %d to account %d".formatted(request.amount(), from.getId(), to.getId()));
        return saved;
    }

    /** Deleting a transaction reverses its effect on the balances, in the same transaction. */
    @Transactional
    public void delete(Long id){
        Transaction txn = transactions.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new BusinessRuleException("Transaction %d not found".formatted(id)));
        Account account = txn.getAccount();
        Account toAccount = txn.getToAccount();
        switch(txn.getType()) {
            case EXPENSE -> account.credit(txn.getAmount());
            case INCOME -> account.debit(txn.getAmount());
            case TRANSFER -> {
                account.credit(txn.getAmount());
                toAccount.debit(txn.getAmount());
            }
            default -> throw new IllegalStateException("Unexpected transaction type: " + txn.getType());
        }
        transactions.delete(txn);
        auditService.record(AuditAction.TRANSACTION_DELETED, EntityType.TRANSACTION.name(), txn.getId(),
                "%s %s reversed".formatted(txn.getType(), txn.getAmount()));
    }

    /** The request's content as one string; its hash detects a key reused for a different request. */
    private static String fingerprint(TransactionType type, TransactionRequest r) {
        return String.join("|", type.name(), String.valueOf(r.accountId()), String.valueOf(r.categoryId()),
                canonical(r.amount()), String.valueOf(r.date()), String.valueOf(r.note()));
    }

    /** 100, 100.0 and 100.00 are the same amount and must produce the same fingerprint. */
    private static String canonical(java.math.BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static TransactionResult toResult(IdempotencyService.Outcome outcome) {
        return new TransactionResult(TransactionResponse.from(outcome.transaction()), outcome.replayed());
    }

}












