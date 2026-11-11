package com.mahesh.expenses.notification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** All outgoing email in one place. Plain text keeps it simple; HTML templates are a later upgrade. */
@Component
public class EmailNotifier {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailNotifier(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendBillReminder(String to, String billName, String currency, BigDecimal amount,
                                 LocalDate dueDate, String accountName) {
        send(to,
                "Reminder: %s of %s %s due on %s".formatted(billName, currency, amount, DATE.format(dueDate)),
                """
                Hi,
   
                Your %s payment of %s %s is due on %s (from %s).
   
                Once you've paid it, mark it as paid in Finance Ledger so your balance stays accurate.
   
                - Finance Ledger
                """.formatted(billName, currency, amount, DATE.format(dueDate), accountName));
    }

    public void sendBudgetAlert(String to, String categoryName, int threshold, BigDecimal spent,
                                BigDecimal limit, YearMonth month) {
        String headline = threshold >= 100
                ? "You've gone over your %s budget".formatted(categoryName)
                : "You've used %d%% of your %s budget".formatted(threshold, categoryName);
        send(to,
                headline + " for " + MONTH.format(month),
                """
                Hi,
   
                %s for %s.
   
                Spent so far: %s
                Monthly limit: %s
   
                - Finance Ledger
                """.formatted(headline, MONTH.format(month), spent, limit));
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

}
