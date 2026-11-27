package com.exchangepro.marketdata;

import com.exchangepro.marketdata.MarketResponses.QuotesMessage;

/** Receives every batch of updated quotes (e.g. the WebSocket broadcaster). Called on the provider's thread. */
public interface MarketDataListener {

    void onQuotes(QuotesMessage message);
}
