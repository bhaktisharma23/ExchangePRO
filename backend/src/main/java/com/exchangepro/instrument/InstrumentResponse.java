package com.exchangepro.instrument;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

/** Public instrument master data. Deliberately excludes the simulator parameters (reference price etc.). */
public record InstrumentResponse(
        String symbol,
        String name,
        String exchange,
        String sector,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal tickSize,
        int lotSize) {

    static InstrumentResponse from(Instrument i) {
        return new InstrumentResponse(i.getSymbol(), i.getName(), i.getExchange(), i.getSector(), i.getCurrency(),
                i.getTickSize(), i.getLotSize());
    }
}
