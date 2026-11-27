package com.exchangepro.marketdata.simulation;

import com.exchangepro.common.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.SplittableRandom;

/**
 * Deterministic random price path for one instrument.
 *
 * <h2>Model</h2>
 * Geometric Brownian motion with mean reversion (an Ornstein-Uhlenbeck process on the log price):
 * <pre>
 *   d(ln P) = theta * (ln P_ref - ln P) dt  -  sigma^2/2 dt  +  sigma * sqrt(dt) * Z,    Z ~ N(0, 1)
 * </pre>
 * <ul>
 *   <li>{@code sigma} (annual volatility) controls how much the price wiggles.</li>
 *   <li>{@code theta} pulls the price back towards the reference so that a simulation left running for months
 *       stays in a plausible range instead of drifting to 0 or infinity.</li>
 *   <li>Working in log space keeps the price strictly positive.</li>
 * </ul>
 *
 * <h2>Determinism</h2>
 * All randomness comes from one {@link SplittableRandom} created from a seed. Same seed + same sequence of
 * calls = identical prices, which makes tests and demos reproducible.
 *
 * <h2>Floating point</h2>
 * The random process itself uses {@code double} (it is a statistical model, not accounting). Every value that
 * leaves this class is converted to {@link BigDecimal} and snapped to the instrument's tick grid, so all money
 * arithmetic elsewhere stays exact.
 *
 * <p>Not thread-safe: owned by the single simulator thread.
 */
public final class PricePath {

    private final SplittableRandom random;
    private final BigDecimal tickSize;
    private final double sigma;
    private final double theta;
    private final double logReference;
    private double logPrice;
    private BigDecimal price;

    public PricePath(long seed, BigDecimal referencePrice, BigDecimal startPrice, BigDecimal annualVolatility,
                     BigDecimal tickSize, double meanReversionPerYear) {
        this.random = new SplittableRandom(seed);
        this.tickSize = tickSize;
        this.sigma = annualVolatility.doubleValue();
        this.theta = meanReversionPerYear;
        this.logReference = Math.log(referencePrice.doubleValue());
        this.price = toTick(startPrice);
        this.logPrice = Math.log(this.price.doubleValue());
    }

    /** Seed for one instrument: global seed mixed with the symbol, so every instrument gets its own path. */
    public static long seedFor(long globalSeed, String symbol) {
        return globalSeed ^ (symbol.hashCode() * 0x9E3779B97F4A7C15L);
    }

    /** Advances the path by {@code dtYears} of simulated time and returns the new price (on the tick grid). */
    public BigDecimal step(double dtYears) {
        if (dtYears > 0) {
            double shock = sigma * Math.sqrt(dtYears) * random.nextGaussian();
            logPrice += theta * (logReference - logPrice) * dtYears - 0.5 * sigma * sigma * dtYears + shock;
            price = toTick(BigDecimal.valueOf(Math.exp(logPrice)));
        }
        return price;
    }

    public BigDecimal price() {
        return price;
    }

    /**
     * Random traded volume with the given expected value (log-normal, mean = expected, never negative).
     */
    public long volume(double expected) {
        if (expected <= 0) {
            return 0;
        }
        double factor = Math.exp(0.5 * random.nextGaussian() - 0.125); // E[factor] = 1
        return Math.round(expected * factor);
    }

    /** Random order size between 1 and {@code max} (inclusive). */
    public long size(long max) {
        return 1 + random.nextLong(Math.max(1, max));
    }

    /** Rounds to the nearest multiple of the tick size (minimum one tick), at the platform money scale. */
    BigDecimal toTick(BigDecimal value) {
        BigDecimal ticks = value.divide(tickSize, 0, RoundingMode.HALF_EVEN);
        if (ticks.signum() <= 0) {
            ticks = BigDecimal.ONE;
        }
        return ticks.multiply(tickSize).setScale(Money.SCALE, Money.ROUNDING);
    }
}
