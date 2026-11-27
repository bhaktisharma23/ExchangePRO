package com.exchangepro.config;

import com.exchangepro.auth.ProblemDetailsSecurityHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security rules.
 *
 * <ul>
 *   <li><b>Stateless</b>: no server-side HTTP session. Each request proves who it is with a JWT in the
 *       {@code Authorization: Bearer ...} header, verified by Spring's OAuth2 resource-server support.</li>
 *   <li><b>CSRF protection disabled</b>: CSRF abuses cookies that browsers attach automatically. Our API
 *       authenticates with a header that browsers never add on their own. The only cookie (refresh token) is
 *       {@code SameSite=Strict} and scoped to {@code /api/v1/auth}.</li>
 *   <li><b>Deny by default</b>: anything not explicitly public requires authentication.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** BCrypt cost 12 (2^12 rounds, roughly 0.2-0.3 s per hash): slow enough to resist offline cracking. */
    static final int BCRYPT_STRENGTH = 12;

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               JwtAuthenticationConverter jwtAuthenticationConverter,
                                               ProblemDetailsSecurityHandlers handlers) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/system/status").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        // WebSocket handshake. Phase 3 serves only public, read-only, simulated market data;
                        // destinations are restricted in WebSocketConfig. Per-user channels with token
                        // authentication arrive in Phase 8.
                        .requestMatchers(HttpMethod.GET, "/ws", "/ws/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(handlers.authenticationEntryPoint())
                        .accessDeniedHandler(handlers.accessDeniedHandler()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(handlers.authenticationEntryPoint())
                        .accessDeniedHandler(handlers.accessDeniedHandler()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
