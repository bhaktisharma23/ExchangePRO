package com.exchangepro.config;

import com.exchangepro.marketdata.MarketDataMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed application settings bound from the {@code exchangepro.*} properties.
 *
 * <p>Values come from {@code application.yml}, which in turn reads environment variables. Because the
 * class is {@link Validated}, the application refuses to start if a required setting is missing or invalid,
 * which surfaces configuration mistakes immediately instead of at runtime.
 */
@Validated
@ConfigurationProperties(prefix = "exchangepro")
public record ExchangeProProperties(@Valid @NotNull Cors cors, @Valid @NotNull MarketData marketData) {

    /** Browser origins allowed to call the API (the Vite dev server in development). */
    public record Cors(@NotEmpty List<String> allowedOrigins) {}

    /** Which market-data source is active. SIMULATION is the default and the only fully implemented mode. */
    public record MarketData(@NotNull MarketDataMode mode) {}
}
