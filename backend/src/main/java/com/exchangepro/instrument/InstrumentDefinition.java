package com.exchangepro.instrument;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything the market-data module needs to simulate an instrument, including the simulator parameters.
 * Internal: never serialized to clients.
 */
public record InstrumentDefinition(UUID id, String symbol, String name, BigDecimal tickSize,
                                   BigDecimal referencePrice, BigDecimal annualVolatility, long avgDailyVolume) {

    static InstrumentDefinition from(Instrument i) {
        return new InstrumentDefinition(i.getId(), i.getSymbol(), i.getName(), i.getTickSize(),
                i.getReferencePrice(), i.getAnnualVolatility(), i.getAvgDailyVolume());
    }
}
