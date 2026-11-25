package com.exchangepro.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Request metadata stored with refresh tokens for the audit trail.
 *
 * <p>The IP is the direct peer address. {@code X-Forwarded-For} is intentionally ignored: it is set by the
 * client and can be forged unless a trusted reverse proxy is configured (Phase 9).
 */
public record ClientInfo(String ip, String userAgent) {

    private static final int MAX_USER_AGENT = 255;

    public static ClientInfo from(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > MAX_USER_AGENT) {
            ua = ua.substring(0, MAX_USER_AGENT);
        }
        return new ClientInfo(request.getRemoteAddr(), ua);
    }
}
