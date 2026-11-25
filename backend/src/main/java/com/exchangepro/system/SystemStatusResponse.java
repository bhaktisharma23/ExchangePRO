package com.exchangepro.system;

import java.time.Instant;
import java.util.List;

/**
 * Public, non-sensitive snapshot of the running system. Returned by {@code GET /api/v1/system/status}.
 *
 * @param application   product name
 * @param version       backend build version
 * @param activeProfiles active Spring profiles
 * @param serverTime    current server time (UTC)
 * @param database      database connectivity and schema migration state
 * @param marketData    which market-data source is active and how it must be labelled in the UI
 */
public record SystemStatusResponse(
        String application,
        String version,
        List<String> activeProfiles,
        Instant serverTime,
        DatabaseStatus database,
        MarketDataStatus marketData) {

    public enum ComponentState { UP, DOWN }

    /**
     * @param state           UP if a query succeeded
     * @param productVersion  e.g. "PostgreSQL 16.10"; null when DOWN
     * @param schemaVersion   latest applied Flyway migration version; null when unknown
     */
    public record DatabaseStatus(ComponentState state, String productVersion, String schemaVersion) {}

    /**
     * @param mode       SIMULATION or REAL
     * @param label      text the UI must show next to every price (e.g. "SIMULATED DATA")
     * @param realTime   whether prices are real-time exchange prices (always false in SIMULATION)
     */
    public record MarketDataStatus(String mode, String label, boolean realTime) {}
}
