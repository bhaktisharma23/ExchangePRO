package com.exchangepro.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/** Immutable OHLCV candle. All prices are exact decimals on the instrument's tick grid. */
public record Candle(Instant openTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                     long volume) {

    public Candle {
        if (high.compareTo(low) < 0 || high.compareTo(open) < 0 || high.compareTo(close) < 0
                || low.compareTo(open) > 0 || low.compareTo(close) > 0) {
            throw new IllegalArgumentException("Invalid OHLC: " + open + "/" + high + "/" + low + "/" + close);
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
    }

    public static Candle open(Instant openTime, BigDecimal price, long volume) {
        return new Candle(openTime, price, price, price, price, volume);
    }

    /** This candle updated with a new trade/tick. */
    public Candle withTick(BigDecimal price, long tickVolume) {
        return new Candle(openTime, open, high.max(price), low.min(price), price, volume + tickVolume);
    }

    /** Combines this candle with a LATER candle (used to aggregate 1m candles into bigger buckets). */
    public Candle mergeLater(Candle later) {
        return new Candle(openTime, open, high.max(later.high), low.min(later.low), later.close,
                volume + later.volume);
    }

    public Candle withOpenTime(Instant newOpenTime) {
        return new Candle(newOpenTime, open, high, low, close, volume);
    }
}
