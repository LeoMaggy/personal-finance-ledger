package com.mahesh.expenses.idempotency;

import jakarta.persistence.*;
import java.time.Instant;

/** Read-only view of an idempotency_keys row; rows are written with native SQL (see repository). */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idem_key", nullable = false, length = 100)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "transaction_id")
    private Long transactionId;          // null if the transaction was later deleted

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
        // required by JPA
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getIdemKey() { return idemKey; }
    public String getRequestHash() { return requestHash; }
    public Long getTransactionId() { return transactionId; }
    public Instant getCreatedAt() { return createdAt; }
}