import { describe, expect, it } from 'vitest';
import { formatDateTimeIst, orDash } from './format';

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
