import { describe, expect, it } from 'vitest';
import { applyLivePrice, toChartBars, toChartTime } from './candles';

describe('candle utils', () => {
  it('shifts chart time to IST so the axis reads in Indian time', () => {
    // 09:15 UTC = 14:45 IST
    expect(toChartTime('2026-01-15T09:15:00Z')).toBe(Date.UTC(2026, 0, 15, 14, 45) / 1000);
  });

  it('converts API candles to chart bars', () => {
    const [bar] = toChartBars([
      { openTime: '2026-01-15T09:15:00Z', open: '100.0500', high: '101.0000', low: '99.0000', close: '100.5000', volume: 7 },
    ]);
    expect(bar).toMatchObject({ open: 100.05, high: 101, low: 99, close: 100.5, volume: 7 });
  });

  it('updates the last bar when a tick falls in the same bucket', () => {
    const last = { time: toChartTime('2026-01-15T09:15:00Z'), open: 100, high: 101, low: 99, close: 100, volume: 5 };

    const updated = applyLivePrice(last, 102, '2026-01-15T09:17:30Z', 300);

    expect(updated).toEqual({ ...last, close: 102, high: 102 });
  });

  it('starts a new bar when a tick falls in the next bucket', () => {
    const last = { time: toChartTime('2026-01-15T09:15:00Z'), open: 100, high: 101, low: 99, close: 100, volume: 5 };

    const next = applyLivePrice(last, 98, '2026-01-15T09:20:05Z', 300);

    expect(next).toEqual({ time: toChartTime('2026-01-15T09:20:00Z'), open: 100, high: 100, low: 98, close: 98, volume: 0 });
  });

  it('ignores ticks older than the last bar', () => {
    const last = { time: toChartTime('2026-01-15T09:15:00Z'), open: 100, high: 101, low: 99, close: 100, volume: 5 };
    expect(applyLivePrice(last, 50, '2026-01-15T09:05:00Z', 300)).toBeNull();
  });
});
