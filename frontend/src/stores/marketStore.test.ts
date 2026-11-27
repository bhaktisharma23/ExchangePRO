import { describe, expect, it } from 'vitest';
import { useMarketStore } from './marketStore';
import { handleQuotesFrame } from '@/services/marketStream';
import { quote, simulatedSource } from '@/test/marketFixtures';

const message = (asOf: string, price: string) =>
  JSON.stringify({ type: 'QUOTES', version: 1, source: simulatedSource, asOf, quotes: [quote('TCS', { lastPrice: price })] });

describe('market store + stream frames', () => {
  it('applies a snapshot and then live updates', () => {
    useMarketStore.getState().applySnapshot(simulatedSource, '2026-01-15T09:15:00Z', [quote('TCS'), quote('INFY')]);

    expect(handleQuotesFrame(message('2026-01-15T09:15:01Z', '101.0000'))).toBe(true);

    const state = useMarketStore.getState();
    expect(state.quotes.TCS?.lastPrice).toBe('101.0000');
    expect(state.quotes.INFY).toBeDefined(); // untouched symbols are kept
    expect(state.asOf).toBe('2026-01-15T09:15:01Z');
  });

  it('ignores stale data arriving out of order', () => {
    handleQuotesFrame(message('2026-01-15T09:15:05Z', '105.0000'));
    handleQuotesFrame(message('2026-01-15T09:15:01Z', '101.0000'));
    useMarketStore.getState().applySnapshot(simulatedSource, '2026-01-15T09:15:00Z', [quote('TCS')]);

    expect(useMarketStore.getState().quotes.TCS?.lastPrice).toBe('105.0000');
  });

  it('rejects malformed frames and unknown message versions', () => {
    expect(handleQuotesFrame('not json')).toBe(false);
    expect(handleQuotesFrame(JSON.stringify({ type: 'QUOTES', version: 2, quotes: [] }))).toBe(false);
    expect(handleQuotesFrame(JSON.stringify({ type: 'OTHER', version: 1, quotes: [] }))).toBe(false);
    expect(useMarketStore.getState().asOf).toBeNull();
  });
});
