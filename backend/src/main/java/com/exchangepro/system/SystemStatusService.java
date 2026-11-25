package com.exchangepro.system;

import com.exchangepro.config.ExchangeProProperties;
import com.exchangepro.marketdata.MarketDataMode;
import com.exchangepro.system.SystemStatusResponse.ComponentState;
import com.exchangepro.system.SystemStatusResponse.DatabaseStatus;
import com.exchangepro.system.SystemStatusResponse.MarketDataStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Builds the {@link SystemStatusResponse}. Database problems are reported as {@code DOWN} instead of
 * failing the request, so the UI can still show <i>what</i> is broken.
 */
@Service
public class SystemStatusService {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusService.class);
    static final String APPLICATION_NAME = "EXCHANGE PRO";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<Flyway> flyway;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final ExchangeProProperties properties;
    private final Environment environment;
    private final Clock clock;

    public SystemStatusService(JdbcTemplate jdbcTemplate,
                               ObjectProvider<Flyway> flyway,
                               ObjectProvider<BuildProperties> buildProperties,
                               ExchangeProProperties properties,
                               Environment environment,
                               Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
        this.buildProperties = buildProperties;
        this.properties = properties;
        this.environment = environment;
        this.clock = clock;
    }

    public SystemStatusResponse currentStatus() {
        BuildProperties build = buildProperties.getIfAvailable();
        return new SystemStatusResponse(
                APPLICATION_NAME,
                build != null ? build.getVersion() : "development",
                Arrays.asList(environment.getActiveProfiles()),
                Instant.now(clock),
                databaseStatus(),
                marketDataStatus());
    }

    private DatabaseStatus databaseStatus() {
        try {
            String product = jdbcTemplate.queryForObject("SHOW server_version", String.class);
            return new DatabaseStatus(ComponentState.UP, "PostgreSQL " + product, currentSchemaVersion());
        } catch (DataAccessException ex) {
            log.warn("Database status check failed: {}", ex.getMessage());
            return new DatabaseStatus(ComponentState.DOWN, null, null);
        }
    }

    private String currentSchemaVersion() {
        Flyway fw = flyway.getIfAvailable();
        if (fw == null) {
            return null;
        }
        MigrationInfo current = fw.info().current();
        return current != null && current.getVersion() != null ? current.getVersion().getVersion() : null;
    }

    private MarketDataStatus marketDataStatus() {
        MarketDataMode mode = properties.marketData().mode();
        // Simulated prices are never real-time exchange prices. REAL mode is not yet available (see MarketDataModeGuard).
        return new MarketDataStatus(mode.name(), mode.displayLabel(), false);
    }
}
