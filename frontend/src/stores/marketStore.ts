import { create } from 'zustand';
import type { MarketDataSource, Quote, QuotesMessage } from '@/types/market';

export type StreamStatus = 'disconnected' | 'connecting' | 'connected';

interface MarketState {
  /** Latest quote per symbol: REST snapshot first, then live WebSocket updates. */
  quotes: Record<string, Quote>;
  source: MarketDataSource | null;
  /** Server time of the newest data held. */
  asOf: string | null;
  stream: StreamStatus;
  applySnapshot: (source: MarketDataSource, asOf: string, quotes: Quote[]) => void;
  applyUpdate: (message: QuotesMessage) => void;
  setStream: (status: StreamStatus) => void;
  reset: () => void;
}

const initial = { quotes: {}, source: null, asOf: null, stream: 'disconnected' as StreamStatus };

/** Ignores data older than what is already held (a slow REST response must not overwrite a newer push). */
function isNewer(candidate: string, current: string | null) {
  return current === null || Date.parse(candidate) >= Date.parse(current);
}

export const useMarketStore = create<MarketState>()((set, get) => ({
  ...initial,
  applySnapshot: (source, asOf, quotes) => {
    if (!isNewer(asOf, get().asOf)) return;
    set({ source, asOf, quotes: Object.fromEntries(quotes.map((q) => [q.symbol, q])) });
  },
  applyUpdate: (message) => {
    if (!isNewer(message.asOf, get().asOf)) return;
    const quotes = { ...get().quotes };
    for (const q of message.quotes) quotes[q.symbol] = q;
    set({ source: message.source, asOf: message.asOf, quotes });
  },
  setStream: (stream) => set({ stream }),
  reset: () => set(initial),
}));
