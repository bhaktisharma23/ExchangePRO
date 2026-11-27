import type { Quote } from '@/types/market';

/** Market breadth: how many instruments are up, down or flat versus their previous close. */
export function breadth(quotes: Quote[]) {
  let advances = 0;
  let declines = 0;
  for (const q of quotes) {
    const change = Number(q.change);
    if (change > 0) advances++;
    else if (change < 0) declines++;
  }
  return { advances, declines, unchanged: quotes.length - advances - declines };
}

/** Top movers by percentage change and by volume (ranking only; values are not modified). */
export function movers(quotes: Quote[], n = 5) {
  const byChange = [...quotes].sort((a, b) => Number(b.changePercent) - Number(a.changePercent));
  return {
    gainers: byChange.slice(0, n),
    losers: byChange.slice(-n).reverse(),
    mostActive: [...quotes].sort((a, b) => b.volume - a.volume).slice(0, n),
  };
}

/** Case-insensitive filter on symbol or company name. */
export function filterQuotes(quotes: Quote[], text: string): Quote[] {
  const q = text.trim().toLowerCase();
  if (!q) return quotes;
  return quotes.filter((x) => x.symbol.toLowerCase().includes(q) || x.name.toLowerCase().includes(q));
}

/** -1, 0 or 1 from a decimal string, for choosing red/green. */
export function direction(value: string | undefined): -1 | 0 | 1 {
  const n = Number(value);
  if (!value || Number.isNaN(n) || n === 0) return 0;
  return n > 0 ? 1 : -1;
}
