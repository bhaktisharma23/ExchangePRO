package com.exchangepro.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exchangepro.marketdata.MarketDataMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketDataModeGuardTest {

    private static ExchangeProProperties props(MarketDataMode mode) {
        return new ExchangeProProperties(
                new ExchangeProProperties.Cors(List.of("http://localhost:5173")),
                new ExchangeProProperties.MarketData(mode));
    }

    @Test
    void simulationModeStarts() {
        assertThatCode(() -> new MarketDataModeGuard(props(MarketDataMode.SIMULATION)).verifyMode())
                .doesNotThrowAnyException();
    }

    @Test
    void realModeIsRefusedUntilALicensedProviderExists() {
        assertThatThrownBy(() -> new MarketDataModeGuard(props(MarketDataMode.REAL)).verifyMode())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MARKET_DATA_MODE=REAL is not available");
    }
}
