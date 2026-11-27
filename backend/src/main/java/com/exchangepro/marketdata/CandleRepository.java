package com.exchangepro.marketdata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Candle persistence with plain JDBC. Candles are high-volume, append-mostly rows: batched SQL is far cheaper
 * here than JPA entities, and aggregation is done by PostgreSQL ({@code date_bin}) instead of in Java.
 */
@Repository
public class CandleRepository {

    public static final String MINUTE = "1m";
    public static final String DAY = "1d";
    /** date_bin origin at midnight IST, so hourly buckets align to IST hours. */
    private static final String IST_ORIGIN = "TIMESTAMPTZ '2000-01-01 00:00:00+05:30'";
    private static final int BATCH_SIZE = 1000;

    private static final RowMapper<Candle> MAPPER = CandleRepository::map;

    private final JdbcTemplate jdbc;

    public CandleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts candles; rows that already exist are left untouched (idempotent, safe to retry). */
    public void insertAll(UUID instrumentId, String timeframe, List<Candle> candles) {
        String sql = """
                INSERT INTO candles (instrument_id, timeframe, open_time, open, high, low, close, volume)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (instrument_id, timeframe, open_time) DO NOTHING""";
        jdbc.batchUpdate(sql, candles, BATCH_SIZE, (ps, c) -> {
            ps.setObject(1, instrumentId);
            ps.setString(2, timeframe);
            ps.setTimestamp(3, Timestamp.from(c.openTime()));
            ps.setBigDecimal(4, c.open());
            ps.setBigDecimal(5, c.high());
            ps.setBigDecimal(6, c.low());
            ps.setBigDecimal(7, c.close());
            ps.setLong(8, c.volume());
        });
    }

    /** Inserts or replaces one candle (used for the still-forming daily candle). */
    public void upsert(UUID instrumentId, String timeframe, Candle c) {
        jdbc.update("""
                INSERT INTO candles (instrument_id, timeframe, open_time, open, high, low, close, volume)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (instrument_id, timeframe, open_time)
                DO UPDATE SET open = EXCLUDED.open, high = EXCLUDED.high, low = EXCLUDED.low,
                              close = EXCLUDED.close, volume = EXCLUDED.volume""",
                instrumentId, timeframe, Timestamp.from(c.openTime()), c.open(), c.high(), c.low(), c.close(),
                c.volume());
    }

    public boolean hasAny(UUID instrumentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM candles WHERE instrument_id = ?)", Boolean.class, instrumentId));
    }

    public Optional<Candle> latest(UUID instrumentId, String timeframe) {
        return jdbc.query("""
                SELECT * FROM candles WHERE instrument_id = ? AND timeframe = ?
                ORDER BY open_time DESC LIMIT 1""", MAPPER, instrumentId, timeframe).stream().findFirst();
    }

    public Optional<Candle> latestBefore(UUID instrumentId, String timeframe, Instant before) {
        return jdbc.query("""
                SELECT * FROM candles WHERE instrument_id = ? AND timeframe = ? AND open_time < ?
                ORDER BY open_time DESC LIMIT 1""", MAPPER, instrumentId, timeframe, Timestamp.from(before))
                .stream().findFirst();
    }

    /** The most recent {@code limit} stored candles opening before {@code before}, oldest first. */
    public List<Candle> recent(UUID instrumentId, String timeframe, Instant before, int limit) {
        List<Candle> rows = new ArrayList<>(jdbc.query("""
                SELECT * FROM candles WHERE instrument_id = ? AND timeframe = ? AND open_time < ?
                ORDER BY open_time DESC LIMIT ?""", MAPPER, instrumentId, timeframe, Timestamp.from(before), limit));
        Collections.reverse(rows);
        return rows;
    }

    /**
     * Aggregates stored 1m candles into buckets of {@code bucketSeconds} (5m, 15m, 1h), oldest first.
     * open = first open in the bucket, close = last close, high/low = extremes, volume = sum.
     */
    public List<Candle> aggregated(UUID instrumentId, long bucketSeconds, Instant from, int limit) {
        List<Candle> rows = new ArrayList<>(jdbc.query("""
                SELECT date_bin(make_interval(secs => ?), open_time, %s) AS open_time,
                       (array_agg(open  ORDER BY open_time ASC))[1]  AS open,
                       max(high)                                      AS high,
                       min(low)                                       AS low,
                       (array_agg(close ORDER BY open_time DESC))[1] AS close,
                       sum(volume)                                    AS volume
                  FROM candles
                 WHERE instrument_id = ? AND timeframe = '1m' AND open_time >= ?
                 GROUP BY 1
                 ORDER BY 1 DESC
                 LIMIT ?""".formatted(IST_ORIGIN),
                MAPPER, bucketSeconds, instrumentId, Timestamp.from(from), limit));
        Collections.reverse(rows);
        return rows;
    }

    private static Candle map(ResultSet rs, int rowNum) throws SQLException {
        return new Candle(rs.getTimestamp("open_time").toInstant(), rs.getBigDecimal("open"),
                rs.getBigDecimal("high"), rs.getBigDecimal("low"), rs.getBigDecimal("close"), rs.getLong("volume"));
    }
}
