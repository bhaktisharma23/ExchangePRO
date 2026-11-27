package com.exchangepro.marketdata.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.exchangepro.marketdata.Candle;
import com.exchangepro.marketdata.MarketClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryGeneratorTest {

    private static final Instant END = Instant.parse("2026-01-15T09:15:00Z"); // 14:45 IST
    private static final Instant INTRADAY_START = END.minus(Duration.ofMinutes(120));
    private static final Instant HISTORY_START = MarketClock.dayStart(INTRADAY_START).minus(Duration.ofDays(30));

    private static PricePath path() {
        return new PricePath(42, new BigDecimal("500"), new BigDecimal("500"), new BigDecimal("0.3"),
                new BigDecimal("0.05"), 2.0);
    }

    private static HistoryGenerator.History generate(PricePath path) {
        return HistoryGenerator.generate(path, 1_000_000, HISTORY_START, INTRADAY_START, END,
                seconds -> seconds * 2 / (365.0 * 24 * 3600));
    }

    @Test
    void producesOneMinuteCandlesForEveryMinuteUpToTheCurrentMinute() {
        List<Candle> minutes = generate(path()).minuteCandles();

        assertThat(minutes).hasSize(120);
        assertThat(minutes.getFirst().openTime()).isEqualTo(INTRADAY_START);
        assertThat(minutes.getLast().openTime()).isEqualTo(END.minusSeconds(60));
        for (int i = 1; i < minutes.size(); i++) {
            assertThat(Duration.between(minutes.get(i - 1).openTime(), minutes.get(i).openTime())).hasSeconds(60);
        }
    }

    @Test
    void producesDailyCandlesAlignedToIstMidnightIncludingToday() {
        List<Candle> days = generate(path()).dayCandles();

        assertThat(days).hasSize(31); // 30 previous days + today (partial)
        assertThat(days).allSatisfy(d -> assertThat(d.openTime()).isEqualTo(MarketClock.dayStart(d.openTime())));
        assertThat(days.getLast().openTime()).isEqualTo(MarketClock.dayStart(END));
    }

    @Test
    void historyIsDeterministicAndContinuesIntoLiveSimulation() {
        PricePath first = path();
        HistoryGenerator.History a = generate(first);
        HistoryGenerator.History b = generate(path());

        assertThat(a).isEqualTo(b);
        // The live simulation continues from the very last generated price.
        assertThat(first.price()).isEqualByComparingTo(a.minuteCandles().getLast().close());
        assertThat(a.dayCandles().getLast().close()).isEqualByComparingTo(first.price());
    }

    @Test
    void todaysDailyCandleIsConsistentWithItsMinuteCandles() {
        HistoryGenerator.History h = generate(path());
        Candle today = h.dayCandles().getLast();
        List<Candle> todaysMinutes = h.minuteCandles().stream()
                .filter(m -> !m.openTime().isBefore(today.openTime())).toList();

        BigDecimal maxHigh = todaysMinutes.stream().map(Candle::high).reduce(BigDecimal::max).orElseThrow();
        BigDecimal minLow = todaysMinutes.stream().map(Candle::low).reduce(BigDecimal::min).orElseThrow();
        assertThat(today.high()).isGreaterThanOrEqualTo(maxHigh);
        assertThat(today.low()).isLessThanOrEqualTo(minLow);
        assertThat(today.volume()).isGreaterThanOrEqualTo(todaysMinutes.stream().mapToLong(Candle::volume).sum());
    }
}
