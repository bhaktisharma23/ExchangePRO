import { Link } from 'react-router-dom';
import type { Quote } from '@/types/market';
import { PriceChange } from './PriceChange';
import { formatPrice, formatVolume } from '@/utils/format';

/** Dense quote table; each row links to the instrument page. */
export function QuoteTable({ quotes, emptyText = 'No instruments match.' }: { quotes: Quote[]; emptyText?: string }) {
  if (quotes.length === 0) {
    return <p className="py-6 text-center text-fg-muted">{emptyText}</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-left">
        <thead className="text-[11px] uppercase tracking-wider text-fg-muted">
          <tr className="border-b border-line">
            <th className="py-2 pr-3 font-medium">Symbol</th>
            <th className="py-2 pr-3 text-right font-medium">LTP</th>
            <th className="py-2 pr-3 text-right font-medium">Change</th>
            <th className="py-2 pr-3 text-right font-medium">Bid</th>
            <th className="py-2 pr-3 text-right font-medium">Ask</th>
            <th className="py-2 text-right font-medium">Volume</th>
          </tr>
        </thead>
        <tbody>
          {quotes.map((q) => (
            <tr key={q.symbol} className="border-b border-line/60 hover:bg-surface-2">
              <td className="py-1.5 pr-3">
                <Link to={`/instruments/${encodeURIComponent(q.symbol)}`} className="font-semibold hover:text-accent">
                  {q.symbol}
                </Link>
                <div className="max-w-[240px] truncate text-[11px] text-fg-muted">{q.name}</div>
              </td>
              <td className="num py-1.5 pr-3 text-right font-mono">{formatPrice(q.lastPrice)}</td>
              <td className="py-1.5 pr-3 text-right">
                <PriceChange change={q.change} changePercent={q.changePercent} />
              </td>
              <td className="num py-1.5 pr-3 text-right font-mono text-fg-muted">{formatPrice(q.bid)}</td>
              <td className="num py-1.5 pr-3 text-right font-mono text-fg-muted">{formatPrice(q.ask)}</td>
              <td className="num py-1.5 text-right font-mono text-fg-muted">{formatVolume(q.volume)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
