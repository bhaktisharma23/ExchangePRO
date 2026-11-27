import type { Instrument, Page } from '@/types/market';
import { apiRequest } from './apiClient';

export const instrumentApi = {
  search: (query: string, size = 8, signal?: AbortSignal) =>
    apiRequest<Page<Instrument>>(`/api/v1/instruments?query=${encodeURIComponent(query)}&size=${size}`, { signal }),
  get: (symbol: string, signal?: AbortSignal) =>
    apiRequest<Instrument>(`/api/v1/instruments/${encodeURIComponent(symbol)}`, { signal }),
};
