package com.mahesh.expenses.audit;

import java.time.Instant;

public record AuditEntryResponse(Long id, AuditAction action, String entityType, Long entityId, String details, Instant createdAt) {
    public static AuditEntryResponse from(AuditLog auditLog) {
        return new AuditEntryResponse(auditLog.getId(), auditLog.getAuditAction(), auditLog.getEntityType(), auditLog.getEntityId(), auditLog.getDetails(), auditLog.getCreatedAt());
    }
}
