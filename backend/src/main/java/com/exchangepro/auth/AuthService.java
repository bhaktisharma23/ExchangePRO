package com.exchangepro.auth;

import com.exchangepro.account.AccountService;
import com.exchangepro.auth.AccessTokenService.IssuedAccessToken;
import com.exchangepro.auth.RefreshTokenService.IssuedRefreshToken;
import com.exchangepro.auth.RefreshTokenService.Rotation;
import com.exchangepro.common.error.ErrorCode;
import com.exchangepro.common.error.ExchangeProException;
import com.exchangepro.user.Role;
import com.exchangepro.user.UserCredentials;
import com.exchangepro.user.UserService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates registration, login, token refresh and logout. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    /** Hash of a random value, compared against when the e-mail is unknown (see {@link #login}). */
    private final String dummyPasswordHash;

    public AuthService(UserService userService, AccountService accountService, PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokenService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Creates the user AND their virtual trading account (INR 1,00,000) in ONE database transaction, then
     * signs the user in. If anything fails, neither the user nor the account is stored.
     */
    @Transactional
    public AuthResult register(RegisterRequest request, ClientInfo client) {
        if (!PasswordPolicy.fitsBcrypt(request.password())) {
            throw new ExchangeProException(ErrorCode.VALIDATION_FAILED, "Password is too long");
        }
        String hash = passwordEncoder.encode(request.password());
        UserCredentials user = userService.createUser(request.email(), hash, request.displayName(), Role.USER);
        accountService.openAccount(user.id());
        log.info("Registered user {}", user.id());
        return signIn(user, refreshTokenService.issueNewFamily(user.id(), client));
    }

    /**
     * Verifies e-mail and password.
     *
     * <p>Security details:
     * <ul>
     *   <li>The same error is returned for "unknown e-mail" and "wrong password", so attackers cannot discover
     *       which e-mails are registered.</li>
     *   <li>When the e-mail is unknown we still run a BCrypt comparison against a dummy hash. Otherwise the
     *       fast "unknown user" response would reveal the same information through timing.</li>
     * </ul>
     */
    @Transactional
    public AuthResult login(LoginRequest request, ClientInfo client) {
        UserCredentials user = userService.findCredentialsByEmail(request.email()).orElse(null);
        boolean passwordOk = PasswordPolicy.fitsBcrypt(request.password())
                && passwordEncoder.matches(request.password(), user != null ? user.passwordHash() : dummyPasswordHash);
        if (user == null || !passwordOk) {
            throw new ExchangeProException(ErrorCode.INVALID_CREDENTIALS, "Invalid e-mail or password");
        }
        if (!user.enabled()) {
            throw new ExchangeProException(ErrorCode.ACCOUNT_DISABLED, "This account has been disabled");
        }
        return signIn(user, refreshTokenService.issueNewFamily(user.id(), client));
    }

    /**
     * Rotates the refresh token and issues a new access token. Deliberately not one transaction: the rotation
     * (and any reuse-detection revocation) must be committed even if a later step fails.
     */
    public AuthResult refresh(String rawRefreshToken, ClientInfo client) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Rotation rotation = refreshTokenService.rotate(rawRefreshToken, client);
        UserCredentials user = userService.findCredentialsById(rotation.userId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!user.enabled()) {
            refreshTokenService.revokeFamily(rotation.next().familyId(), RefreshToken.RevocationReason.USER_DISABLED);
            throw new ExchangeProException(ErrorCode.ACCOUNT_DISABLED, "This account has been disabled");
        }
        return signIn(user, rotation.next());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeFamilyOf(rawRefreshToken);
        }
    }

    private AuthResult signIn(UserCredentials user, IssuedRefreshToken refreshToken) {
        IssuedAccessToken accessToken = accessTokenService.issue(user.id(), user.role());
        return new AuthResult(accessToken, refreshToken, userService.getProfile(user.id()));
    }

    /** Internal result: includes the raw refresh token, which the controller puts into the cookie. */
    public record AuthResult(IssuedAccessToken accessToken, IssuedRefreshToken refreshToken,
                             com.exchangepro.user.UserProfileResponse user) {

        AuthResponse toResponse() {
            return new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds(), user);
        }
    }
}
