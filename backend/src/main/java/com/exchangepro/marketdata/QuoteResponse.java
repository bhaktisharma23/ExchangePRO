package com.exchangepro.marketdata;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/** API/WebSocket shape of a quote. Decimal values are JSON strings to preserve exactness in JavaScript. */
public record QuoteResponse(
        String symbol,
        String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal lastPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal change,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal changePercent,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal open,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal high,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal low,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal previousClose,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal bid,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal ask,
        long bidSize,
        long askSize,
        long volume,
        Instant timestamp) {

    public static QuoteResponse from(Quote q) {
        return new QuoteResponse(q.symbol(), q.name(), q.lastPrice(), q.change(), q.changePercent(), q.open(),
                q.high(), q.low(), q.previousClose(), q.bid(), q.ask(), q.bidSize(), q.askSize(), q.volume(),
                q.timestamp());
    }
}
