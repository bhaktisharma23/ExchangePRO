package com.exchangepro.auth;

import com.exchangepro.auth.AuthService.AuthResult;
import com.exchangepro.common.error.ApiProblems;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookies cookies;
    public AuthController(AuthService authService, RefreshTokenCookies cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Register and receive a virtual trading account with INR 1,00,000")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest http) {
        return respond(HttpStatus.CREATED, authService.register(request, ClientInfo.from(http)));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with e-mail and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return respond(HttpStatus.OK, authService.login(request, ClientInfo.from(http)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh-token cookie for a new access token (rotates the cookie)")
    public ResponseEntity<?> refresh(HttpServletRequest http) {
        try {
            return respond(HttpStatus.OK, authService.refresh(readCookie(http), ClientInfo.from(http)));
        } catch (InvalidRefreshTokenException ex) {
            // Same 401 body as the global handler, plus deletion of the dead cookie so the browser stops sending it.
            return ResponseEntity.status(ex.errorCode().status())
                    .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                    .body(ApiProblems.create(ex.errorCode(), ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session and delete the refresh-token cookie")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        authService.logout(readCookie(http));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthResult result) {
        var cookie = cookies.create(result.refreshToken().rawToken());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.toResponse());
    }

    private String readCookie(HttpServletRequest http) {
        var cookie = WebUtils.getCookie(http, cookies.name());
        return cookie != null ? cookie.getValue() : null;
    }
}
