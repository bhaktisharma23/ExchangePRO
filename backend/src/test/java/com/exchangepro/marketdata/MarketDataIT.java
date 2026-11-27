package com.exchangepro.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exchangepro.marketdata.simulation.SimulatedMarketDataProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Market data end to end: seeded instruments, generated history, REST API, and WebSocket broadcasting,
 * against a real PostgreSQL 16. The simulator's automatic ticking is disabled in tests; each test advances the
 * market explicitly with {@code provider.tick()}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@ActiveProfiles("test")
@SuppressWarnings({"unchecked", "rawtypes"})
class MarketDataIT {

    private static final int SEEDED_INSTRUMENTS = 43;
    private static final BigDecimal TICK = new BigDecimal("0.05");

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SimulatedMarketDataProvider provider;

    @LocalServerPort
    private int port;

    private String token;

    @BeforeEach
    void signIn() {
        ResponseEntity<Map> registered = http.postForEntity("/api/v1/auth/register", Map.of(
                "email", "market-" + UUID.randomUUID() + "@example.com",
                "password", "Password123", "displayName", "Market Tester"), Map.class);
        token = (String) registered.getBody().get("accessToken");
    }

    private ResponseEntity<Map> get(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private static BigDecimal dec(Object value) {
        return new BigDecimal((String) value);
    }

    // ------------------------------------------------------------------ instruments & history

    @Test
    void instrumentsAreSeededAndHistoryIsBackfilled() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM instruments WHERE active", Integer.class))
                .isEqualTo(SEEDED_INSTRUMENTS);
        Integer minuteCandles = jdbc.queryForObject("""
                SELECT count(*) FROM candles c JOIN instruments i ON i.id = c.instrument_id
                 WHERE i.symbol = 'RELIANCE' AND c.timeframe = '1m'""", Integer.class);
        Integer dayCandles = jdbc.queryForObject("""
                SELECT count(*) FROM candles c JOIN instruments i ON i.id = c.instrument_id
                 WHERE i.symbol = 'RELIANCE' AND c.timeframe = '1d'""", Integer.class);
        assertThat(minuteCandles).isGreaterThanOrEqualTo(180);   // test profile: 180 minutes of history
        assertThat(dayCandles).isGreaterThanOrEqualTo(11);       // 10 days + today
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT source) FROM candles", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT min(source) FROM candles", String.class)).isEqualTo("SIMULATED");
    }

    @Test
    void searchMatchesSymbolAndNameCaseInsensitivelyWithSymbolMatchesFirst() {
        List<Map<String, Object>> reli = (List) get("/api/v1/instruments?query=reli").getBody().get("content");
        assertThat(reli).extracting(i -> i.get("symbol")).containsExactly("RELIANCE");

        List<Map<String, Object>> banks = (List) get("/api/v1/instruments?query=bank&size=50").getBody().get("content");
        assertThat(banks).extracting(i -> i.get("symbol")).contains("HDFCBANK", "ICICIBANK", "SBIN");

        List<Map<String, Object>> health = (List) get("/api/v1/instruments?sector=Healthcare&size=50").getBody()
                .get("content");
        assertThat(health).isNotEmpty().allSatisfy(i -> assertThat(i).containsEntry("sector", "Healthcare"));

        // LIKE wildcards are matched literally, not as "match everything".
        assertThat((List) get("/api/v1/instruments?query=%25").getBody().get("content")).isEmpty();
        assertThat((List) get("/api/v1/instruments?query=_").getBody().get("content")).isEmpty();
    }

    @Test
    void instrumentDetailsHideSimulatorParameters() {
        ResponseEntity<Map> response = get("/api/v1/instruments/reliance");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("symbol", "RELIANCE").containsEntry("exchange", "NSE")
                .containsEntry("tickSize", "0.0500").containsEntry("lotSize", 1)
                .doesNotContainKeys("referencePrice", "annualVolatility", "avgDailyVolume");
        // Symbols may contain '&'; the URI template encodes it correctly.
        assertThat(http.exchange("/api/v1/instruments/{symbol}", HttpMethod.GET, new HttpEntity<>(bearer()),
                Map.class, "M&M").getBody()).containsEntry("symbol", "M&M");
        assertThat(get("/api/v1/instruments/NOPE").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((List<String>) (List) http.exchange("/api/v1/instruments/sectors", HttpMethod.GET,
                new HttpEntity<>(bearer()), List.class).getBody()).contains("Information Technology", "FMCG");
    }

    private HttpHeaders bearer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void marketDataRequiresAuthentication() {
        assertThat(http.getForEntity("/api/v1/market/overview", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.getForEntity("/api/v1/instruments", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ quotes & overview

    @Test
    void overviewIsLabelledSimulatedAndInternallyConsistent() {
        provider.tick();
        Map<String, Object> overview = get("/api/v1/market/overview").getBody();

        assertThat((Map<String, Object>) overview.get("source"))
                .containsEntry("mode", "SIMULATION").containsEntry("label", "SIMULATED DATA")
                .containsEntry("realTime", false);
        assertThat(overview.get("asOf")).isNotNull();
        List<Map<String, Object>> quotes = (List) overview.get("quotes");
        assertThat(quotes).hasSize(SEEDED_INSTRUMENTS);
        int breadth = (int) overview.get("advances") + (int) overview.get("declines") + (int) overview.get("unchanged");
        assertThat(breadth).isEqualTo(SEEDED_INSTRUMENTS);
        List<Map<String, Object>> gainers = (List) overview.get("topGainers");
        List<Map<String, Object>> losers = (List) overview.get("topLosers");
        assertThat(gainers).hasSize(5);
        assertThat(losers).hasSize(5);
        assertThat(dec(gainers.getFirst().get("changePercent")))
                .isGreaterThanOrEqualTo(dec(gainers.getLast().get("changePercent")));
        assertThat(dec(losers.getFirst().get("changePercent")))
                .isLessThanOrEqualTo(dec(losers.getLast().get("changePercent")));
    }

    @Test
    void quotesAreOnTheTickGridWithBidBelowAsk() {
        provider.tick();
        Map<String, Object> envelope = get("/api/v1/market/quotes/TCS").getBody();
        Map<String, Object> quote = (Map<String, Object>) envelope.get("quote");

        assertThat((Map<String, Object>) envelope.get("source")).containsEntry("label", "SIMULATED DATA");
        BigDecimal last = dec(quote.get("lastPrice"));
        assertThat(last.remainder(TICK).signum()).isZero();
        assertThat(dec(quote.get("bid"))).isLessThanOrEqualTo(last);
        assertThat(dec(quote.get("ask"))).isGreaterThan(last);
        assertThat(dec(quote.get("high"))).isGreaterThanOrEqualTo(dec(quote.get("low")));
        assertThat(dec(quote.get("change")))
                .isEqualByComparingTo(last.subtract(dec(quote.get("previousClose"))));
    }

    @Test
    void tickingMovesTheMarketForward() throws Exception {
        provider.tick();
        Instant before = Instant.parse((String) ((Map) get("/api/v1/market/quotes/INFY").getBody().get("quote"))
                .get("timestamp"));
        Thread.sleep(20);
        provider.tick();
        Instant after = Instant.parse((String) ((Map) get("/api/v1/market/quotes/INFY").getBody().get("quote"))
                .get("timestamp"));

        assertThat(after).isAfter(before);
    }

    // ------------------------------------------------------------------ candles

    @Test
    void minuteCandlesEndWithTheFormingCandleAtTheCurrentPrice() {
        provider.tick();
        Map<String, Object> series = get("/api/v1/market/candles/SBIN?timeframe=1m&limit=50").getBody();
        String lastPrice = (String) ((Map) get("/api/v1/market/quotes/SBIN").getBody().get("quote")).get("lastPrice");

        assertThat(series).containsEntry("symbol", "SBIN").containsEntry("timeframe", "1m");
        assertThat((Map<String, Object>) series.get("source")).containsEntry("label", "SIMULATED DATA");
        List<Map<String, Object>> candles = (List) series.get("candles");
        assertThat(candles).hasSize(50);
        assertAscendingAndValid(candles);
        assertThat(candles.getLast().get("close")).isEqualTo(lastPrice);
    }

    @Test
    void aggregatedAndDailyTimeframesAreValid() {
        provider.tick();
        for (String timeframe : List.of("5m", "15m", "1h", "1d")) {
            List<Map<String, Object>> candles = (List) get("/api/v1/market/candles/ITC?timeframe=" + timeframe
                    + "&limit=20").getBody().get("candles");
            assertThat(candles).as(timeframe).isNotEmpty().hasSizeLessThanOrEqualTo(20);
            assertAscendingAndValid(candles);
        }
        List<Map<String, Object>> fiveMinute = (List) get("/api/v1/market/candles/ITC?timeframe=5m&limit=10")
                .getBody().get("candles");
        assertThat(fiveMinute).allSatisfy(c ->
                assertThat(Instant.parse((String) c.get("openTime")).getEpochSecond() % 300).isZero());
    }

    @Test
    void invalidCandleRequestsAreRejected() {
        ResponseEntity<Map> badTimeframe = get("/api/v1/market/candles/ITC?timeframe=2h");
        assertThat(badTimeframe.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badTimeframe.getBody()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(get("/api/v1/market/candles/ITC?limit=5000").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("/api/v1/market/candles/NOPE").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static void assertAscendingAndValid(List<Map<String, Object>> candles) {
        Instant previous = Instant.MIN;
        for (Map<String, Object> c : candles) {
            Instant t = Instant.parse((String) c.get("openTime"));
            assertThat(t).isAfter(previous);
            previous = t;
            BigDecimal high = dec(c.get("high"));
            BigDecimal low = dec(c.get("low"));
            assertThat(high).isGreaterThanOrEqualTo(dec(c.get("open"))).isGreaterThanOrEqualTo(dec(c.get("close")));
            assertThat(low).isLessThanOrEqualTo(dec(c.get("open"))).isLessThanOrEqualTo(dec(c.get("close")));
            assertThat(((Number) c.get("volume")).longValue()).isNotNegative();
        }
    }

    // ------------------------------------------------------------------ WebSocket

    private WebSocketStompClient stompClient() {
        // Tomcat's Java WebSocket client defaults to an 8 KB text buffer; one quote batch for all instruments is
        // larger. (Browsers have no such limit.)
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(512 * 1024);
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient(container));
        client.setInboundMessageSizeLimit(512 * 1024);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.getObjectMapper().findAndRegisterModules();
        client.setMessageConverter(converter);
        return client;
    }

    private StompSession connect(String origin) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (origin != null) {
            headers.setOrigin(origin);
        }
        return stompClient().connectAsync("ws://localhost:" + port + "/ws", headers,
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    private static BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<String, Object>) payload);
            }
        });
        return received;
    }

    @Test
    void quotesAreBroadcastOverWebSocketOnEveryTick() throws Exception {
        StompSession session = connect("http://localhost:5173");
        BlockingQueue<Map<String, Object>> received = subscribe(session, MarketDataBroadcaster.QUOTES_TOPIC);
        Thread.sleep(300); // let the SUBSCRIBE frame reach the broker

        provider.tick();
        Map<String, Object> message = received.poll(5, TimeUnit.SECONDS);

        assertThat(message).isNotNull().containsEntry("type", "QUOTES").containsEntry("version", 1);
        assertThat((Map<String, Object>) message.get("source")).containsEntry("label", "SIMULATED DATA");
        assertThat((List) message.get("quotes")).hasSize(SEEDED_INSTRUMENTS);
        session.disconnect();
    }

    @Test
    void clientsCannotPublishFakeQuotesToOtherSubscribers() throws Exception {
        StompSession victim = connect("http://localhost:5173");
        BlockingQueue<Map<String, Object>> received = subscribe(victim, MarketDataBroadcaster.QUOTES_TOPIC);
        StompSession attacker = connect("http://localhost:5173");
        Thread.sleep(300);

        attacker.send(MarketDataBroadcaster.QUOTES_TOPIC, Map.of("type", "QUOTES", "fake", true));

        assertThat(received.poll(1, TimeUnit.SECONDS)).as("fake message must not be relayed").isNull();
        victim.disconnect();
    }

    @Test
    void subscriptionsOutsideMarketTopicsAreRefused() throws Exception {
        StompSession session = connect("http://localhost:5173");
        BlockingQueue<Map<String, Object>> received = subscribe(session, "/topic/private-stuff");
        Thread.sleep(300);

        provider.tick();

        assertThat(received.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void webSocketHandshakeFromUnknownOriginIsRejected() {
        assertThatThrownBy(() -> connect("https://evil.example")).isInstanceOf(Exception.class);
    }
}
