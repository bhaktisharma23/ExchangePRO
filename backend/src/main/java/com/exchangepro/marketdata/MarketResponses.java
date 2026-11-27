package com.exchangepro.marketdata;

import java.time.Instant;
import java.util.List;

/** Response envelopes. Each carries the data {@link MarketDataSource} so clients can label prices. */
public final class MarketResponses {

    private MarketResponses() {}

    public record QuoteEnvelope(MarketDataSource source, QuoteResponse quote) {}

    public record CandleSeries(String symbol, String timeframe, MarketDataSource source, List<CandleResponse> candles) {}

    /**
     * @param advances  instruments trading above their previous close
     * @param declines  instruments below their previous close
     * @param unchanged instruments at their previous close
     */
    public record MarketOverview(MarketDataSource source, Instant asOf, int advances, int declines, int unchanged,
                                 List<QuoteResponse> topGainers, List<QuoteResponse> topLosers,
                                 List<QuoteResponse> mostActive, List<QuoteResponse> quotes) {}

    /**
     * WebSocket payload on {@code /topic/market/quotes}. {@code version} lets clients detect format changes.
     */
    public record QuotesMessage(String type, int version, MarketDataSource source, Instant asOf,
                                List<QuoteResponse> quotes) {
        public static final String TYPE = "QUOTES";
        public static final int VERSION = 1;
    }
}
