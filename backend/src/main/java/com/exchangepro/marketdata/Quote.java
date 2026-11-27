package com.exchangepro.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Latest market snapshot for one instrument (internal model; see {@link QuoteResponse} for the API shape).
 *
 * @param change        lastPrice - previousClose
 * @param changePercent change / previousClose * 100, 2 decimals
 * @param volume        shares traded so far today (IST day)
 * @param timestamp     when this quote was produced
 */
public record Quote(String symbol, String name, BigDecimal lastPrice, BigDecimal change, BigDecimal changePercent,
                    BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal previousClose,
                    BigDecimal bid, BigDecimal ask, long bidSize, long askSize, long volume, Instant timestamp) {}
