package com.exchangepro.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for entities that track creation/modification time and use optimistic locking.
 *
 * <p><b>Optimistic locking</b>: Hibernate adds {@code WHERE version = ?} to every UPDATE and increments the
 * version. If two transactions modify the same row concurrently, the second UPDATE matches zero rows and fails
 * with an {@code OptimisticLockingFailureException} instead of silently overwriting the first change.
 *
 * <p>A {@code null} version also tells Spring Data that the entity is new (we assign UUIDs ourselves, so the id
 * cannot be used for that), which makes {@code save()} perform a plain INSERT.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
