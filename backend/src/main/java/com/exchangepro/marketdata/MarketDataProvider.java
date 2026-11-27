package com.exchangepro.marketdata;

/**
 * A source of market data. It publishes quotes into {@link LiveMarketState} and candles into the
 * {@link CandleRepository}, and notifies {@link MarketDataListener}s on every update.
 *
 * <p>Today the only implementation is the simulator. A real-data implementation can be added later, but only
 * for a provider whose licence explicitly permits free, multi-user display (see README, "Market data policy").
 */
public interface MarketDataProvider {

    MarketDataSource source();

    boolean isRunning();
}
