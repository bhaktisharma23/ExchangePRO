import type { Candle } from '@/types/market';

/** Display-only bar for the chart library, which needs JS numbers. Never use these numbers for arithmetic on money. */
export interface ChartBar {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

const IST_OFFSET_SECONDS = 19_800; // UTC+05:30

/**
 * Chart time for a candle. The chart library renders timestamps as UTC, so they are shifted by +05:30 to make
 * the axis read in Indian Standard Time.
 */
export function toChartTime(iso: string): number {
  return Math.floor(Date.parse(iso) / 1000) + IST_OFFSET_SECONDS;
}

export function toChartBars(candles: Candle[]): ChartBar[] {
  return candles.map((c) => ({
    time: toChartTime(c.openTime),
    open: Number(c.open),
    high: Number(c.high),
    low: Number(c.low),
    close: Number(c.close),
    volume: c.volume,
  }));
}

/** Start of the IST-aligned bucket (in chart time) that contains chart time `t`. */
export function bucketOf(t: number, bucketSeconds: number): number {
  return Math.floor(t / bucketSeconds) * bucketSeconds;
}

/**
 * Applies a live price to the last bar between candle refreshes: updates close/high/low if the tick falls in the
 * last bar's bucket, or starts a new bar. Returns the bar to draw, or null if the tick is older than the last bar.
 */
export function applyLivePrice(last: ChartBar | undefined, price: number, timestampIso: string, bucketSeconds: number): ChartBar | null {
  const bucket = bucketOf(toChartTime(timestampIso), bucketSeconds);
  if (!last) return { time: bucket, open: price, high: price, low: price, close: price, volume: 0 };
  if (bucket < last.time) return null;
  if (bucket === last.time) {
    return { ...last, close: price, high: Math.max(last.high, price), low: Math.min(last.low, price) };
  }
  return { time: bucket, open: last.close, high: Math.max(last.close, price), low: Math.min(last.close, price), close: price, volume: 0 };
}
