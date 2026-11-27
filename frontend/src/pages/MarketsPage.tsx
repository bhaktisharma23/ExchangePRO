import { useMemo, useState } from 'react';
import { Panel } from '@/components/ui/Panel';
import { FormAlert } from '@/components/ui/FormAlert';
import { DataSourceBadge } from '@/components/market/DataSourceBadge';
import { QuoteTable } from '@/components/market/QuoteTable';
import { MoversList } from '@/components/market/MoversList';
import { useMarketOverview } from '@/hooks/useMarketData';
import { useMarketStore } from '@/stores/marketStore';
import { breadth, filterQuotes, movers } from '@/utils/market';
import { describeError } from '@/utils/apiErrors';

/** Market overview: breadth, movers and all quotes, updated live over WebSocket. */
export function MarketsPage() {
  const { isPending, error } = useMarketOverview();
  const quoteMap = useMarketStore((s) => s.quotes);
  const [filter, setFilter] = useState('');

  const quotes = useMemo(() => Object.values(quoteMap).sort((a, b) => a.symbol.localeCompare(b.symbol)), [quoteMap]);
  const stats = useMemo(() => breadth(quotes), [quotes]);
  const top = useMemo(() => movers(quotes), [quotes]);
  const visible = useMemo(() => filterQuotes(quotes, filter), [quotes, filter]);

  const loading = isPending && quotes.length === 0;

  return (
    <div className="mx-auto max-w-7xl space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="text-base font-semibold">Markets</h1>
          <p className="text-fg-muted">NSE-listed equities with simulated prices. Not real exchange data.</p>
        </div>
        <DataSourceBadge />
      </div>

      {error && quotes.length === 0 && <FormAlert message={describeError(error)} />}
      {loading && (
        <div role="status" className="h-64 animate-pulse rounded-sm border border-line bg-surface">
          <span className="sr-only">Loading market data</span>
        </div>
      )}

      {quotes.length > 0 && (
        <>
          <div className="grid grid-cols-3 gap-px overflow-hidden rounded-sm border border-line bg-line">
            <Stat label="Advances" value={stats.advances} className="text-up" />
            <Stat label="Declines" value={stats.declines} className="text-down" />
            <Stat label="Unchanged" value={stats.unchanged} className="text-fg-muted" />
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <Panel title="Top gainers"><MoversList quotes={top.gainers} metric="change" /></Panel>
            <Panel title="Top losers"><MoversList quotes={top.losers} metric="change" /></Panel>
            <Panel title="Most active"><MoversList quotes={top.mostActive} metric="volume" /></Panel>
          </div>

          <Panel
            title={`All instruments (${visible.length})`}
            actions={
              <input
                aria-label="Filter instruments"
                placeholder="Filter…"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                className="h-6 w-44 rounded-sm border border-line bg-surface-2 px-2 text-[12px] outline-none focus:border-accent"
              />
            }
          >
            <QuoteTable quotes={visible} />
          </Panel>
        </>
      )}
    </div>
  );
}

function Stat({ label, value, className }: { label: string; value: number; className: string }) {
  return (
    <div className="bg-surface px-4 py-3">
      <p className="text-[11px] uppercase tracking-wider text-fg-muted">{label}</p>
      <p className={`num font-mono text-lg font-semibold ${className}`}>{value}</p>
    </div>
  );
}
