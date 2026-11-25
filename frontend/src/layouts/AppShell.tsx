import { Outlet } from 'react-router-dom';
import { Activity } from 'lucide-react';
import { MarketDataBadge } from '@/components/MarketDataBadge';
import { useSystemStatus } from '@/hooks/useSystemStatus';

/** Top-level layout: brand bar, routed content and a persistent virtual-funds disclaimer. */
export function AppShell() {
  const { data } = useSystemStatus();

  return (
    <div className="flex min-h-full flex-col">
      <header className="flex h-11 shrink-0 items-center justify-between border-b border-line bg-surface px-4">
        <div className="flex items-center gap-2">
          <Activity className="h-4 w-4 text-accent" aria-hidden />
          <span className="text-sm font-bold tracking-[0.18em]">EXCHANGE PRO</span>
        </div>
        <MarketDataBadge marketData={data?.marketData} />
      </header>

      <main className="flex-1 p-4">
        <Outlet />
      </main>

      <footer className="border-t border-line px-4 py-2 text-[11px] text-fg-muted">
        Trading simulator. All funds are virtual; no real securities are bought or sold.
      </footer>
    </div>
  );
}
