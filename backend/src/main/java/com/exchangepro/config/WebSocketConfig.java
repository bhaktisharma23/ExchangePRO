package com.exchangepro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket endpoint ({@code /ws}) speaking STOMP, a small text protocol with SUBSCRIBE/SEND/MESSAGE frames.
 *
 * <p>Phase 3 exposes only PUBLIC, read-only market data. Security rules:
 * <ul>
 *   <li>Browsers may connect only from the configured origins (same list as CORS).</li>
 *   <li>Clients may SUBSCRIBE only to {@code /topic/market/**}.</li>
 *   <li>Clients may not SEND anything. With a simple broker, a client SEND to {@code /topic/...} would otherwise
 *       be relayed to every subscriber, letting anyone broadcast fake prices.</li>
 * </ul>
 * Authenticated, per-user channels (order updates, notifications) are added in Phase 8.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String PUBLIC_MARKET_TOPICS = "/topic/market/";

    private final ExchangeProProperties properties;

    public WebSocketConfig(ExchangeProProperties properties) {
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }
                StompCommand command = accessor.getCommand();
                if (command == StompCommand.SEND) {
                    throw new MessageDeliveryException("Clients may not send messages");
                }
                if (command == StompCommand.SUBSCRIBE) {
                    String destination = accessor.getDestination();
                    if (destination == null || !destination.startsWith(PUBLIC_MARKET_TOPICS)) {
                        throw new MessageDeliveryException("Subscription not allowed: " + destination);
                    }
                }
                return message;
            }
        });
    }
}
