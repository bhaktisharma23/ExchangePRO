import { direction } from '@/utils/market';
import { formatPercent, formatSignedPrice } from '@/utils/format';

/** Change and % change coloured green/red; the sign is always shown as text too (not colour alone). */
export function PriceChange({ change, changePercent, className = '' }: {
  change?: string;
  changePercent?: string;
  className?: string;
}) {
  const dir = direction(change);
  const color = dir > 0 ? 'text-up' : dir < 0 ? 'text-down' : 'text-fg-muted';
  return (
    <span className={`num font-mono ${color} ${className}`}>
      {formatSignedPrice(change)} ({formatPercent(changePercent)})
    </span>
  );
}
