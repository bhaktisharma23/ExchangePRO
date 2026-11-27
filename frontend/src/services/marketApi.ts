import type { CandleSeries, MarketOverview, QuoteEnvelope, Timeframe } from '@/types/market';
import { apiRequest } from './apiClient';

const enc = encodeURIComponent; // symbols may contain '&' (e.g. M&M)

export const marketApi = {
  getOverview: (signal?: AbortSignal) => apiRequest<MarketOverview>('/api/v1/market/overview', { signal }),
  getQuote: (symbol: string, signal?: AbortSignal) =>
    apiRequest<QuoteEnvelope>(`/api/v1/market/quotes/${enc(symbol)}`, { signal }),
  getCandles: (symbol: string, timeframe: Timeframe, limit = 300, signal?: AbortSignal) =>
    apiRequest<CandleSeries>(`/api/v1/market/candles/${enc(symbol)}?timeframe=${timeframe}&limit=${limit}`, { signal }),
};
