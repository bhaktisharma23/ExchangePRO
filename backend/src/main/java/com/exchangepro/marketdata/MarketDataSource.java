package com.exchangepro.marketdata;

/**
 * Describes where displayed prices come from. Attached to every market-data response so the UI can always
 * show the source and whether prices are real-time.
 *
 * @param mode     SIMULATION or REAL
 * @param provider human-readable provider name
 * @param label    text the UI must show next to prices, e.g. "SIMULATED DATA"
 * @param realTime true only for genuine real-time exchange data (never true for simulated data)
 */
public record MarketDataSource(MarketDataMode mode, String provider, String label, boolean realTime) {

    public static final MarketDataSource SIMULATED =
            new MarketDataSource(MarketDataMode.SIMULATION, "EXCHANGE PRO simulator",
                    MarketDataMode.SIMULATION.displayLabel(), false);
}
