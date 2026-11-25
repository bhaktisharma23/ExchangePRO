package com.exchangepro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exchangepro.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.BadJwtException;

class AccessTokenServiceTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    private static SecurityProperties props(String secret, String issuer) {
        return new SecurityProperties(new SecurityProperties.Jwt(secret, issuer, Duration.ofMinutes(15)),
                new SecurityProperties.RefreshToken(Duration.ofDays(7), "ep_refresh", true));
    }

    private AccessTokenService serviceAt(Instant now, SecurityProperties p) {
        return new AccessTokenService(jwtConfig.jwtEncoder(jwtConfig.jwtSigningKey(p)), p,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private JwtDecoder decoder(SecurityProperties p) {
        return jwtConfig.jwtDecoder(jwtConfig.jwtSigningKey(p), p);
    }

    private final SecurityProperties props = props("unit-test-secret-0123456789abcdefghij", "exchange-pro");

    @Test
    void issuesVerifiableTokenWithSubjectRolesAndExpiry() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        var issued = serviceAt(now, props).issue(userId, Role.ADMIN);
        Jwt jwt = decoder(props).decode(issued.value());

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("exchange-pro");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(issued.expiresInSeconds()).isEqualTo(900);
        assertThat(jwt.getClaims()).doesNotContainKey("email"); // no personal data in a readable token
    }

    @Test
    void expiredTokenIsRejected() {
        var issued = serviceAt(Instant.now().minus(Duration.ofHours(1)), props).issue(UUID.randomUUID(), Role.USER);

        assertThatThrownBy(() -> decoder(props).decode(issued.value()))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        var foreign = props("attacker-secret-0123456789abcdefghijklmnop", "exchange-pro");
        var issued = serviceAt(Instant.now(), foreign).issue(UUID.randomUUID(), Role.ADMIN);

        assertThatThrownBy(() -> decoder(props).decode(issued.value())).isInstanceOf(BadJwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        var otherIssuer = props("unit-test-secret-0123456789abcdefghij", "someone-else");
        var issued = serviceAt(Instant.now(), otherIssuer).issue(UUID.randomUUID(), Role.USER);

        assertThatThrownBy(() -> decoder(props).decode(issued.value())).isInstanceOf(JwtValidationException.class);
    }
}
