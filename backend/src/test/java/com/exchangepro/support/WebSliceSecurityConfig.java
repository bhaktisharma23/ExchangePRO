package com.exchangepro.support;

import com.exchangepro.auth.ProblemDetailsSecurityHandlers;
import com.exchangepro.config.ExchangeProProperties;
import com.exchangepro.config.SecurityConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Brings the real security rules into {@code @WebMvcTest} slices. The slice needs a {@code JwtDecoder}; tests
 * provide it with {@code @MockitoBean} (or use {@code SecurityMockMvcRequestPostProcessors.jwt()}).
 */
@TestConfiguration
@Import({SecurityConfig.class, ProblemDetailsSecurityHandlers.class})
@EnableConfigurationProperties(ExchangeProProperties.class)
public class WebSliceSecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }
}
