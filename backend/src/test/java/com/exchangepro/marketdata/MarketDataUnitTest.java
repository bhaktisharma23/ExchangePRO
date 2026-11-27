package com.exchangepro.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exchangepro.common.error.ExchangeProException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketDataUnitTest {

    private static Candle candle(String time, String o, String h, String l, String c, long v) {
        return new Candle(Instant.parse(time), new BigDecimal(o), new BigDecimal(h), new BigDecimal(l),
                new BigDecimal(c), v);
    }

    @Test
    void istDayStartsAtMidnightIst() {
        assertThat(MarketClock.dayStart(Instant.parse("2026-01-15T18:29:59Z")))
                .isEqualTo(Instant.parse("2026-01-14T18:30:00Z"));
        assertThat(MarketClock.dayStart(Instant.parse("2026-01-15T18:30:00Z")))
                .isEqualTo(Instant.parse("2026-01-15T18:30:00Z"));
    }

    @Test
    void hourlyBucketsAlignToIstHours() {
        // 10:47 IST = 05:17 UTC -> bucket 10:00 IST = 04:30 UTC
        assertThat(MarketClock.bucketStart(Instant.parse("2026-01-15T05:17:00Z"), 3600))
                .isEqualTo(Instant.parse("2026-01-15T04:30:00Z"));
        assertThat(MarketClock.bucketStart(Instant.parse("2026-01-15T05:17:00Z"), 300))
                .isEqualTo(Instant.parse("2026-01-15T05:15:00Z"));
    }

    @Test
    void candleRejectsInconsistentOhlc() {
        assertThatThrownBy(() -> candle("2026-01-15T05:00:00Z", "10", "9", "8", "9", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> candle("2026-01-15T05:00:00Z", "10", "11", "8", "9", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void candleTicksAndMergesUpdateHighLowCloseVolume() {
        Candle c = Candle.open(Instant.EPOCH, new BigDecimal("100"), 5)
                .withTick(new BigDecimal("103"), 1).withTick(new BigDecimal("98"), 2).withTick(new BigDecimal("101"), 3);
        assertThat(c).isEqualTo(new Candle(Instant.EPOCH, new BigDecimal("100"), new BigDecimal("103"),
                new BigDecimal("98"), new BigDecimal("101"), 11));

        Candle merged = c.mergeLater(candle("1970-01-01T00:01:00Z", "101", "105", "100", "104", 4));
        assertThat(merged.open()).isEqualByComparingTo("100");
        assertThat(merged.high()).isEqualByComparingTo("105");
        assertThat(merged.low()).isEqualByComparingTo("98");
        assertThat(merged.close()).isEqualByComparingTo("104");
        assertThat(merged.volume()).isEqualTo(15);
    }

    @Test
    void formingMinuteIsMergedIntoItsBucketOrStartsANewOne() {
        Candle bucket = candle("2026-01-15T05:15:00Z", "100", "102", "99", "101", 10);
        Candle formingSameBucket = candle("2026-01-15T05:17:00Z", "101", "104", "101", "103", 2);
        Candle formingNextBucket = candle("2026-01-15T05:20:00Z", "101", "101", "100", "100", 2);

        List<Candle> merged = MarketDataService.mergeIntoBuckets(List.of(bucket), formingSameBucket, 300);
        assertThat(merged).hasSize(1);
        assertThat(merged.getFirst().high()).isEqualByComparingTo("104");
        assertThat(merged.getFirst().close()).isEqualByComparingTo("103");
        assertThat(merged.getFirst().volume()).isEqualTo(12);

        List<Candle> appended = MarketDataService.mergeIntoBuckets(List.of(bucket), formingNextBucket, 300);
        assertThat(appended).hasSize(2);
        assertThat(appended.getLast().openTime()).isEqualTo(Instant.parse("2026-01-15T05:20:00Z"));
    }

    @Test
    void timeframeCodesParseAndUnknownCodesAreRejected() {
        assertThat(Timeframe.fromCode("15m")).isEqualTo(Timeframe.M15);
        assertThatThrownBy(() -> Timeframe.fromCode("2h")).isInstanceOf(ExchangeProException.class)
                .hasMessageContaining("1m, 5m, 15m, 1h, 1d");
    }

    @Test
    void simulatedSourceIsNeverRealTime() {
        assertThat(MarketDataSource.SIMULATED.realTime()).isFalse();
        assertThat(MarketDataSource.SIMULATED.label()).isEqualTo("SIMULATED DATA");
    }
}
