package com.exchangepro.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Loads the token with {@code SELECT ... FOR UPDATE}. If two requests try to rotate the same token at the
     * same moment, the second one waits for the first to commit and then sees the token as already revoked.
     * Without the lock both could succeed and the user would end up with two valid tokens.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now, t.revocationReason = :reason, t.version = t.version + 1
             where t.familyId = :familyId and t.revokedAt is null""")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);
}
