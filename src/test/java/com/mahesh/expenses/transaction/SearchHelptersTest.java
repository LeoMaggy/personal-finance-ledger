package com.mahesh.expenses.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mahesh.expenses.common.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class SearchHelpersTest {

    @Test
    void likeWildcardsAreEscaped() {
        assertThat(TransactionSpecs.escapeLike("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
    }

    @Test
    void publicSortNamesAreMappedAndATieBreakerAdded() {
        Pageable result = SortWhitelist.apply(PageRequest.of(2, 20, Sort.by(Sort.Direction.ASC, "amount")));

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getSort().toList()).containsExactly(Sort.Order.asc("amount"), Sort.Order.desc("id"));
    }

    @Test
    void noSortMeansNewestFirst() {
        Pageable result = SortWhitelist.apply(PageRequest.of(0, 20));

        assertThat(result.getSort().toList()).containsExactly(Sort.Order.desc("transactionDate"), Sort.Order.desc("id"));
    }

    @Test
    void unknownSortIsA400NotA500() {
        assertThatThrownBy(() -> SortWhitelist.apply(PageRequest.of(0, 20, Sort.by("user.passwordHash"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("user.passwordHash");
    }

    @Test
    void reversedRangesAreRejected() {
        var dates = new TransactionSearch(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1),
                null, null, null, null, null, null);
        var amounts = new TransactionSearch(null, null, null, null, null,
                new BigDecimal("500"), new BigDecimal("100"), null);

        assertThatThrownBy(() -> TransactionSpecs.matching(1L, dates)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> TransactionSpecs.matching(1L, amounts)).isInstanceOf(BadRequestException.class);
    }
}