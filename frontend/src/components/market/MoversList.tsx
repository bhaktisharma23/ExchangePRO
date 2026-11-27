import { Link } from 'react-router-dom';
import type { Quote } from '@/types/market';
import { formatPercent, formatPrice, formatVolume } from '@/utils/format';
import { direction } from '@/utils/market';

/** Compact list for top gainers / losers / most active. */
export function MoversList({ quotes, metric }: { quotes: Quote[]; metric: 'change' | 'volume' }) {
  return (
    <ul className="divide-y divide-line/60">
      {quotes.map((q) => {
        const dir = direction(q.changePercent);
        return (
          <li key={q.symbol} className="flex items-center justify-between py-1.5">
            <Link to={`/instruments/${encodeURIComponent(q.symbol)}`} className="font-semibold hover:text-accent">
              {q.symbol}
            </Link>
            <span className="num font-mono">
              <span className="mr-3 text-fg-muted">{formatPrice(q.lastPrice)}</span>
              {metric === 'change' ? (
                <span className={dir > 0 ? 'text-up' : dir < 0 ? 'text-down' : 'text-fg-muted'}>
                  {formatPercent(q.changePercent)}
                </span>
              ) : (
                <span>{formatVolume(q.volume)}</span>
              )}
            </span>
          </li>
        );
      })}
    </ul>
  );
}
