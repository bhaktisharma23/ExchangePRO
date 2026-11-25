package com.exchangepro.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Server-side record of a refresh token. Only the SHA-256 hash of the token is stored. */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken {

    enum RevocationReason { ROTATED, LOGOUT, REUSE_DETECTED, USER_DISABLED }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 32)
    private String revocationReason;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Version
    private Long version;

    protected RefreshToken() {
        // for JPA
    }

    RefreshToken(UUID userId, UUID familyId, String tokenHash, Instant issuedAt, Instant expiresAt,
                 String clientIp, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    boolean isRevoked() {
        return revokedAt != null;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    void revoke(Instant now, RevocationReason reason, UUID replacedBy) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
            this.revocationReason = reason.name();
            this.replacedBy = replacedBy;
        }
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getFamilyId() {
        return familyId;
    }

    String getRevocationReason() {
        return revocationReason;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }
}
