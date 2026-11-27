import { useEffect, useRef } from 'react';
import {
  CandlestickSeries,
  ColorType,
  HistogramSeries,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts';
import type { Candle } from '@/types/market';
import { applyLivePrice, toChartBars, type ChartBar } from '@/utils/candles';

const COLORS = {
  background: '#11151A',
  text: '#8994A3',
  grid: '#1B222B',
  border: '#252D38',
  up: '#00C087',
  down: '#F04452',
};

interface CandlestickChartProps {
  candles: Candle[];
  bucketSeconds: number;
  /** Latest price and its timestamp, applied to the last bar between candle refreshes. */
  livePrice?: string;
  liveTimestamp?: string;
}

/**
 * Candlestick + volume chart (TradingView Lightweight Charts, Apache-2.0; the attribution logo stays enabled
 * as its licence requests). Data comes only from the backend candle API plus live quotes; nothing is invented.
 */
export function CandlestickChart({ candles, bucketSeconds, livePrice, liveTimestamp }: CandlestickChartProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const priceSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);
  const lastBarRef = useRef<ChartBar | undefined>(undefined);

  useEffect(() => {
    if (!containerRef.current) return;
    const chart = createChart(containerRef.current, {
      autoSize: true,
      layout: {
        background: { type: ColorType.Solid, color: COLORS.background },
        textColor: COLORS.text,
        fontSize: 11,
        attributionLogo: true,
      },
      grid: { vertLines: { color: COLORS.grid }, horzLines: { color: COLORS.grid } },
      rightPriceScale: { borderColor: COLORS.border },
      timeScale: { borderColor: COLORS.border, timeVisible: true, secondsVisible: false },
    });
    priceSeriesRef.current = chart.addSeries(CandlestickSeries, {
      upColor: COLORS.up,
      downColor: COLORS.down,
      borderUpColor: COLORS.up,
      borderDownColor: COLORS.down,
      wickUpColor: COLORS.up,
      wickDownColor: COLORS.down,
    });
    priceSeriesRef.current.priceScale().applyOptions({ scaleMargins: { top: 0.08, bottom: 0.25 } });
    volumeSeriesRef.current = chart.addSeries(HistogramSeries, { priceFormat: { type: 'volume' }, priceScaleId: '' });
    volumeSeriesRef.current.priceScale().applyOptions({ scaleMargins: { top: 0.8, bottom: 0 } });
    chartRef.current = chart;
    return () => {
      chart.remove();
      chartRef.current = null;
      priceSeriesRef.current = null;
      volumeSeriesRef.current = null;
    };
  }, []);

  useEffect(() => {
    const bars = toChartBars(candles);
    priceSeriesRef.current?.setData(bars.map((b) => ({ ...b, time: b.time as UTCTimestamp })));
    volumeSeriesRef.current?.setData(
      bars.map((b) => ({
        time: b.time as UTCTimestamp,
        value: b.volume,
        color: b.close >= b.open ? `${COLORS.up}55` : `${COLORS.down}55`,
      })),
    );
    lastBarRef.current = bars.at(-1);
  }, [candles]);

  useEffect(() => {
    if (!livePrice || !liveTimestamp) return;
    const bar = applyLivePrice(lastBarRef.current, Number(livePrice), liveTimestamp, bucketSeconds);
    if (!bar) return;
    priceSeriesRef.current?.update({ ...bar, time: bar.time as UTCTimestamp });
    lastBarRef.current = bar;
  }, [livePrice, liveTimestamp, bucketSeconds]);

  return <div ref={containerRef} className="h-full w-full" data-testid="candlestick-chart" />;
}
