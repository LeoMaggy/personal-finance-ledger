package com.mahesh.expenses.transaction;

import com.mahesh.expenses.account.Account;
import com.mahesh.expenses.category.Category;
import com.mahesh.expenses.category.CategoryType;
import com.mahesh.expenses.common.BusinessRuleException;
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

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final UserRepository users;
    private final CurrentUser currentUser;
    private final TransactionRepository transactions;
    private final CategoryService categoryService;
    private final AccountService accountService;

public TransactionService(UserRepository users, CurrentUser currentUser, TransactionRepository transactions, CategoryService categoryService, AccountService accountService) {
        this.users = users;
        this.currentUser = currentUser;
        this.transactions = transactions;
        this.categoryService = categoryService;
        this.accountService = accountService;
    }

    public PageResponse<TransactionResponse> list(Pageable pageable) {
        return PageResponse.from(transactions.findByUserIdOrderByTransactionDateDescIdDesc(currentUser.id(), pageable)
                .map(TransactionResponse::from));
    }

    @Transactional
    public TransactionResponse recordIncome(TransactionRequest request) {
        return recordTransaction(TransactionType.INCOME, request);
    }

    @Transactional
    public TransactionResponse recordExpense(TransactionRequest request) {
        return recordTransaction(TransactionType.EXPENSE, request);
    }

    // Private and called from inside this class: it runs in the transaction already
    // started by recordExpense/recordIncome. A @Transactional here would be ignored.
    private TransactionResponse recordTransaction(TransactionType transactionType, TransactionRequest request) {
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
        transactions.save(transaction);
        return TransactionResponse.from(transaction);
    }

    /**
     * Debit one account, credit the other and record the transfer: all three commit together
     * or not at all. @Version on Account turns a concurrent update into a 409, not a lost update.
     */
    @Transactional
    public TransactionResponse recordTransfer(TransferRequest request) {
        if(request.fromAccountId() == request.toAccountId()) {
            throw new BusinessRuleException("Cannot transfer to the same account, please choose two different accounts for the transfer.");
        }
        Account fromAccount = accountService.getOwnedAccount(request.fromAccountId());
        Account toAccount = accountService.getOwnedAccount(request.toAccountId());
        if(!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BusinessRuleException("Cannot transfer between accounts with different currencies: '%s' is %s, '%s' is %s".formatted(
                    fromAccount.getName(), fromAccount.getCurrency(),
                    toAccount.getName(), toAccount.getCurrency()
            ));

        }
        User user = users.getReferenceById(currentUser.id());
        fromAccount.debit(request.amount());
        toAccount.credit(request.amount());
        Transaction transaction = Transaction.transfer(user, request.amount(), fromAccount, toAccount, request.date(), request.note());
        transactions.save(transaction);
        return TransactionResponse.from(transaction);
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
    }

}












