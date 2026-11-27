package com.exchangepro.marketdata.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricePathTest {

    private static final BigDecimal TICK = new BigDecimal("0.05");
    private static final double ONE_MINUTE_AT_2X = 120.0 / (365 * 24 * 3600);

    private static PricePath path(long seed) {
        return new PricePath(seed, new BigDecimal("1400"), new BigDecimal("1400"), new BigDecimal("0.25"), TICK, 2.0);
    }

    private static List<BigDecimal> run(PricePath p, int steps, double dt) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            prices.add(p.step(dt));
        }
        return prices;
    }

    @Test
    void sameSeedProducesIdenticalPaths() {
        assertThat(run(path(7), 500, ONE_MINUTE_AT_2X)).isEqualTo(run(path(7), 500, ONE_MINUTE_AT_2X));
    }

    @Test
    void differentSeedsProduceDifferentPaths() {
        assertThat(run(path(7), 50, ONE_MINUTE_AT_2X)).isNotEqualTo(run(path(8), 50, ONE_MINUTE_AT_2X));
    }

    @Test
    void perInstrumentSeedsDiffer() {
        assertThat(PricePath.seedFor(1, "TCS")).isNotEqualTo(PricePath.seedFor(1, "INFY"));
        assertThat(PricePath.seedFor(1, "TCS")).isEqualTo(PricePath.seedFor(1, "TCS"));
    }

    @Test
    void pricesArePositiveOnTheTickGridAtMoneyScale() {
        for (BigDecimal price : run(path(3), 2_000, ONE_MINUTE_AT_2X)) {
            assertThat(price.signum()).isPositive();
            assertThat(price.scale()).isEqualTo(4);
            assertThat(price.remainder(TICK).signum()).isZero();
        }
    }

    @Test
    void meanReversionKeepsLongRunsNearTheReference() {
        // Ten simulated years in daily steps: without mean reversion a 25 % vol path could wander far away.
        List<BigDecimal> prices = run(path(11), 3_650, 1.0 / 365);
        for (BigDecimal p : prices) {
            assertThat(p).isBetween(new BigDecimal("700"), new BigDecimal("2800"));
        }
    }

    @Test
    void zeroTimeStepDoesNotMoveThePrice() {
        PricePath p = path(5);
        assertThat(p.step(0)).isEqualByComparingTo("1400");
    }

    @Test
    void tinyPricesNeverRoundToZero() {
        PricePath p = new PricePath(1, new BigDecimal("0.05"), new BigDecimal("0.01"), new BigDecimal("0.9"), TICK, 0);
        assertThat(p.price()).isEqualByComparingTo("0.05");
        for (BigDecimal price : run(p, 200, 0.01)) {
            assertThat(price).isGreaterThanOrEqualTo(TICK);
        }
    }

    @Test
    void volumesAreNonNegativeAndAverageToTheExpectation() {
        PricePath p = path(9);
        long total = 0;
        for (int i = 0; i < 10_000; i++) {
            long v = p.volume(100);
            assertThat(v).isNotNegative();
            total += v;
        }
        assertThat(total / 10_000.0).isBetween(95.0, 105.0);
        assertThat(p.volume(0)).isZero();
    }
}
