package com.exchangepro.marketdata;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Time-bucketing rules shared by the simulator, the candle store and the API.
 *
 * <p>Days and hourly buckets follow Indian Standard Time (UTC+05:30): a daily candle runs from 00:00 to 24:00
 * IST, and a 1-hour candle from hh:00 to hh+1:00 IST (which is hh:30 UTC).
 */
public final class MarketClock {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    /** IST offset in seconds. India has no daylight saving time, so this is constant. */
    static final long IST_OFFSET_SECONDS = 5 * 3600 + 30 * 60;

    private MarketClock() {}

    public static Instant minuteStart(Instant t) {
        return t.truncatedTo(ChronoUnit.MINUTES);
    }

    public static Instant dayStart(Instant t) {
        return t.atZone(IST).toLocalDate().atStartOfDay(IST).toInstant();
    }

    /** Start of the IST-aligned bucket of {@code bucketSeconds} containing {@code t}. */
    public static Instant bucketStart(Instant t, long bucketSeconds) {
        long local = t.getEpochSecond() + IST_OFFSET_SECONDS;
        return Instant.ofEpochSecond(Math.floorDiv(local, bucketSeconds) * bucketSeconds - IST_OFFSET_SECONDS);
    }
}
