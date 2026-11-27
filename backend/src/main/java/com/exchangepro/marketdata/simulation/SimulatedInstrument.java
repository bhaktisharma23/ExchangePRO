package com.exchangepro.marketdata.simulation;

import com.exchangepro.common.money.Money;
import com.exchangepro.instrument.InstrumentDefinition;
import com.exchangepro.marketdata.Candle;
import com.exchangepro.marketdata.LiveMarketState.InstrumentSnapshot;
import com.exchangepro.marketdata.MarketClock;
import com.exchangepro.marketdata.Quote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Mutable simulation state of one instrument: its price path, the forming 1-minute and daily candles, and the
 * previous day's close. Confined to the simulator thread; it only ever hands out immutable snapshots.
 */
final class SimulatedInstrument {

    /** Result of one tick. {@code closedMinute}/{@code closedDay} are non-null only when a period just ended. */
    record TickResult(InstrumentSnapshot snapshot, Candle closedMinute, Candle closedDay) {}

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);

    private final InstrumentDefinition definition;
    private final PricePath path;
    private final int spreadBasisPoints;
    private final long maxQuoteSize;
    private Candle minute;
    private Candle day;
    private BigDecimal previousClose;
    private InstrumentSnapshot snapshot;

    SimulatedInstrument(InstrumentDefinition definition, PricePath path, int spreadBasisPoints,
                        Candle formingDay, BigDecimal previousClose, Instant now) {
        this.definition = definition;
        this.path = path;
        this.spreadBasisPoints = spreadBasisPoints;
        this.maxQuoteSize = Math.max(1, definition.avgDailyVolume() / 5_000);
        this.day = formingDay;
        this.previousClose = previousClose;
        this.snapshot = new InstrumentSnapshot(quote(now), null, day);
    }

    InstrumentDefinition definition() {
        return definition;
    }

    InstrumentSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Advances the price by {@code dtYears} of simulated time at wall-clock time {@code now} and updates the
     * forming candles. When {@code now} falls into a new minute/day, the previous candle is returned as closed.
     */
    TickResult tick(Instant now, double dtYears, double expectedVolume) {
        BigDecimal price = path.step(dtYears);
        long volume = path.volume(expectedVolume);
        Candle closedMinute = null;
        Candle closedDay = null;

        Instant minuteStart = MarketClock.minuteStart(now);
        if (minute != null && !minute.openTime().equals(minuteStart)) {
            closedMinute = minute;
            minute = null;
        }
        minute = minute == null ? Candle.open(minuteStart, price, volume) : minute.withTick(price, volume);

        Instant dayStart = MarketClock.dayStart(now);
        if (day != null && !day.openTime().equals(dayStart)) {
            closedDay = day;
            previousClose = day.close();
            day = null;
        }
        day = day == null ? Candle.open(dayStart, price, volume) : day.withTick(price, volume);

        snapshot = new InstrumentSnapshot(quote(now), minute, day);
        return new TickResult(snapshot, closedMinute, closedDay);
    }

    private Quote quote(Instant now) {
        BigDecimal last = path.price();
        BigDecimal tick = definition.tickSize();
        BigDecimal change = last.subtract(previousClose);
        BigDecimal changePercent = change.multiply(HUNDRED).divide(previousClose, 2, RoundingMode.HALF_EVEN);

        // Spread in whole ticks (at least one). The bid is at or below the last price, the ask above it.
        long spreadTicks = Math.max(1, last.multiply(BigDecimal.valueOf(spreadBasisPoints))
                .divide(TEN_THOUSAND.multiply(tick), 0, RoundingMode.HALF_EVEN).longValue());
        long bidTicksBelow = spreadTicks / 2;
        BigDecimal bid = last.subtract(tick.multiply(BigDecimal.valueOf(bidTicksBelow))).max(tick);
        BigDecimal ask = bid.add(tick.multiply(BigDecimal.valueOf(spreadTicks)));

        Candle d = day != null ? day : Candle.open(MarketClock.dayStart(now), last, 0);
        return new Quote(definition.symbol(), definition.name(), last, change, changePercent,
                d.open(), d.high(), d.low(), previousClose,
                Money.normalize(bid), Money.normalize(ask), path.size(maxQuoteSize), path.size(maxQuoteSize),
                d.volume(), now);
    }
}
