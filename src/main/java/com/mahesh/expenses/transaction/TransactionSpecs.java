package com.mahesh.expenses.transaction;

import com.mahesh.expenses.common.BadRequestException;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clause from whichever filters were supplied. Each Specification is one condition;
 * allOf joins them with AND. Absent filters add nothing, so there is no "if param != null" SQL concatenation
 * and every value is a bind parameter (no SQL injection).
 */
public final class TransactionSpecs {

    private TransactionSpecs() {
    }

    public static Specification<Transaction> matching(Long userId, TransactionSearch search) {
        validate(search);
        List<Specification<Transaction>> specs = new ArrayList<>();
        //Always first, never optional: only the logged-in user's transactions
        specs.add((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        if(search.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), search.from()));
        }
        if(search.to() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), search.to()));
        }
        if(search.type() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("type"), search.type()));
        }
        if(search.accountId() != null) {
            specs.add((root, query, cb) ->
                    cb.or(
                            cb.equal(root.get("account").get("id"), search.accountId()),
                            cb.equal(root.join("toAccount", JoinType.LEFT).get("id"), search.accountId())
                    ));
        }
        if(search.categoryId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category").get("id"), search.categoryId()));
        }
        if(search.minAmount() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), search.minAmount()));
        }
        if(search.maxAmount() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), search.maxAmount()));
        }
        if(search.q() != null && !search.q().isBlank()) {
            String likePattern = "%" + escapeLike(search.q().strip().toLowerCase(Locale.ROOT)) + "%";
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("note")), likePattern, '\\'));
        }


        return Specification.allOf(specs);
    }

    /** "50%" must find the text "50%", not "50 anything". Escape the LIKE wildcards % and _ and the escape char. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void validate(TransactionSearch s) {
        if (s.from() != null && s.to() != null && s.from().isAfter(s.to())) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        if (s.minAmount() != null && s.maxAmount() != null && s.minAmount().compareTo(s.maxAmount()) > 0) {
            throw new BadRequestException("'minAmount' must not be greater than 'maxAmount'");
        }
        if (s.q() != null && s.q().length() > 100) {
            throw new BadRequestException("'q' can be at most 100 characters");
        }
    }
}
