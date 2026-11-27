import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Panel } from '@/components/ui/Panel';
import { KeyValueList } from '@/components/ui/KeyValueList';
import { FormAlert } from '@/components/ui/FormAlert';
import { DataSourceBadge } from '@/components/market/DataSourceBadge';
import { PriceChange } from '@/components/market/PriceChange';
import { CandlestickChart } from '@/charts/CandlestickChart';
import { useCandles, useInstrument, useLiveQuote } from '@/hooks/useMarketData';
import { ApiError } from '@/services/apiClient';
import { TIMEFRAMES, type Timeframe } from '@/types/market';
import { formatPrice, formatVolume } from '@/utils/format';
import { describeError } from '@/utils/apiErrors';

/** One instrument: live quote, candlestick chart with timeframes, and quote details. */
export function InstrumentPage() {
  const symbol = decodeURIComponent(useParams().symbol ?? '').toUpperCase();
  const [timeframe, setTimeframe] = useState<Timeframe>('5m');
  const instrument = useInstrument(symbol);
  const { quote, source } = useLiveQuote(symbol);
  const candles = useCandles(symbol, timeframe);
  const bucketSeconds = TIMEFRAMES.find((t) => t.value === timeframe)!.seconds;

  if (instrument.error instanceof ApiError && instrument.error.status === 404) {
    return (
      <div className="mx-auto mt-16 max-w-md text-center">
        <p className="text-base font-semibold">Instrument {symbol} not found</p>
        <Link to="/markets" className="mt-3 inline-block text-accent hover:underline">Back to markets</Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-4">
      <Link to="/markets" className="inline-flex items-center gap-1 text-fg-muted hover:text-fg">
        <ArrowLeft className="h-3.5 w-3.5" aria-hidden /> Markets
      </Link>

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <div className="flex items-baseline gap-2">
            <h1 className="text-lg font-bold">{symbol}</h1>
            {instrument.data && (
              <span className="text-fg-muted">
                {instrument.data.name} · {instrument.data.exchange} · {instrument.data.sector}
              </span>
            )}
          </div>
          <div className="mt-1 flex items-baseline gap-3">
            <span className="num font-mono text-2xl font-semibold" data-testid="last-price">
              {quote ? `₹${formatPrice(quote.lastPrice)}` : '—'}
            </span>
            {quote && <PriceChange change={quote.change} changePercent={quote.changePercent} />}
          </div>
        </div>
        <DataSourceBadge fallbackSource={source ?? candles.data?.source} fallbackAsOf={quote?.timestamp} />
      </div>

      <div className="grid gap-4 lg:grid-cols-[1fr_280px]">
        <Panel
          title="Chart"
          actions={
            <div role="group" aria-label="Timeframe" className="flex gap-0.5">
              {TIMEFRAMES.map((t) => (
                <button
                  key={t.value}
                  type="button"
                  aria-pressed={t.value === timeframe}
                  onClick={() => setTimeframe(t.value)}
                  className={`rounded-sm px-2 py-0.5 text-[11px] font-semibold ${
                    t.value === timeframe ? 'bg-surface-3 text-fg' : 'text-fg-muted hover:text-fg'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          }
        >
          <div className="h-[420px]">
            {candles.error && !candles.data ? (
              <FormAlert message={describeError(candles.error)} />
            ) : candles.data ? (
              <CandlestickChart
                candles={candles.data.candles}
                bucketSeconds={bucketSeconds}
                livePrice={quote?.lastPrice}
                liveTimestamp={quote?.timestamp}
              />
            ) : (
              <div role="status" className="h-full animate-pulse rounded-sm bg-surface-2">
                <span className="sr-only">Loading chart</span>
              </div>
            )}
          </div>
        </Panel>

        <Panel title="Quote">
          {quote ? (
            <KeyValueList
              items={[
                { label: 'Bid', value: `${formatPrice(quote.bid)} × ${formatVolume(quote.bidSize)}` },
                { label: 'Ask', value: `${formatPrice(quote.ask)} × ${formatVolume(quote.askSize)}` },
                { label: 'Open', value: formatPrice(quote.open) },
                { label: 'High', value: formatPrice(quote.high) },
                { label: 'Low', value: formatPrice(quote.low) },
                { label: 'Prev. close', value: formatPrice(quote.previousClose) },
                { label: 'Volume', value: formatVolume(quote.volume) },
                { label: 'Tick size', value: instrument.data ? formatPrice(instrument.data.tickSize) : '—' },
                { label: 'Lot size', value: instrument.data?.lotSize ?? '—' },
              ]}
            />
          ) : (
            <p className="text-fg-muted">Loading quote…</p>
          )}
        </Panel>
      </div>
    </div>
  );
}
