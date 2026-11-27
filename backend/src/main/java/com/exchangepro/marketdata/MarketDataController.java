package com.exchangepro.marketdata;

import com.exchangepro.marketdata.MarketResponses.CandleSeries;
import com.exchangepro.marketdata.MarketResponses.MarketOverview;
import com.exchangepro.marketdata.MarketResponses.QuoteEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Market data API. All prices are SIMULATED (see the {@code source} field in every response).
 * Live updates are pushed over WebSocket/STOMP on {@code /topic/market/quotes}.
 */
@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market data", description = "Simulated quotes and candles. Not real NSE/BSE prices.")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/overview")
    @Operation(summary = "All quotes plus top gainers, losers, most active and market breadth")
    public MarketOverview overview() {
        return marketDataService.overview();
    }

    @GetMapping("/quotes/{symbol}")
    @Operation(summary = "Latest quote for one instrument")
    public QuoteEnvelope quote(@PathVariable @Size(max = 20) String symbol) {
        return marketDataService.quote(symbol);
    }

    @GetMapping("/candles/{symbol}")
    @Operation(summary = "OHLCV candles, oldest first. timeframe: 1m, 5m, 15m, 1h, 1d")
    public CandleSeries candles(@PathVariable @Size(max = 20) String symbol,
                                @RequestParam(defaultValue = "1m") String timeframe,
                                @RequestParam(defaultValue = "300") int limit) {
        return marketDataService.candles(symbol, Timeframe.fromCode(timeframe), limit);
    }
}
