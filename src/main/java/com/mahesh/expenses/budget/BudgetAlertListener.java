package com.mahesh.expenses.budget;

import com.mahesh.expenses.notification.EmailNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BudgetAlertListener {
    private static final Logger log = LoggerFactory.getLogger(BudgetAlertListener.class);

    private final EmailNotifier emailNotifier;

    public BudgetAlertListener(EmailNotifier emailNotifier) {
        this.emailNotifier = emailNotifier;
    }


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBudgetThresholdReached(BudgetThresholdReached event) {
        try{
            log.info("Budget threshold reached: {}", event);
            emailNotifier.sendBudgetAlert(event.email(), event.categoryName(), event.threshold(),
                    event.spent(), event.limit(), event.month());
        } catch (MailException e) {
            log.warn("Budget alert email to {} failed: {}", event.email(), e.getMessage());
        }
    }
}
