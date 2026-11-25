package com.exchangepro.auth;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the refresh-token cookie.
 *
 * <ul>
 *   <li>{@code HttpOnly}: invisible to JavaScript, so an XSS bug cannot steal it.</li>
 *   <li>{@code Secure}: only sent over HTTPS (browsers also allow http://localhost).</li>
 *   <li>{@code SameSite=Strict}: never sent on requests started by another site, which blocks CSRF against the
 *       refresh/logout endpoints.</li>
 *   <li>{@code Path=/api/v1/auth}: sent only to the auth endpoints, not with every API call.</li>
 * </ul>
 */
@Component
class RefreshTokenCookies {

    static final String PATH = "/api/v1/auth";

    private final SecurityProperties properties;

    RefreshTokenCookies(SecurityProperties properties) {
        this.properties = properties;
    }

    String name() {
        return properties.refreshToken().cookieName();
    }

    /** Cookie lifetime equals the refresh-token lifetime; the server-side expiry is still authoritative. */
    ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(properties.refreshToken().ttl()).build();
    }

    ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name(), value)
                .httpOnly(true)
                .secure(properties.refreshToken().cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
