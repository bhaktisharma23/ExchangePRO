package com.exchangepro.auth;

import com.exchangepro.user.UserProfileResponse;

/**
 * Body returned by register/login/refresh. The refresh token is NOT in the body: it is only sent as an
 * HttpOnly cookie, so page JavaScript (and therefore an XSS attack) can never read it.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserProfileResponse user) {}
