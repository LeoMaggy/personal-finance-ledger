package com.mahesh.expenses.report;

import com.mahesh.expenses.transaction.TransactionsChanged;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Drops cached summaries for exactly the months that changed, AFTER the change commits.
 * Evicting before commit would let a concurrent request re-cache the old numbers in between.
 * If the transaction rolls back, nothing changed, so this never runs.
 */
@Component
public class ReportCacheEvictor {

    private final CacheManager cacheManager;

    public ReportCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionsChanged(TransactionsChanged event) {
        Cache cache = cacheManager.getCache(ReportService.MONTHLY_SUMMARY_QUERY);
        if (cache != null) {
            event.months().forEach(month -> cache.evict(event.userId() + ":" + month));
        }
    }
}
