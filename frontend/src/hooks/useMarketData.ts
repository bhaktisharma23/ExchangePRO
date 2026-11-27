import { useEffect } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { marketApi } from '@/services/marketApi';
import { instrumentApi } from '@/services/instrumentApi';
import { connectMarketStream, disconnectMarketStream } from '@/services/marketStream';
import { useMarketStore } from '@/stores/marketStore';
import { useAuthStore } from '@/stores/authStore';
import type { MarketDataSource, Quote, Timeframe } from '@/types/market';

/** Opens the live stream while signed in and closes it on sign-out. Mount once (App). */
export function useMarketStream() {
  const status = useAuthStore((s) => s.status);
  useEffect(() => {
    if (status !== 'authenticated') return;
    connectMarketStream();
    return () => {
      disconnectMarketStream();
      useMarketStore.getState().reset();
    };
  }, [status]);
}

/**
 * Initial market snapshot from REST, copied into the live store. The WebSocket keeps it current afterwards; the
 * slow refetch is a safety net while the stream is reconnecting.
 */
export function useMarketOverview() {
  const query = useQuery({
    queryKey: ['market', 'overview'],
    queryFn: ({ signal }) => marketApi.getOverview(signal),
    refetchInterval: 60_000,
  });
  useEffect(() => {
    if (query.data) useMarketStore.getState().applySnapshot(query.data.source, query.data.asOf, query.data.quotes);
  }, [query.data]);
  return query;
}

/** Live quote for one symbol from the store, with a REST fallback until the store has it. */
export function useLiveQuote(symbol: string): {
  quote: Quote | undefined;
  source: MarketDataSource | undefined;
  isLoading: boolean;
  error: unknown;
} {
  const live = useMarketStore((s) => s.quotes[symbol]);
  const fallback = useQuery({
    queryKey: ['market', 'quote', symbol],
    queryFn: ({ signal }) => marketApi.getQuote(symbol, signal),
    enabled: !live,
  });
  return {
    quote: live ?? fallback.data?.quote,
    source: fallback.data?.source,
    isLoading: !live && fallback.isPending,
    error: fallback.error,
  };
}

export function useInstrument(symbol: string) {
  return useQuery({
    queryKey: ['instrument', symbol],
    queryFn: ({ signal }) => instrumentApi.get(symbol, signal),
    staleTime: 10 * 60_000,
    retry: false,
  });
}

/** Candles for the chart. Refetched every minute so closed candles come from the server, not the client. */
export function useCandles(symbol: string, timeframe: Timeframe) {
  return useQuery({
    queryKey: ['market', 'candles', symbol, timeframe],
    queryFn: ({ signal }) => marketApi.getCandles(symbol, timeframe, 300, signal),
    refetchInterval: 60_000,
    placeholderData: keepPreviousData,
  });
}

export function useInstrumentSearch(query: string) {
  const q = query.trim();
  return useQuery({
    queryKey: ['instrument', 'search', q],
    queryFn: ({ signal }) => instrumentApi.search(q, 8, signal),
    enabled: q.length > 0,
    staleTime: 60_000,
  });
}
