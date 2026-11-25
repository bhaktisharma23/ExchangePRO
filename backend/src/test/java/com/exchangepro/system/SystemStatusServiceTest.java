package com.exchangepro.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exchangepro.config.ExchangeProProperties;
import com.exchangepro.marketdata.MarketDataMode;
import com.exchangepro.system.SystemStatusResponse.ComponentState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

class SystemStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T09:15:00Z");

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<Flyway> flyway = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<BuildProperties> build = mock(ObjectProvider.class);

    private final SystemStatusService service = new SystemStatusService(
            jdbcTemplate, flyway, build,
            new ExchangeProProperties(
                    new ExchangeProProperties.Cors(List.of("http://localhost:5173")),
                    new ExchangeProProperties.MarketData(MarketDataMode.SIMULATION)),
            new MockEnvironment(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void reportsDatabaseUpWithVersion() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("16.10");

        SystemStatusResponse status = service.currentStatus();

        assertThat(status.application()).isEqualTo("EXCHANGE PRO");
        assertThat(status.serverTime()).isEqualTo(NOW);
        assertThat(status.version()).isEqualTo("development");
        assertThat(status.database().state()).isEqualTo(ComponentState.UP);
        assertThat(status.database().productVersion()).isEqualTo("PostgreSQL 16.10");
    }

    @Test
    void reportsDatabaseDownInsteadOfFailing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class)))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        SystemStatusResponse status = service.currentStatus();

        assertThat(status.database().state()).isEqualTo(ComponentState.DOWN);
        assertThat(status.database().productVersion()).isNull();
    }

    @Test
    void simulationModeIsLabelledAndNeverRealTime() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("16.10");

        SystemStatusResponse.MarketDataStatus marketData = service.currentStatus().marketData();

        assertThat(marketData.mode()).isEqualTo("SIMULATION");
        assertThat(marketData.label()).isEqualTo("SIMULATED DATA");
        assertThat(marketData.realTime()).isFalse();
    }
}
