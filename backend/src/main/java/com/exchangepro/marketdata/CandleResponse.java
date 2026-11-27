package com.exchangepro.marketdata;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record CandleResponse(
        Instant openTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal open,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal high,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal low,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal close,
        long volume) {

    static CandleResponse from(Candle c) {
        return new CandleResponse(c.openTime(), c.open(), c.high(), c.low(), c.close(), c.volume());
    }
}
