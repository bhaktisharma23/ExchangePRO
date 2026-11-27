package com.exchangepro.marketdata;

import com.exchangepro.common.error.ErrorCode;
import com.exchangepro.common.error.ExchangeProException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Chart resolutions. Only 1m and 1d are stored; 5m, 15m and 1h are aggregated from 1m candles at query time,
 * which avoids storing (and keeping consistent) several copies of the same data.
 */
public enum Timeframe {
    M1("1m", 60, Storage.MINUTE),
    M5("5m", 300, Storage.AGGREGATED),
    M15("15m", 900, Storage.AGGREGATED),
    H1("1h", 3600, Storage.AGGREGATED),
    D1("1d", 86_400, Storage.DAY);

    enum Storage { MINUTE, AGGREGATED, DAY }

    private final String code;
    private final long seconds;
    private final Storage storage;

    Timeframe(String code, long seconds, Storage storage) {
        this.code = code;
        this.seconds = seconds;
        this.storage = storage;
    }

    public String code() { return code; }
    public long seconds() { return seconds; }
    Storage storage() { return storage; }

    public static Timeframe fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst()
                .orElseThrow(() -> new ExchangeProException(ErrorCode.VALIDATION_FAILED,
                        "Unsupported timeframe '" + code + "'. Use one of: "
                                + Arrays.stream(values()).map(Timeframe::code).collect(Collectors.joining(", "))));
    }
}
