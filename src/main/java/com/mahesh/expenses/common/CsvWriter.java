package com.mahesh.expenses.common;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes CSV that is safe to open in Excel. A cell starting with = + - @ (or tab / carriage return)
 * is treated by Excel as a formula: a description like =HYPERLINK("http://evil",...) would run when the
 * user opens the export. Such text cells get a leading apostrophe, which Excel shows as plain text.
 */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static String row(List<?> cells) {
        return cells.stream()
                .map(CsvWriter::cell)
                .collect(Collectors.joining(",")) + "\r\n";
    }

    static String cell(Object value) {
        if(value == null) {
            return "";
        }
        if(value instanceof Number number) {
            return number instanceof BigDecimal big ? big.toPlainString() : number.toString();
        }
        String text = value.toString();
        if(!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text; // Excel treats a leading apostrophe as "plain text"
        }

        // Escape any double quotes by doubling them, and wrap the whole cell in double quotes.
        // This is the only way to make Excel treat a cell as plain text, even if it starts with = + - @.
        if(text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }
}
