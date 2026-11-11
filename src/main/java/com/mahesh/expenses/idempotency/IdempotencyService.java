package com.mahesh.expenses.idempotency;

import com.mahesh.expenses.common.BadRequestException;
import com.mahesh.expenses.common.BusinessRuleException;
import com.mahesh.expenses.common.ConflictException;
import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.transaction.Transaction;
import com.mahesh.expenses.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 100;

    private final IdempotencyRepository idempotencyRepository;
    private final TransactionRepository transactions;
    private final CurrentUser currentUser;

    public IdempotencyService(IdempotencyRepository idempotencyRepository, TransactionRepository transactions,
                              CurrentUser currentUser) {
        this.idempotencyRepository = idempotencyRepository;
        this.transactions = transactions;
        this.currentUser = currentUser;
    }

    /**
     * Runs {@code action} at most once per (user, key). MANDATORY: it must join the caller's
     * transaction, so the key row and the money movement commit or roll back together.
     * A failed request (e.g. insufficient balance) rolls the key back too, so the client may retry.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Outcome execute(String key, String requestFingerprint, Supplier<Transaction> action) {
        if (key == null || key.isBlank()) {
            return new Outcome(action.get(), false);          // no key: plain, non-idempotent request
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most %d characters".formatted(MAX_KEY_LENGTH));
        }

        Long userId = currentUser.id();
        String hash = sha256(requestFingerprint);

        if (idempotencyRepository.tryInsert(userId, key, hash) == 0) {
            // Seen before: replay the original result instead of running the action again
            IdempotencyRecord existing = idempotencyRepository.findByUserIdAndIdemKey(userId, key)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key vanished: " + key));
            if (!existing.getRequestHash().equals(hash)) {
                throw new BusinessRuleException("This Idempotency-Key was already used for a different request");
            }
            if (existing.getTransactionId() == null) {
                throw new ConflictException("The original transaction for this Idempotency-Key was deleted");
            }
            Transaction original = transactions.findByIdAndUserId(existing.getTransactionId(), userId)
                    .orElseThrow(() -> new ConflictException("The original transaction for this Idempotency-Key was deleted"));
            return new Outcome(original, true);
        }

        Transaction created = action.get();
        idempotencyRepository.complete(userId, key, created.getId());
        return new Outcome(created, false);
    }

    static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }

    /** The transaction, and whether it came from an earlier request with the same key. */
    public record Outcome(Transaction transaction, boolean replayed) {
    }
}