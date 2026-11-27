import { useEffect, useId, useState, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useInstrumentSearch } from '@/hooks/useMarketData';

/** Debounced instrument search (backend /api/v1/instruments) with keyboard navigation. */
export function InstrumentSearch() {
  const navigate = useNavigate();
  const listId = useId();
  const [text, setText] = useState('');
  const [debounced, setDebounced] = useState('');
  const [active, setActive] = useState(0);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(text), 200);
    return () => clearTimeout(timer);
  }, [text]);

  const { data, isFetching } = useInstrumentSearch(debounced);
  const results = data?.content ?? [];

  function go(symbol: string) {
    setText('');
    setOpen(false);
    navigate(`/instruments/${encodeURIComponent(symbol)}`);
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((i) => Math.min(i + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter' && results[active]) {
      go(results[active].symbol);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  }

  const showList = open && debounced.trim().length > 0;
  return (
    <div className="relative w-56">
      <Search className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-fg-muted" aria-hidden />
      <input
        role="combobox"
        aria-label="Search instruments"
        aria-expanded={showList}
        aria-controls={listId}
        placeholder="Search symbol or company"
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setActive(0);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onKeyDown={onKeyDown}
        className="h-7 w-full rounded-sm border border-line bg-surface-2 pl-7 pr-2 text-fg outline-none placeholder:text-fg-muted/60 focus:border-accent"
      />
      {showList && (
        <ul id={listId} role="listbox" className="absolute z-20 mt-1 w-80 rounded-sm border border-line bg-surface-3 py-1 shadow-lg">
          {results.length === 0 && (
            <li className="px-3 py-2 text-fg-muted">{isFetching ? 'Searching…' : 'No matches'}</li>
          )}
          {results.map((r, i) => (
            <li
              key={r.symbol}
              role="option"
              aria-selected={i === active}
              onMouseDown={(e) => {
                e.preventDefault();
                go(r.symbol);
              }}
              className={`flex cursor-pointer justify-between gap-3 px-3 py-1.5 ${i === active ? 'bg-surface-2' : ''}`}
            >
              <span className="font-semibold">{r.symbol}</span>
              <span className="truncate text-fg-muted">{r.name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
