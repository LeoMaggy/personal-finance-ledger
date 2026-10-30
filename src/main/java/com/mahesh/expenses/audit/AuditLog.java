package com.mahesh.expenses.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "audit_log")
public class AuditLog {

    private static final int MAX_DETAILS = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction auditAction;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = MAX_DETAILS)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog(Long userId, AuditAction action, String entityType, Long entityId, String details) {
        this.userId = userId;
        this.auditAction = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details == null || details.length() <= MAX_DETAILS
                ? details : details.substring(0, MAX_DETAILS);
        this.createdAt = Instant.now();
    }
}
