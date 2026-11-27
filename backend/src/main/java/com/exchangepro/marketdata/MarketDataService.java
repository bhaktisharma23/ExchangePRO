package com.exchangepro.marketdata;

import com.exchangepro.common.error.ErrorCode;
import com.exchangepro.common.error.ExchangeProException;
import com.exchangepro.instrument.InstrumentDefinition;
import com.exchangepro.instrument.InstrumentService;
import com.exchangepro.marketdata.LiveMarketState.InstrumentSnapshot;
import com.exchangepro.marketdata.MarketResponses.CandleSeries;
import com.exchangepro.marketdata.MarketResponses.MarketOverview;
import com.exchangepro.marketdata.MarketResponses.QuoteEnvelope;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Read side of market data: quotes, overview and candles. */
@Service
public class MarketDataService {

    static final int TOP_N = 5;
    public static final int MAX_CANDLES = 1000;

    private final LiveMarketState state;
    private final CandleRepository candleRepository;
    private final InstrumentService instrumentService;
    private final MarketDataProvider provider;

    public MarketDataService(LiveMarketState state, CandleRepository candleRepository,
                             InstrumentService instrumentService, MarketDataProvider provider) {
        this.state = state;
        this.candleRepository = candleRepository;
        this.instrumentService = instrumentService;
        this.provider = provider;
    }

    public MarketDataSource source() {
        return provider.source();
    }

    public MarketOverview overview() {
        List<Quote> quotes = state.allQuotes();
        if (quotes.isEmpty()) {
            throw unavailable();
        }
        Comparator<Quote> byChange = Comparator.comparing(Quote::changePercent);
        int advances = (int) quotes.stream().filter(q -> q.change().signum() > 0).count();
        int declines = (int) quotes.stream().filter(q -> q.change().signum() < 0).count();
        return new MarketOverview(source(), state.asOf(), advances, declines, quotes.size() - advances - declines,
                top(quotes, byChange.reversed()), top(quotes, byChange),
                top(quotes, Comparator.comparingLong(Quote::volume).reversed()),
                quotes.stream().map(QuoteResponse::from).toList());
    }

    public QuoteEnvelope quote(String symbol) {
        InstrumentDefinition def = instrumentService.getDefinition(symbol); // 404 for unknown symbols
        InstrumentSnapshot snapshot = state.get(def.symbol()).orElseThrow(this::unavailable);
        return new QuoteEnvelope(source(), QuoteResponse.from(snapshot.quote()));
    }

    /**
     * Returns the latest {@code limit} candles, oldest first. Stored history is combined with the candle that is
     * still forming in memory, so the chart's last bar is always current.
     */
    public CandleSeries candles(String symbol, Timeframe timeframe, int limit) {
        if (limit < 1 || limit > MAX_CANDLES) {
            throw new ExchangeProException(ErrorCode.VALIDATION_FAILED, "limit must be between 1 and " + MAX_CANDLES);
        }
        InstrumentDefinition def = instrumentService.getDefinition(symbol);
        InstrumentSnapshot snapshot = state.get(def.symbol()).orElse(null);
        Candle formingMinute = snapshot != null ? snapshot.formingMinute() : null;
        Candle formingDay = snapshot != null ? snapshot.formingDay() : null;
        Instant now = state.asOf() != null ? state.asOf() : Instant.now();

        List<Candle> candles = switch (timeframe.storage()) {
            case MINUTE -> withForming(
                    candleRepository.recent(def.id(), CandleRepository.MINUTE, cutoff(formingMinute, now), limit),
                    formingMinute);
            case DAY -> withForming(
                    candleRepository.recent(def.id(), CandleRepository.DAY,
                            formingDay != null ? formingDay.openTime() : MarketClock.dayStart(now).plusSeconds(1),
                            limit),
                    formingDay);
            case AGGREGATED -> {
                Instant from = MarketClock.bucketStart(now, timeframe.seconds())
                        .minusSeconds(timeframe.seconds() * (long) limit);
                yield mergeIntoBuckets(candleRepository.aggregated(def.id(), timeframe.seconds(), from, limit),
                        formingMinute, timeframe.seconds());
            }
        };
        if (candles.size() > limit) {
            candles = candles.subList(candles.size() - limit, candles.size());
        }
        return new CandleSeries(def.symbol(), timeframe.code(), source(),
                candles.stream().map(CandleResponse::from).toList());
    }

    private static Instant cutoff(Candle forming, Instant now) {
        return forming != null ? forming.openTime() : MarketClock.minuteStart(now).plusSeconds(60);
    }

    private static List<Candle> withForming(List<Candle> stored, Candle forming) {
        if (forming == null) {
            return stored;
        }
        List<Candle> result = new ArrayList<>(stored);
        result.add(forming);
        return result;
    }

    /**
     * Adds the forming 1-minute candle to aggregated buckets: merged into the last bucket if it belongs to it,
     * otherwise appended as a new bucket.
     */
    static List<Candle> mergeIntoBuckets(List<Candle> buckets, Candle formingMinute, long bucketSeconds) {
        if (formingMinute == null) {
            return buckets;
        }
        Instant bucket = MarketClock.bucketStart(formingMinute.openTime(), bucketSeconds);
        List<Candle> result = new ArrayList<>(buckets);
        if (!result.isEmpty() && result.getLast().openTime().equals(bucket)) {
            result.set(result.size() - 1, result.getLast().mergeLater(formingMinute));
        } else {
            result.add(formingMinute.withOpenTime(bucket));
        }
        return result;
    }

    private static List<QuoteResponse> top(List<Quote> quotes, Comparator<Quote> order) {
        return quotes.stream().sorted(order).limit(TOP_N).map(QuoteResponse::from).toList();
    }

    private ExchangeProException unavailable() {
        return new ExchangeProException(ErrorCode.MARKET_DATA_UNAVAILABLE, "Market data is not available yet");
    }
}
