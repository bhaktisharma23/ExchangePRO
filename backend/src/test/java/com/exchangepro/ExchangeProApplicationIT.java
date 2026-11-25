package com.exchangepro;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full-stack integration test: boots the complete application on a random port against a REAL PostgreSQL 16
 * server (embedded binaries, no Docker), lets Flyway migrate the schema, then calls the HTTP API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@ActiveProfiles("test")
class ExchangeProApplicationIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliedBaselineMigration() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void actuatorHealthIsUpIncludingDatabase() {
        ResponseEntity<Map> response = http.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        Map<String, Object> components = (Map<String, Object>) response.getBody().get("components");
        assertThat((Map<String, Object>) components.get("db")).containsEntry("status", "UP");
    }

    @Test
    void livenessAndReadinessProbesAreUp() {
        assertThat(http.getForEntity("/actuator/health/liveness", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(http.getForEntity("/actuator/health/readiness", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("unchecked")
    void systemStatusReportsRealDatabaseAndSimulationMode() {
        ResponseEntity<Map> response = http.getForEntity("/api/v1/system/status", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
        Map<String, Object> body = response.getBody();
        Map<String, Object> database = (Map<String, Object>) body.get("database");
        String latestMigration = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        assertThat(database).containsEntry("state", "UP").containsEntry("schemaVersion", latestMigration);
        assertThat((String) database.get("productVersion")).startsWith("PostgreSQL 16");
        assertThat((Map<String, Object>) body.get("marketData"))
                .containsEntry("mode", "SIMULATION")
                .containsEntry("label", "SIMULATED DATA")
                .containsEntry("realTime", false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownEndpointRequiresAuthenticationAndReturnsProblemDetail() {
        // Deny-by-default: anonymous callers cannot even probe which endpoints exist.
        ResponseEntity<Map> response = http.getForEntity("/api/v1/does-not-exist", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("code", "UNAUTHORIZED").containsKey("correlationId");
    }

    @Test
    void openApiDocumentIsPublished() {
        ResponseEntity<String> response = http.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("EXCHANGE PRO API").contains("/api/v1/system/status");
    }
}
