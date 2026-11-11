package com.mahesh.expenses.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Claims the key. Returns 1 if this request is the first, 0 if the key already exists.
     * If another request is inserting the same key right now, PostgreSQL makes this wait
     * until that transaction commits (then 0) or rolls back (then 1). No exception, no race.
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (user_id, idem_key, request_hash, created_at)
            VALUES (:userId, :key, :hash, now())
            ON CONFLICT (user_id, idem_key) DO NOTHING
            """, nativeQuery = true)
    int tryInsert(@Param("userId") Long userId, @Param("key") String key, @Param("hash") String hash);

    Optional<IdempotencyRecord> findByUserIdAndIdemKey(Long userId, String idemKey);

    @Modifying
    @Query("""
            update IdempotencyRecord r set r.transactionId = :transactionId
            where r.userId = :userId and r.idemKey = :key
            """)
    int complete(@Param("userId") Long userId, @Param("key") String key,
                 @Param("transactionId") Long transactionId);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
