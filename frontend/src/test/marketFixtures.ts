import type { MarketDataSource, MarketOverview, Quote } from '@/types/market';

export const simulatedSource: MarketDataSource = {
  mode: 'SIMULATION',
  provider: 'EXCHANGE PRO simulator',
  label: 'SIMULATED DATA',
  realTime: false,
};

export function quote(symbol: string, overrides: Partial<Quote> = {}): Quote {
  return {
    symbol,
    name: `${symbol} Ltd`,
    lastPrice: '100.0000',
    change: '0.0000',
    changePercent: '0.00',
    open: '100.0000',
    high: '101.0000',
    low: '99.0000',
    previousClose: '100.0000',
    bid: '99.9500',
    ask: '100.0000',
    bidSize: 10,
    askSize: 12,
    volume: 1000,
    timestamp: '2026-01-15T09:15:00Z',
    ...overrides,
  };
}

export function overview(quotes: Quote[], asOf = '2026-01-15T09:15:00Z'): MarketOverview {
  return { source: simulatedSource, asOf, advances: 0, declines: 0, unchanged: 0, topGainers: [], topLosers: [], mostActive: [], quotes };
}
