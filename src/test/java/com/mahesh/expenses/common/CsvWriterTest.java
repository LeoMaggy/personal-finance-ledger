package com.mahesh.expenses.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvWriterTest {

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+91 98450", "-5", "@SUM(A1)", "\tcmd"})
    void textStartingWithAFormulaCharacterIsNeutralised(String note) {
        assertThat(CsvWriter.cell(note)).startsWith("'");
    }

    @Test
    void formulaWithCommasIsPrefixedThenQuoted() {
        assertThat(CsvWriter.cell("=HYPERLINK(\"https://x\",\"click\")"))
                .isEqualTo("\"'=HYPERLINK(\"\"https://x\"\",\"\"click\"\")\"");
    }

    @Test
    void ordinaryTextIsUntouched() {
        assertThat(CsvWriter.cell("Swiggy order")).isEqualTo("Swiggy order");
    }

    @Test
    void commasQuotesAndLineBreaksAreQuoted() {
        assertThat(CsvWriter.cell("Rent, \"October\"")).isEqualTo("\"Rent, \"\"October\"\"\"");
        assertThat(CsvWriter.cell("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    @Test
    void numbersStayNumbersEvenWhenNegative() {
        assertThat(CsvWriter.cell(new BigDecimal("-500.00"))).isEqualTo("-500.00");
        assertThat(CsvWriter.cell(new BigDecimal("1E+3"))).isEqualTo("1000");
    }

    @Test
    void rowJoinsCellsAndEndsWithCrlf() {
        assertThat(CsvWriter.row(Arrays.asList("a", null, 5))).isEqualTo("a,,5\n");
    }
}