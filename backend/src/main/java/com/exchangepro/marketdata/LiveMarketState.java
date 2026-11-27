package com.exchangepro.marketdata;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Latest quote and currently-forming candles for every instrument, readable from any thread.
 *
 * <p><b>Concurrency model:</b> exactly one writer (the market-data provider's thread) replaces a whole
 * {@link InstrumentSnapshot} per instrument; HTTP threads only read. Snapshots are immutable records, so a reader
 * always sees a quote and candles that belong together, never a half-updated mix, and no locks are needed.
 */
@Component
public class LiveMarketState {

    /** Everything about one instrument at one moment. */
    public record InstrumentSnapshot(Quote quote, Candle formingMinute, Candle formingDay) {}

    private final ConcurrentMap<String, InstrumentSnapshot> snapshots = new ConcurrentHashMap<>();
    private volatile Instant asOf;

    /** Called only by the market-data provider thread (the single writer). */
    public void publish(String symbol, InstrumentSnapshot snapshot) {
        snapshots.put(symbol, snapshot);
    }

    public void markUpdated(Instant time) {
        this.asOf = time;
    }

    public Optional<InstrumentSnapshot> get(String symbol) {
        return Optional.ofNullable(snapshots.get(symbol));
    }

    public List<Quote> allQuotes() {
        return snapshots.values().stream().map(InstrumentSnapshot::quote)
                .sorted(Comparator.comparing(Quote::symbol)).toList();
    }

    /** Time of the last update, or null before the provider has started. */
    public Instant asOf() {
        return asOf;
    }
}
