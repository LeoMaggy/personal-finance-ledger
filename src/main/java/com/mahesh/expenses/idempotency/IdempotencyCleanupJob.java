package com.mahesh.expenses.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;


/** Keys only need to live as long as a client might retry; 24 hours is a common choice. */
@Component
public class IdempotencyCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);
    private static final Duration RETENTION = Duration.ofHours(24);

    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyCleanupJob(IdempotencyRepository idempotencyRepository){
        this.idempotencyRepository = idempotencyRepository;
    }

    @Scheduled(cron = "0 30 2 * * *", zone = "${app.reminders.zone}")    //2.30AM everyday
    @Transactional
    public void purgeExpredKeys(){
        int deleted = idempotencyRepository.deleteOlderThan(Instant.now().minus(RETENTION));
        log.info("Deleted {} expired idempotency keys", deleted);
    }
}
