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

import com.exchangepro.account.AccountService;
import com.exchangepro.common.error.ErrorCode;
import com.exchangepro.common.error.ExchangeProException;
import com.exchangepro.user.Role;
import com.exchangepro.user.UserCredentials;
import com.exchangepro.user.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final ClientInfo CLIENT = new ClientInfo("127.0.0.1", "JUnit");

    private final UserService userService = mock(UserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AccessTokenService accessTokenService = mock(AccessTokenService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthService authService;

    AuthServiceTest() {
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$dummy");
        authService = new AuthService(userService, accountService, passwordEncoder, accessTokenService,
                refreshTokenService);
    }

    private static UserCredentials user(boolean enabled) {
        return new UserCredentials(UUID.randomUUID(), "a@b.com", "$2a$12$real", "A", Role.USER, enabled);
    }

    private static ErrorCode codeOf(Throwable t) {
        return ((ExchangeProException) t).errorCode();
    }

    @Test
    void unknownEmailStillPerformsBcryptComparisonAndReturnsGenericError() {
        when(userService.findCredentialsByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@x.com", "Password1"), CLIENT))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        // Same work as for a real user, so response time does not reveal whether the e-mail exists.
        verify(passwordEncoder).matches(eq("Password1"), eq("$2a$12$dummy"));
    }

    @Test
    void wrongPasswordGivesSameGenericError() {
        UserCredentials u = user(true);
        when(userService.findCredentialsByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("WrongPass1", u.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "WrongPass1"), CLIENT))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo(ErrorCode.INVALID_CREDENTIALS))
                .hasMessage("Invalid e-mail or password");
        verify(refreshTokenService, never()).issueNewFamily(any(), any());
    }

    @Test
    void disabledUserCannotLogIn() {
        UserCredentials u = user(false);
        when(userService.findCredentialsByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Password1", u.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "Password1"), CLIENT))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    @Test
    void passwordOver72BytesIsRejectedAtRegistration() {
        String longMultibyte = "ä1".repeat(30); // 60 characters but 90 UTF-8 bytes

        assertThatThrownBy(() -> authService.register(new RegisterRequest("a@b.com", longMultibyte, "A B"), CLIENT))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo(ErrorCode.VALIDATION_FAILED));
        verify(userService, never()).createUser(anyString(), anyString(), anyString(), any());
    }

    @Test
    void refreshWithoutCookieIsRejected() {
        assertThatThrownBy(() -> authService.refresh(null, CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh(" ", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void registrationCreatesUserThenAccount() {
        UserCredentials u = user(true);
        when(userService.createUser("a@b.com", "$2a$12$dummy", "Alice", Role.USER)).thenReturn(u);
        when(refreshTokenService.issueNewFamily(u.id(), CLIENT)).thenReturn(
                new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "raw", null));
        when(accessTokenService.issue(u.id(), Role.USER)).thenReturn(
                new AccessTokenService.IssuedAccessToken("jwt", null, 900));

        var result = authService.register(new RegisterRequest("a@b.com", "Password1", "Alice"), CLIENT);

        verify(accountService).openAccount(u.id());
        assertThat(result.toResponse().accessToken()).isEqualTo("jwt");
        assertThat(result.toResponse().tokenType()).isEqualTo("Bearer");
    }
}
