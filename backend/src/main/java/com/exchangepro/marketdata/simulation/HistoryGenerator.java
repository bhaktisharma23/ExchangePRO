package com.exchangepro.marketdata.simulation;

import com.exchangepro.marketdata.Candle;
import com.exchangepro.marketdata.MarketClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * Generates simulated price history so charts have data on the very first start.
 *
 * <p>It walks one continuous {@link PricePath} forward in time:
 * <ol>
 *   <li>From {@code historyStart} to {@code intradayStart} in 30-minute steps, producing daily candles.</li>
 *   <li>From {@code intradayStart} to {@code end} in 15-second steps, producing 1-minute candles (and continuing
 *       the daily candles).</li>
 * </ol>
 * Because it is one continuous path, the last generated price is exactly where live simulation continues.
 * Pure function of its inputs: same path seed and times = same history.
 */
public final class HistoryGenerator {

    static final Duration COARSE_STEP = Duration.ofMinutes(30);
    static final Duration FINE_STEP = Duration.ofSeconds(15);
    private static final double SECONDS_PER_DAY = 86_400;

    public record History(List<Candle> minuteCandles, List<Candle> dayCandles) {}

    private HistoryGenerator() {}

    /**
     * @param simulatedYears converts real seconds into simulated years (see SimulationProperties.timeScale)
     * @param avgDailyVolume expected shares traded per day
     * @param end            exclusive end; must be minute-aligned (the current, still-open minute is not generated)
     */
    public static History generate(PricePath path, long avgDailyVolume, Instant historyStart, Instant intradayStart,
                                   Instant end, DoubleUnaryOperator simulatedYears) {
        Map<Instant, Candle> days = new LinkedHashMap<>();
        List<Candle> minutes = new ArrayList<>();

        Instant t = historyStart;
        while (t.isBefore(intradayStart)) {
            Instant next = min(t.plus(COARSE_STEP), intradayStart);
            double seconds = Duration.between(t, next).toSeconds();
            BigDecimal price = path.step(simulatedYears.applyAsDouble(seconds));
            long volume = path.volume(avgDailyVolume * seconds / SECONDS_PER_DAY);
            addToDay(days, t, price, volume);
            t = next;
        }

        for (Instant minute = MarketClock.minuteStart(intradayStart); minute.isBefore(end);
             minute = minute.plusSeconds(60)) {
            Candle candle = null;
            for (Instant s = minute; s.isBefore(minute.plusSeconds(60)); s = s.plus(FINE_STEP)) {
                double seconds = FINE_STEP.toSeconds();
                BigDecimal price = path.step(simulatedYears.applyAsDouble(seconds));
                long volume = path.volume(avgDailyVolume * seconds / SECONDS_PER_DAY);
                candle = candle == null ? Candle.open(minute, price, volume) : candle.withTick(price, volume);
                addToDay(days, s, price, volume);
            }
            minutes.add(candle);
        }
        return new History(minutes, new ArrayList<>(days.values()));
    }

    private static void addToDay(Map<Instant, Candle> days, Instant at, BigDecimal price, long volume) {
        days.compute(MarketClock.dayStart(at),
                (day, candle) -> candle == null ? Candle.open(day, price, volume) : candle.withTick(price, volume));
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
