package com.mahesh.expenses.budget;

import com.mahesh.expenses.budget.BudgetDtos.*;
import com.mahesh.expenses.common.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final ZoneId zoneId;

    public BudgetController(BudgetService budgetService, @Value("${app.reminders.zone}") String zoneId) {
        this.budgetService = budgetService;
        this.zoneId = ZoneId.of(zoneId);
    }

    /** PUT, not POST: setting a category's budget twice leaves the same result (naturally idempotent). */
    @PutMapping("/{categoryId}")
    public BudgetResponse setBudget(@PathVariable("categoryId") Long categoryId, @Valid @RequestBody SetBudgetRequest request) {
        return budgetService.set(categoryId, request);
    }

    @GetMapping
    public BudgetReport report(@RequestParam(name = "month", required = false) String month) {
        return budgetService.report(parseMonth(month));
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(zoneId);
        }
        try{
            return YearMonth.parse(month);
        }catch(DateTimeParseException e){
            throw new BadRequestException("Invalid month format. Expected format: YYYY-MM like 2026-10");
        }
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@PathVariable("categoryId") Long categoryId) {
        budgetService.delete(categoryId);
    }

}
