package com.exchangepro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exchangepro.auth.RefreshToken.RevocationReason;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T09:15:00Z");
    private static final ClientInfo CLIENT = new ClientInfo("127.0.0.1", "JUnit");

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final SecurityProperties properties = new SecurityProperties(
            new SecurityProperties.Jwt("x".repeat(32), "exchange-pro", Duration.ofMinutes(15)),
            new SecurityProperties.RefreshToken(Duration.ofDays(7), "ep_refresh", true));
    private final RefreshTokenService service =
            new RefreshTokenService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID userId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();

    private RefreshToken stored(Instant expiresAt) {
        return new RefreshToken(userId, familyId, "hash", NOW.minusSeconds(60), expiresAt, "1.1.1.1", "ua");
    }

    @Test
    void hashIsDeterministicSha256Hex() {
        assertThat(RefreshTokenService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void issuesRandomTokenAndStoresOnlyItsHash() {
        var first = service.issueNewFamily(userId, CLIENT);
        var second = service.issueNewFamily(userId, CLIENT);

        assertThat(first.rawToken()).hasSize(43).isNotEqualTo(second.rawToken()); // 32 bytes, base64url
        assertThat(first.familyId()).isNotEqualTo(second.familyId());
        assertThat(first.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0)).extracting("tokenHash")
                .isEqualTo(RefreshTokenService.hash(first.rawToken()));
    }

    @Test
    void rotationRevokesPresentedTokenAndIssuesReplacementInSameFamily() {
        RefreshToken current = stored(NOW.plus(Duration.ofDays(1)));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));

        var rotation = service.rotate("raw", CLIENT);

        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.next().familyId()).isEqualTo(familyId);
        assertThat(current.isRevoked()).isTrue();
        assertThat(current.getRevocationReason()).isEqualTo("ROTATED");
    }

    @Test
    void reuseOfRotatedTokenRevokesWholeFamily() {
        RefreshToken alreadyRotated = stored(NOW.plus(Duration.ofDays(1)));
        alreadyRotated.revoke(NOW.minusSeconds(10), RevocationReason.ROTATED, UUID.randomUUID());
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(alreadyRotated));

        assertThatThrownBy(() -> service.rotate("stolen", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeFamily(eq(familyId), eq(NOW), eq("REUSE_DETECTED"));
    }

    @Test
    void loggedOutTokenIsRejectedWithoutFamilyRevocation() {
        RefreshToken loggedOut = stored(NOW.plus(Duration.ofDays(1)));
        loggedOut.revoke(NOW.minusSeconds(10), RevocationReason.LOGOUT, null);
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(loggedOut));

        assertThatThrownBy(() -> service.rotate("old", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).revokeFamily(any(), any(), anyString());
    }

    @Test
    void expiredTokenIsRejected() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored(NOW)));

        assertThatThrownBy(() -> service.rotate("raw", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nope", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);
    }
}
