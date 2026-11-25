package com.exchangepro.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Authentication settings ({@code exchangepro.security.*}).
 *
 * @param jwt          access-token (JWT) settings
 * @param refreshToken refresh-token and cookie settings
 */
@Validated
@ConfigurationProperties(prefix = "exchangepro.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull RefreshToken refreshToken) {

    /**
     * @param secret         HMAC-SHA256 signing key. At least 32 characters (256 bits); from {@code JWT_SECRET}.
     * @param issuer         value of the {@code iss} claim; tokens from another issuer are rejected
     * @param accessTokenTtl lifetime of an access token. Short, because a JWT cannot be revoked before it expires.
     */
    public record Jwt(@NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
                      @NotBlank String issuer,
                      @NotNull Duration accessTokenTtl) {

        @Override
        public String toString() {
            return "Jwt[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + ", secret=****]";
        }
    }

    /**
     * @param ttl          lifetime of a refresh token (and of its cookie)
     * @param cookieName   name of the HttpOnly cookie carrying the refresh token
     * @param cookieSecure send the cookie only over HTTPS. Browsers treat http://localhost as secure, so this
     *                     can stay {@code true} in local development.
     */
    public record RefreshToken(@NotNull Duration ttl, @NotBlank String cookieName, boolean cookieSecure) {}
}
