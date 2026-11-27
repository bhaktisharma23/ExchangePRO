package com.exchangepro.marketdata.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.exchangepro.instrument.InstrumentDefinition;
import com.exchangepro.marketdata.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SimulatedInstrumentTest {

    private static final BigDecimal TICK = new BigDecimal("0.05");
    private static final double DT = 1.0 / (365 * 24 * 3600);
    private final InstrumentDefinition def = new InstrumentDefinition(UUID.randomUUID(), "TEST", "Test Ltd", TICK,
            new BigDecimal("1000"), new BigDecimal("0.25"), 1_000_000);

    private SimulatedInstrument newInstrument(Instant now) {
        PricePath path = new PricePath(1, def.referencePrice(), def.referencePrice(), def.annualVolatility(), TICK, 2);
        return new SimulatedInstrument(def, path, 5, null, new BigDecimal("990.0000"), now);
    }

    @Test
    void quoteHasConsistentDerivedFields() {
        Instant now = Instant.parse("2026-01-15T05:00:10Z");
        SimulatedInstrument sim = newInstrument(now);

        Quote q = sim.tick(now, DT, 10).snapshot().quote();

        assertThat(q.change()).isEqualByComparingTo(q.lastPrice().subtract(q.previousClose()));
        assertThat(q.changePercent().scale()).isEqualTo(2);
        assertThat(q.bid()).isLessThanOrEqualTo(q.lastPrice());
        assertThat(q.ask()).isGreaterThan(q.lastPrice());
        assertThat(q.ask().subtract(q.bid()).remainder(TICK).signum()).isZero();
        assertThat(q.high()).isGreaterThanOrEqualTo(q.low());
        assertThat(q.timestamp()).isEqualTo(now);
    }

    @Test
    void minuteCandleClosesWhenANewMinuteStarts() {
        Instant t0 = Instant.parse("2026-01-15T05:00:10Z");
        SimulatedInstrument sim = newInstrument(t0);

        var first = sim.tick(t0, DT, 10);
        var sameMinute = sim.tick(t0.plusSeconds(20), DT, 10);
        var nextMinute = sim.tick(t0.plusSeconds(55), DT, 10);

        assertThat(first.closedMinute()).isNull();
        assertThat(sameMinute.closedMinute()).isNull();
        assertThat(nextMinute.closedMinute()).isNotNull();
        assertThat(nextMinute.closedMinute().openTime()).isEqualTo(Instant.parse("2026-01-15T05:00:00Z"));
        assertThat(nextMinute.closedMinute().close()).isEqualByComparingTo(sameMinute.snapshot().quote().lastPrice());
        assertThat(nextMinute.snapshot().formingMinute().openTime()).isEqualTo(Instant.parse("2026-01-15T05:01:00Z"));
    }

    @Test
    void dayRollsOverAtIstMidnightAndPreviousCloseUpdates() {
        Instant beforeMidnightIst = Instant.parse("2026-01-15T18:29:50Z"); // 23:59:50 IST
        SimulatedInstrument sim = newInstrument(beforeMidnightIst);
        BigDecimal lastOfDay = sim.tick(beforeMidnightIst, DT, 10).snapshot().quote().lastPrice();

        var afterMidnight = sim.tick(Instant.parse("2026-01-15T18:30:05Z"), DT, 10);

        assertThat(afterMidnight.closedDay()).isNotNull();
        assertThat(afterMidnight.closedDay().openTime()).isEqualTo(Instant.parse("2026-01-14T18:30:00Z"));
        Quote q = afterMidnight.snapshot().quote();
        assertThat(q.previousClose()).isEqualByComparingTo(lastOfDay);
        assertThat(q.open()).isEqualByComparingTo(q.lastPrice()); // first tick of the new day
    }
}
