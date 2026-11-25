import { describe, expect, it } from 'vitest';
import { formatDateTimeIst, formatInr, orDash } from './format';

describe('formatDateTimeIst', () => {
  it('formats UTC instants in Indian Standard Time', () => {
    // 09:15 UTC is 14:45 IST
    expect(formatDateTimeIst('2026-01-15T09:15:00Z')).toMatch(/15 Jan 2026.*2:45:00\s?pm IST/i);
  });

  it('returns a dash for missing or invalid values', () => {
    expect(formatDateTimeIst(undefined)).toBe('—');
    expect(formatDateTimeIst('not-a-date')).toBe('—');
  });
});

describe('orDash', () => {
  it('replaces empty values with a dash', () => {
    expect(orDash(undefined)).toBe('—');
    expect(orDash(null)).toBe('—');
    expect(orDash('')).toBe('—');
    expect(orDash(0)).toBe('0');
    expect(orDash('1')).toBe('1');
  });
});

describe('formatInr', () => {
  it('uses Indian digit grouping and two decimals', () => {
    expect(formatInr('100000.0000')).toBe('₹1,00,000.00');
    expect(formatInr('12345678.5')).toBe('₹1,23,45,678.50');
    expect(formatInr('0.0000')).toBe('₹0.00');
  });

  it('keeps exact decimal values beyond float precision', () => {
    // 2^53 + 1 cannot be represented as a JS number, but the string path keeps it exact.
    expect(formatInr('9007199254740993.00')).toBe('₹9,00,71,99,25,47,40,993.00');
  });

  it('returns a dash for missing or invalid values', () => {
    expect(formatInr(undefined)).toBe('—');
    expect(formatInr('abc')).toBe('—');
  });
});
