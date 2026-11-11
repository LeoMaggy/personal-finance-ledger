package com.mahesh.expenses.recurring;


import com.mahesh.expenses.notification.EmailNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class ReminderJob {

    public static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private static final long LOCK_ID = 42_001L;          // any number unique to this job
    private static final int MAX_REMIND_DAYS = 30;

    private final RecurringExpenseRepository recurring;
    private final EmailNotifier emailNotifier;
    private final ZoneId zone;

    public ReminderJob(RecurringExpenseRepository recurring, EmailNotifier emailNotifier, @Value("${app.reminders.zone}") ZoneId zone) {
        this.recurring = recurring;
        this.emailNotifier = emailNotifier;
        this.zone = zone;
    }

    @Scheduled(cron = "${app.reminders.cron}", zone = "${app.reminders.zone}") // every day at 9am
    @Transactional
    public void sendDueReminders() {
        if(!recurring.tryAdvisoryXactLock(LOCK_ID)){
            log.info("Reminder job already running on another instance, skipping this run");
        }
        LocalDate today = LocalDate.now(zone);
        List<RecurringExpense> candidates = recurring.findReminderCandidates(today.plusDays(MAX_REMIND_DAYS));
        log.info("Found {} recurring expenses due in the next {} days", candidates.size(), MAX_REMIND_DAYS);

        int sent = 0;
        for (RecurringExpense bill : candidates) {
            if(!bill.isReminderDue(today)){
                continue;
            }
            try{
                emailNotifier.sendBillReminder(bill.getUser().getEmail(), bill.getName(), bill.getAccount().getCurrency(), bill.getAmount(),
                        bill.getNextDueDate(), bill.getAccount().getName());
                bill.markReminded();    // saved at commit by dirty checking
                sent++;
            }catch(MailException e){
                // Not marked, so tomorrow's run tries again: at-least-once delivery
                log.warn("Could not send reminder for recurring expense {}: {}", bill.getId(), e.getMessage());
            }
        }
        log.info("Reminder job for {}: {} candidate(s), {} reminder(s) sent", today, candidates.size(), sent);
    }
}
