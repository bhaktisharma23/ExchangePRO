package com.exchangepro.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end authentication, authorization and account tests over real HTTP against a real PostgreSQL 16.
 * Every test uses fresh, random e-mail addresses, so tests are independent although they share one database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@ActiveProfiles("test")
@SuppressWarnings({"unchecked", "rawtypes"})
class AuthFlowIT {

    private static final String PASSWORD = "Password123";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtEncoder jwtEncoder;

    // ------------------------------------------------------------------ helpers

    private static String newEmail() {
        return "trader-" + UUID.randomUUID() + "@example.com";
    }

    private ResponseEntity<Map> post(String path, Object body, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<Map> register(String email) {
        return post("/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD, "displayName", "Test Trader"), null);
    }

    private ResponseEntity<Map> login(String email, String password) {
        return post("/api/v1/auth/login", Map.of("email", email, "password", password), null);
    }

    private ResponseEntity<Map> refresh(String cookie) {
        return post("/api/v1/auth/refresh", null, cookie);
    }

    private ResponseEntity<Map> get(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private static String accessToken(ResponseEntity<Map> authResponse) {
        return (String) authResponse.getBody().get("accessToken");
    }

    /** "ep_refresh=abc; Path=/api/v1/auth; ..." -> "ep_refresh=abc" (what a browser would send back). */
    private static String refreshCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private static String code(ResponseEntity<Map> response) {
        return (String) response.getBody().get("code");
    }

    // ------------------------------------------------------------------ registration & accounts

    @Test
    void registrationCreatesUserAccountWithOneLakhAndSignsIn() {
        String email = newEmail();

        ResponseEntity<Map> response = register(email);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("tokenType", "Bearer").containsEntry("expiresIn", 900);
        assertThat((Map<String, Object>) response.getBody().get("user"))
                .containsEntry("email", email).containsEntry("role", "USER").containsEntry("displayName", "Test Trader");
        assertThat(response.getBody()).doesNotContainKey("refreshToken"); // only ever in the HttpOnly cookie
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");

        ResponseEntity<Map> account = get("/api/v1/accounts/me", accessToken(response));
        assertThat(account.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(account.getBody())
                .containsEntry("currency", "INR")
                .containsEntry("availableBalance", "100000.0000")   // exact decimal string, not a float
                .containsEntry("reservedBalance", "0.0000")
                .containsEntry("totalBalance", "100000.0000");
    }

    @Test
    void refreshCookieIsHttpOnlySecureSameSiteStrictAndScopedToAuthPath() {
        String setCookie = register(newEmail()).getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).startsWith("ep_refresh=")
                .contains("HttpOnly").contains("Secure").contains("SameSite=Strict").contains("Path=/api/v1/auth")
                .contains("Max-Age=604800");
    }

    @Test
    void passwordIsStoredOnlyAsBcryptHash() {
        String email = newEmail();
        register(email);

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);

        assertThat(hash).startsWith("$2a$12$").doesNotContain(PASSWORD);
    }

    @Test
    void emailsAreCaseInsensitiveAndUnique() {
        String email = newEmail();
        register(email);

        ResponseEntity<Map> duplicate = register("  " + email.toUpperCase() + " ");

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(code(duplicate)).isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(login(email.toUpperCase(), PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidRegistrationIsRejectedWithFieldErrors() {
        ResponseEntity<Map> response = post("/api/v1/auth/register",
                Map.of("email", "not-an-email", "password", "short", "displayName", "<script>"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(code(response)).isEqualTo("VALIDATION_FAILED");
        List<Map<String, String>> errors = (List<Map<String, String>>) response.getBody().get("errors");
        assertThat(errors).extracting(e -> e.get("field")).contains("email", "password", "displayName");
    }

    @Test
    void clientCannotChooseItsOwnRoleOrBalance() {
        // Unknown JSON properties are rejected outright (fail-on-unknown-properties), so a "role" or
        // "balance" field can never be smuggled into registration.
        ResponseEntity<Map> response = post("/api/v1/auth/register", Map.of("email", newEmail(),
                "password", PASSWORD, "displayName", "Sneaky", "role", "ADMIN"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------ login

    @Test
    void wrongPasswordAndUnknownEmailGiveIdenticalErrors() {
        String email = newEmail();
        register(email);

        ResponseEntity<Map> wrongPassword = login(email, "WrongPass999");
        ResponseEntity<Map> unknownEmail = login(newEmail(), PASSWORD);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(code(wrongPassword)).isEqualTo(code(unknownEmail)).isEqualTo("INVALID_CREDENTIALS");
        assertThat(wrongPassword.getBody().get("detail")).isEqualTo(unknownEmail.getBody().get("detail"));
    }

    @Test
    void disabledUserCannotLogIn() {
        String email = newEmail();
        register(email);
        jdbc.update("UPDATE users SET enabled = false WHERE email = ?", email);

        ResponseEntity<Map> response = login(email, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(code(response)).isEqualTo("ACCOUNT_DISABLED");
    }

    // ------------------------------------------------------------------ access tokens

    @Test
    void protectedEndpointsRequireAValidAccessToken() {
        ResponseEntity<Map> anonymous = get("/api/v1/accounts/me", null);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(code(anonymous)).isEqualTo("UNAUTHORIZED");
        assertThat(anonymous.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");

        String token = accessToken(register(newEmail()));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThat(get("/api/v1/accounts/me", tampered).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/v1/accounts/me", "not.a.jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredAccessTokenIsRejected() {
        String userId = (String) ((Map<String, Object>) register(newEmail()).getBody().get("user")).get("id");
        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("exchange-pro").subject(userId)
                .issuedAt(past).expiresAt(past.plusSeconds(900)).claim("roles", List.of("USER")).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();

        assertThat(get("/api/v1/accounts/me", expired).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ authorization & isolation

    @Test
    void usersOnlyEverSeeTheirOwnAccountAndProfile() {
        String aliceEmail = newEmail();
        String bobEmail = newEmail();
        String alice = accessToken(register(aliceEmail));
        String bob = accessToken(register(bobEmail));

        Object aliceAccount = get("/api/v1/accounts/me", alice).getBody().get("accountId");
        Object bobAccount = get("/api/v1/accounts/me", bob).getBody().get("accountId");
        assertThat(aliceAccount).isNotNull().isNotEqualTo(bobAccount);
        assertThat(get("/api/v1/users/me", alice).getBody()).containsEntry("email", aliceEmail);
        assertThat(get("/api/v1/users/me", bob).getBody()).containsEntry("email", bobEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(alice);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> updated = http.exchange("/api/v1/users/me", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("displayName", "Alice Renamed"), headers), Map.class);
        assertThat(updated.getBody()).containsEntry("displayName", "Alice Renamed");
        assertThat(get("/api/v1/users/me", bob).getBody()).containsEntry("displayName", "Test Trader");
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() {
        ResponseEntity<Map> response = get("/api/v1/admin/users", accessToken(register(newEmail())));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(code(response)).isEqualTo("FORBIDDEN");
    }

    @Test
    void bootstrapAdminCanListUsersButHasNoTradingAccount() {
        String someUser = newEmail();
        register(someUser);
        ResponseEntity<Map> adminLogin = login("admin@exchangepro.test", "AdminPass123");
        assertThat(adminLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) adminLogin.getBody().get("user")).containsEntry("role", "ADMIN");
        String admin = accessToken(adminLogin);

        ResponseEntity<Map> users = get("/api/v1/admin/users?size=100", admin);
        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) users.getBody().get("content");
        assertThat(content).extracting(u -> u.get("email")).contains(someUser);
        assertThat(content).allSatisfy(u -> assertThat(u).doesNotContainKey("passwordHash"));

        assertThat(get("/api/v1/accounts/me", admin).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ refresh tokens

    @Test
    void refreshRotatesTokenAndIssuesNewAccessToken() {
        String firstCookie = refreshCookie(register(newEmail()));

        ResponseEntity<Map> refreshed = refresh(firstCookie);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondCookie = refreshCookie(refreshed);
        assertThat(secondCookie).isNotEqualTo(firstCookie);
        assertThat(get("/api/v1/accounts/me", accessToken(refreshed)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refresh(secondCookie).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reusingARotatedRefreshTokenRevokesTheWholeSession() {
        String original = refreshCookie(register(newEmail()));
        String legitimateNext = refreshCookie(refresh(original));

        // An attacker replays the stolen, already-rotated token...
        ResponseEntity<Map> replay = refresh(original);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(code(replay)).isEqualTo("INVALID_REFRESH_TOKEN");
        assertThat(replay.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        // ...so the legitimate successor is revoked too: the family is dead, the user must sign in again.
        assertThat(refresh(legitimateNext).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() {
        String cookie = refreshCookie(register(newEmail()));

        ResponseEntity<Map> logout = post("/api/v1/auth/logout", null, cookie);

        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logout.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("ep_refresh=;").contains("Max-Age=0");
        assertThat(refresh(cookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Logout is idempotent.
        assertThat(post("/api/v1/auth/logout", null, cookie).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void refreshWithoutCookieIsRejected() {
        ResponseEntity<Map> response = refresh(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(code(response)).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void refreshTokensAreStoredHashedNeverInPlaintext() {
        String email = newEmail();
        String cookieValue = refreshCookie(register(email)).substring("ep_refresh=".length());

        List<String> stored = jdbc.queryForList("""
                SELECT t.token_hash FROM refresh_tokens t JOIN users u ON u.id = t.user_id WHERE u.email = ?""",
                String.class, email);

        assertThat(stored).containsExactly(RefreshTokenService.hash(cookieValue)).doesNotContain(cookieValue);
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    void concurrentRegistrationsWithSameEmailCreateExactlyOneUserAndAccount() throws Exception {
        String email = newEmail();
        int threads = 8;

        List<HttpStatus> statuses = runConcurrently(threads, () -> (HttpStatus) register(email).getStatusCode());

        assertThat(statuses).filteredOn(s -> s == HttpStatus.CREATED).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == HttpStatus.CONFLICT).hasSize(threads - 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM accounts a JOIN users u ON u.id = a.user_id WHERE u.email = ?""",
                Integer.class, email)).isEqualTo(1);
    }

    @Test
    void concurrentRefreshesOfTheSameTokenSucceedAtMostOnce() throws Exception {
        String cookie = refreshCookie(register(newEmail()));

        List<HttpStatus> statuses = runConcurrently(5, () -> (HttpStatus) refresh(cookie).getStatusCode());

        // The row lock (SELECT ... FOR UPDATE) serialises the rotations: one wins, the rest see a rotated
        // token, which is treated as reuse.
        assertThat(statuses).filteredOn(s -> s == HttpStatus.OK).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == HttpStatus.UNAUTHORIZED).hasSize(4);
    }

    /** Starts all tasks at the same instant (latch) to maximise contention. */
    private static <T> List<T> runConcurrently(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
