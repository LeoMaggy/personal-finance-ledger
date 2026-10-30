package com.mahesh.expenses.audit;

import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUser currentUser;

    public AuditService(AuditLogRepository auditLogRepository, CurrentUser currentUser) {
        this.auditLogRepository = auditLogRepository;
        this.currentUser = currentUser;
    }

    /**
     * Records an event for the logged-in user, inside the caller's transaction (REQUIRED).
     * If the business operation rolls back, this row rolls back with it.
     */
    @Transactional
    public void record(AuditAction action, String entityType, Long entityId, String details){
        auditLogRepository.save(new AuditLog(currentUser.id(), action, entityType, entityId, details));
    }

    /** Same, for when there's no logged-in user yet, e.g. right after sign-up. */
    @Transactional
    public void recordFor(Long userId, AuditAction action, String entityType, Long entityId, String details){
        auditLogRepository.save(new AuditLog(userId, action, entityType, entityId, details));
    }

    /**
     * Commits on its own (REQUIRES_NEW), whatever happens to the caller.
     * Used for login attempts: a failed login must be recorded even though the request fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTransaction(Long userId, AuditAction action, String entityType, Long entityId, String details){
        auditLogRepository.save(new AuditLog(userId, action, entityType, entityId, details));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEntryResponse> myAuditTrail(Pageable pageable) {
        return PageResponse.from(auditLogRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(currentUser.id(), pageable)
                .map(AuditEntryResponse::from));
    }

}
