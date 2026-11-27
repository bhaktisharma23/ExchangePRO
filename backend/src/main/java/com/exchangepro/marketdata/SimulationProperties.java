package com.exchangepro.marketdata;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Price-simulator settings ({@code exchangepro.market-data.simulation.*}).
 *
 * @param enabled              run the tick scheduler. false = simulator state is initialised but prices only
 *                             move when {@code tick()} is called (used by tests)
 * @param tickInterval         wall-clock time between price updates
 * @param timeScale            simulation speed: simulated market time per real second. 1 = realistic volatility,
 *                             2 = prices move as much in 1 real minute as in 2 minutes of a real market
 * @param seed                 random seed; the same seed always produces the same price paths
 * @param meanReversionPerYear strength of the pull back towards the reference price (keeps long runs plausible)
 * @param spreadBasisPoints    simulated bid/ask spread (1 bp = 0.01 %)
 * @param backfillDays         days of daily history generated on first start
 * @param backfillMinutes      minutes of 1-minute history generated on first start
 */
@Validated
@ConfigurationProperties(prefix = "exchangepro.market-data.simulation")
public record SimulationProperties(
        boolean enabled,
        @NotNull Duration tickInterval,
        @DecimalMin("0.01") @DecimalMax("10000") double timeScale,
        long seed,
        @DecimalMin("0") @DecimalMax("50") double meanReversionPerYear,
        @Min(1) @Max(500) int spreadBasisPoints,
        @Min(0) @Max(3650) int backfillDays,
        @Min(0) @Max(20_160) int backfillMinutes) {

    /** Simulated market time, in years, that elapses during {@code realSeconds} of wall-clock time. */
    public double simulatedYears(double realSeconds) {
        return realSeconds * timeScale / SECONDS_PER_YEAR;
    }

    /** The simulated market trades around the clock (no sessions/holidays), so a year is 365 full days. */
    public static final double SECONDS_PER_YEAR = 365.0 * 24 * 3600;
}
