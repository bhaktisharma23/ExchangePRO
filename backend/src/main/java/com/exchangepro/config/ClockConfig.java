package com.exchangepro.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a {@link Clock} bean. Code that needs "now" injects the clock instead of calling
 * {@code Instant.now()} directly, so tests can freeze time (important later for order timestamps,
 * price-time priority and candle boundaries).
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
