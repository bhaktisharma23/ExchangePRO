package com.exchangepro.marketdata;

import com.exchangepro.marketdata.MarketResponses.QuotesMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes every quote batch to WebSocket subscribers of {@value #QUOTES_TOPIC}. One message per tick containing
 * all instruments keeps the number of frames per client constant regardless of how many instruments exist.
 */
@Component
public class MarketDataBroadcaster implements MarketDataListener {

    public static final String QUOTES_TOPIC = "/topic/market/quotes";

    private final SimpMessagingTemplate messagingTemplate;

    public MarketDataBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onQuotes(QuotesMessage message) {
        messagingTemplate.convertAndSend(QUOTES_TOPIC, message);
    }
}
