import { describe, expect, it } from 'vitest';
import { breadth, direction, filterQuotes, movers } from './market';
import { quote } from '@/test/marketFixtures';

const quotes = [
  quote('AAA', { change: '5.00', changePercent: '5.00', volume: 10 }),
  quote('BBB', { change: '-2.00', changePercent: '-2.00', volume: 500, name: 'Bharat Bank' }),
  quote('CCC', { change: '0.0000', changePercent: '0.00', volume: 50 }),
  quote('DDD', { change: '1.00', changePercent: '1.00', volume: 5 }),
];

describe('market utils', () => {
  it('counts advances, declines and unchanged', () => {
    expect(breadth(quotes)).toEqual({ advances: 2, declines: 1, unchanged: 1 });
  });

  it('ranks gainers, losers and most active', () => {
    const top = movers(quotes, 2);
    expect(top.gainers.map((q) => q.symbol)).toEqual(['AAA', 'DDD']);
    expect(top.losers.map((q) => q.symbol)).toEqual(['BBB', 'CCC']);
    expect(top.mostActive.map((q) => q.symbol)).toEqual(['BBB', 'CCC']);
  });

  it('filters by symbol or name, case-insensitively', () => {
    expect(filterQuotes(quotes, 'bank').map((q) => q.symbol)).toEqual(['BBB']);
    expect(filterQuotes(quotes, 'aa').map((q) => q.symbol)).toEqual(['AAA']);
    expect(filterQuotes(quotes, '  ')).toHaveLength(4);
  });

  it('derives price direction from decimal strings', () => {
    expect(direction('1.5')).toBe(1);
    expect(direction('-0.05')).toBe(-1);
    expect(direction('0.0000')).toBe(0);
    expect(direction(undefined)).toBe(0);
  });
});
