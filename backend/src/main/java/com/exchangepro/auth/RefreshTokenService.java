package com.exchangepro.auth;

import com.exchangepro.auth.RefreshToken.RevocationReason;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <h2>Why refresh tokens?</h2>
 * Access tokens (JWTs) are valid until they expire and cannot be revoked, so they live only 15 minutes.
 * A long-lived refresh token, kept in an HttpOnly cookie that JavaScript cannot read, is exchanged for a new
 * access token when the old one expires.
 *
 * <h2>Rotation and reuse detection</h2>
 * Every successful refresh revokes the presented token and issues a new one in the same <i>family</i>.
 * A legitimate client therefore never presents a revoked token. If a revoked token shows up again, someone
 * must have copied it, so the whole family is revoked: both the attacker and the real user are logged out,
 * and the real user signs in again with their password.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32; // 256 bits of randomness

    private final RefreshTokenRepository repository;
    private final SecurityProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, SecurityProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Starts a new token family (a new login). */
    @Transactional
    public IssuedRefreshToken issueNewFamily(UUID userId, ClientInfo client) {
        return issue(userId, UUID.randomUUID(), client);
    }

    /**
     * Exchanges a valid refresh token for a new one.
     *
     * <p>{@code noRollbackFor}: when reuse is detected we revoke the family AND throw. Without this setting the
     * exception would roll back the revocation, defeating its purpose.
     *
     * @return the user id and the replacement token
     * @throws InvalidRefreshTokenException if the token is unknown, expired, revoked or reused
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken, ClientInfo client) {
        Instant now = Instant.now(clock);
        RefreshToken current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            if (RevocationReason.ROTATED.name().equals(current.getRevocationReason())) {
                int revoked = repository.revokeFamily(current.getFamilyId(), now, RevocationReason.REUSE_DETECTED.name());
                log.warn("Refresh token reuse detected for user {} (family {}); revoked {} active token(s)",
                        current.getUserId(), current.getFamilyId(), revoked);
            }
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken next = issue(current.getUserId(), current.getFamilyId(), client);
        current.revoke(now, RevocationReason.ROTATED, next.id());
        return new Rotation(current.getUserId(), next);
    }

    /** Logout: revokes the presented token's whole family. Unknown tokens are ignored (logout is idempotent). */
    @Transactional
    public void revokeFamilyOf(String rawToken) {
        repository.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token ->
                repository.revokeFamily(token.getFamilyId(), Instant.now(clock), RevocationReason.LOGOUT.name()));
    }

    /** Revokes a family (e.g. the account was disabled while a session was active). */
    @Transactional
    public void revokeFamily(UUID familyId, RevocationReason reason) {
        repository.revokeFamily(familyId, Instant.now(clock), reason.name());
    }

    private IssuedRefreshToken issue(UUID userId, UUID familyId, ClientInfo client) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.refreshToken().ttl());
        String rawToken = newRawToken();
        RefreshToken token = new RefreshToken(userId, familyId, hash(rawToken), now, expiresAt,
                client.ip(), client.userAgent());
        repository.save(token);
        return new IssuedRefreshToken(token.getId(), familyId, rawToken, expiresAt);
    }

    private String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 is appropriate here (unlike for passwords) because the token is 256 random bits: it cannot be
     * guessed or brute-forced, so a slow hash like BCrypt would add cost without adding security.
     */
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /** A newly issued token. {@code rawToken} is the only copy of the secret value; it is never stored. */
    public record IssuedRefreshToken(UUID id, UUID familyId, String rawToken, Instant expiresAt) {
        @Override
        public String toString() {
            return "IssuedRefreshToken[id=" + id + ", familyId=" + familyId + ", expiresAt=" + expiresAt + "]";
        }
    }

    public record Rotation(UUID userId, IssuedRefreshToken next) {}
}
