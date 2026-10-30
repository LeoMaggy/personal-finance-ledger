package com.mahesh.expenses.account;

import com.mahesh.expenses.audit.AuditAction;
import com.mahesh.expenses.audit.AuditService;
import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.common.EntityType;
import com.mahesh.expenses.common.NotFoundException;
import com.mahesh.expenses.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mahesh.expenses.account.AccountDtos.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accounts;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public AccountService(AccountRepository accounts, UserRepository users, CurrentUser currentUser, AuditService auditService) {
        this.accounts = accounts;
        this.users = users;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    public List<AccountResponse> list(){
        return accounts.findByUserIdOrderByNameAsc(currentUser.id())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse get(Long id) {
        return AccountResponse.from(getOwnedAccount(id));
    }

    /** Used by other services. Throws 404 for a missing account AND for someone else's account. */
    public Account getOwnedAccount(Long id) {
        return accounts.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Account %d not found".formatted(id)));
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Long userId = currentUser.id();
        String name = request.name().strip();
        if (accounts.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new IllegalArgumentException("Account with name '%s' already exists".formatted(name));
        }
        String currency = request.currency() == null ? "INR" : request.currency().toUpperCase();
        BigDecimal openingBalance = request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance();
        Account account = accounts.save(new Account(users.getReferenceById(userId), name, currency, request.type(), openingBalance));
        auditService.record(AuditAction.ACCOUNT_CREATED, EntityType.ACCOUNT.name(), account.getId(),
                "%s '%s', opening balance %s %s".formatted(account.getType(), name, openingBalance, currency));
        return AccountResponse.from(account);
    }
}
