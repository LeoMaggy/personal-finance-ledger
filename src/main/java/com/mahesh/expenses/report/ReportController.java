package com.mahesh.expenses.report;

import com.mahesh.expenses.common.BadRequestException;
import com.mahesh.expenses.report.ReportDtos.*;
import com.mahesh.expenses.common.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService service;
    private final CurrentUser currentUser;
    private final ZoneId zone;

    public ReportController(ReportService service, CurrentUser currentUser, @Value("${app.reminders.zone}") String zone) {
        this.service = service;
        this.currentUser = currentUser;
        this.zone = ZoneId.of(zone);
    }

    /** GET /api/v1/reports/monthly?month=2026-11 (default: current month in India) */
    @GetMapping("/monthly")
    public MonthlySummary monthly(@RequestParam(name = "month", required = false) String month) {
        return service.monthlySummary(currentUser.id(), parseMonth(month));
    }

    /** GET /api/v1/reports/trend?months=6 : income vs expense per month, ending this month */
    @GetMapping("/trend")
    public List<MonthTrend> trend(@RequestParam(name = "months", defaultValue = "6") int months,
                                  @RequestParam(name = "until", required = false) String until) {
        if (months < 1 || months > 24) {
            throw new BadRequestException("'months' must be between 1 and 24");
        }
        return service.trend(currentUser.id(), parseMonth(until), months);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(zone);
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Month must look like 2026-10");
        }
    }
}
