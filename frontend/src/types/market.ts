/** Mirrors com.exchangepro.marketdata.MarketDataSource. */
export interface MarketDataSource {
  mode: 'SIMULATION' | 'REAL';
  provider: string;
  /** Must be shown next to prices, e.g. "SIMULATED DATA". */
  label: string;
  realTime: boolean;
}

/** Mirrors QuoteResponse. Decimal values are strings; convert to numbers only for display/charting. */
export interface Quote {
  symbol: string;
  name: string;
  lastPrice: string;
  change: string;
  changePercent: string;
  open: string;
  high: string;
  low: string;
  previousClose: string;
  bid: string;
  ask: string;
  bidSize: number;
  askSize: number;
  volume: number;
  timestamp: string;
}

export interface Instrument {
  symbol: string;
  name: string;
  exchange: string;
  sector: string;
  currency: string;
  tickSize: string;
  lotSize: number;
}

export interface Candle {
  openTime: string;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: number;
}

export type Timeframe = '1m' | '5m' | '15m' | '1h' | '1d';

export const TIMEFRAMES: { value: Timeframe; label: string; seconds: number }[] = [
  { value: '1m', label: '1m', seconds: 60 },
  { value: '5m', label: '5m', seconds: 300 },
  { value: '15m', label: '15m', seconds: 900 },
  { value: '1h', label: '1H', seconds: 3600 },
  { value: '1d', label: '1D', seconds: 86400 },
];

export interface CandleSeries {
  symbol: string;
  timeframe: Timeframe;
  source: MarketDataSource;
  candles: Candle[];
}

export interface QuoteEnvelope {
  source: MarketDataSource;
  quote: Quote;
}

export interface MarketOverview {
  source: MarketDataSource;
  asOf: string;
  advances: number;
  declines: number;
  unchanged: number;
  topGainers: Quote[];
  topLosers: Quote[];
  mostActive: Quote[];
  quotes: Quote[];
}

/** WebSocket payload on /topic/market/quotes (version 1). */
export interface QuotesMessage {
  type: 'QUOTES';
  version: number;
  source: MarketDataSource;
  asOf: string;
  quotes: Quote[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
