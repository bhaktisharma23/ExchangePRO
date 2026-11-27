package com.exchangepro.marketdata.simulation;

import com.exchangepro.config.ExchangeProProperties;
import com.exchangepro.instrument.InstrumentDefinition;
import com.exchangepro.instrument.InstrumentService;
import com.exchangepro.marketdata.Candle;
import com.exchangepro.marketdata.CandleRepository;
import com.exchangepro.marketdata.LiveMarketState;
import com.exchangepro.marketdata.MarketClock;
import com.exchangepro.marketdata.MarketDataListener;
import com.exchangepro.marketdata.MarketDataMode;
import com.exchangepro.marketdata.MarketDataProvider;
import com.exchangepro.marketdata.MarketDataSource;
import com.exchangepro.marketdata.MarketResponses.QuotesMessage;
import com.exchangepro.marketdata.QuoteResponse;
import com.exchangepro.marketdata.SimulationProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Synthetic market-data provider ("SIMULATION mode").
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>On startup (after the database is migrated) every active instrument without candles gets generated
 *       history (daily + 1-minute), so charts work immediately.</li>
 *   <li>The simulation resumes from the last stored price.</li>
 *   <li>Every {@code tickInterval} the price of every instrument moves one step; quotes are published to
 *       {@link LiveMarketState} and to listeners (WebSocket); closed 1-minute candles and the forming daily
 *       candle are written to the database.</li>
 * </ol>
 *
 * <h2>Single writer</h2>
 * All mutable simulation state is touched only inside {@link #tick()}, which runs on ONE dedicated thread
 * ({@code market-sim}) and is additionally {@code synchronized} so tests may call it directly. No locks or
 * concurrent collections are needed for the simulation state itself.
 */
@Component
public class SimulatedMarketDataProvider implements MarketDataProvider, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(SimulatedMarketDataProvider.class);
    private static final double SECONDS_PER_DAY = 86_400;

    private final InstrumentService instrumentService;
    private final CandleRepository candleRepository;
    private final LiveMarketState state;
    private final List<MarketDataListener> listeners;
    private final SimulationProperties properties;
    private final ExchangeProProperties appProperties;
    private final Clock clock;

    /** Simulation state; accessed only under this object's monitor (in practice: the market-sim thread). */
    private final Map<String, SimulatedInstrument> instruments = new LinkedHashMap<>();
    private Instant lastTick;
    private ScheduledExecutorService executor;
    private volatile boolean running;

    public SimulatedMarketDataProvider(InstrumentService instrumentService, CandleRepository candleRepository,
                                       LiveMarketState state, List<MarketDataListener> listeners,
                                       SimulationProperties properties, ExchangeProProperties appProperties,
                                       Clock clock) {
        this.instrumentService = instrumentService;
        this.candleRepository = candleRepository;
        this.state = state;
        this.listeners = listeners;
        this.properties = properties;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    public MarketDataSource source() {
        return MarketDataSource.SIMULATED;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public synchronized void start() {
        if (appProperties.marketData().mode() != MarketDataMode.SIMULATION) {
            return;
        }
        initialize();
        running = true;
        if (properties.enabled()) {
            long periodMs = properties.tickInterval().toMillis();
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "market-sim");
                t.setDaemon(true);
                return t;
            });
            executor.scheduleAtFixedRate(this::safeTick, periodMs, periodMs, TimeUnit.MILLISECONDS);
            log.info("Market simulation running: {} instruments, tick every {} ms, time scale {}x, seed {}",
                    instruments.size(), periodMs, properties.timeScale(), properties.seed());
        } else {
            log.info("Market simulation initialised with automatic ticking DISABLED");
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // ------------------------------------------------------------------ simulation

    /** Advances every instrument by one step. Public so tests can drive the simulation deterministically. */
    public synchronized void tick() {
        Instant now = Instant.now(clock);
        double realSeconds = lastTick == null
                ? properties.tickInterval().toMillis() / 1000.0
                : Math.max(0, Duration.between(lastTick, now).toMillis() / 1000.0);
        lastTick = now;
        double dtYears = properties.simulatedYears(realSeconds);

        List<QuoteResponse> updated = new ArrayList<>(instruments.size());
        for (SimulatedInstrument sim : instruments.values()) {
            InstrumentDefinition def = sim.definition();
            double expectedVolume = def.avgDailyVolume() * realSeconds / SECONDS_PER_DAY;
            SimulatedInstrument.TickResult result = sim.tick(now, dtYears, expectedVolume);
            state.publish(def.symbol(), result.snapshot());
            updated.add(QuoteResponse.from(result.snapshot().quote()));
            persist(def, result);
        }
        state.markUpdated(now);
        notifyListeners(new QuotesMessage(QuotesMessage.TYPE, QuotesMessage.VERSION, source(), now, updated));
    }

    private void safeTick() {
        try {
            tick();
        } catch (RuntimeException ex) {
            // Never let an exception escape: it would silently cancel the scheduled task.
            log.error("Market simulation tick failed", ex);
        }
    }

    private void persist(InstrumentDefinition def, SimulatedInstrument.TickResult result) {
        if (result.closedMinute() != null) {
            candleRepository.insertAll(def.id(), CandleRepository.MINUTE, List.of(result.closedMinute()));
            // Once a minute, save the forming daily candle so a restart can resume today's statistics.
            candleRepository.upsert(def.id(), CandleRepository.DAY, result.snapshot().formingDay());
        }
        if (result.closedDay() != null) {
            candleRepository.upsert(def.id(), CandleRepository.DAY, result.closedDay());
        }
    }

    private void notifyListeners(QuotesMessage message) {
        for (MarketDataListener listener : listeners) {
            try {
                listener.onQuotes(message);
            } catch (RuntimeException ex) {
                log.warn("Market data listener {} failed: {}", listener.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ initialisation

    private void initialize() {
        Instant now = Instant.now(clock);
        instruments.clear();
        for (InstrumentDefinition def : instrumentService.activeDefinitions()) {
            PricePath path;
            if (!candleRepository.hasAny(def.id())) {
                path = newPath(def, def.referencePrice());
                backfill(def, path, now);
            } else {
                BigDecimal last = candleRepository.latest(def.id(), CandleRepository.MINUTE)
                        .or(() -> candleRepository.latest(def.id(), CandleRepository.DAY))
                        .map(Candle::close).orElse(def.referencePrice());
                path = newPath(def, last);
            }
            SimulatedInstrument sim = restore(def, path, now);
            instruments.put(def.symbol(), sim);
            state.publish(def.symbol(), sim.snapshot());
        }
        state.markUpdated(now);
        lastTick = now;
    }

    private PricePath newPath(InstrumentDefinition def, BigDecimal startPrice) {
        return new PricePath(PricePath.seedFor(properties.seed(), def.symbol()), def.referencePrice(), startPrice,
                def.annualVolatility(), def.tickSize(), properties.meanReversionPerYear());
    }

    private void backfill(InstrumentDefinition def, PricePath path, Instant now) {
        Instant end = MarketClock.minuteStart(now);
        Instant intradayStart = end.minus(Duration.ofMinutes(properties.backfillMinutes()));
        Instant historyStart = MarketClock.dayStart(intradayStart).minus(Duration.ofDays(properties.backfillDays()));
        HistoryGenerator.History history = HistoryGenerator.generate(path, def.avgDailyVolume(), historyStart,
                intradayStart, end, properties::simulatedYears);
        candleRepository.insertAll(def.id(), CandleRepository.DAY, history.dayCandles());
        candleRepository.insertAll(def.id(), CandleRepository.MINUTE, history.minuteCandles());
        log.debug("Backfilled {}: {} daily and {} 1-minute simulated candles", def.symbol(),
                history.dayCandles().size(), history.minuteCandles().size());
    }

    /** Rebuilds today's statistics (open/high/low/volume, previous close) from the stored candles. */
    private SimulatedInstrument restore(InstrumentDefinition def, PricePath path, Instant now) {
        Instant today = MarketClock.dayStart(now);
        Optional<Candle> todayCandle = candleRepository.latest(def.id(), CandleRepository.DAY)
                .filter(c -> c.openTime().equals(today));
        BigDecimal previousClose = candleRepository.latestBefore(def.id(), CandleRepository.DAY, today)
                .map(Candle::close)
                .orElse(todayCandle.map(Candle::open).orElse(path.price()));
        return new SimulatedInstrument(def, path, properties.spreadBasisPoints(), todayCandle.orElse(null),
                previousClose, now);
    }
}
