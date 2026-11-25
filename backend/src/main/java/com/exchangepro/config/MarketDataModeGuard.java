package com.exchangepro.config;

import com.exchangepro.marketdata.MarketDataMode;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fails fast if REAL market-data mode is requested.
 *
 * <p>No compliant, free, redistributable Indian market-data provider is integrated, so starting in REAL mode
 * would mean displaying data we do not have. Refusing to start is safer than silently falling back.
 */
@Component
class MarketDataModeGuard {

    private final ExchangeProProperties properties;

    MarketDataModeGuard(ExchangeProProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void verifyMode() {
        if (properties.marketData().mode() == MarketDataMode.REAL) {
            throw new IllegalStateException(
                    "MARKET_DATA_MODE=REAL is not available: no licensed real market-data provider is configured. "
                            + "Use MARKET_DATA_MODE=SIMULATION.");
        }
    }
}
