import { describe, expect, it } from 'vitest';
import { compactErrors, validateDisplayName, validateEmail, validatePassword } from './validation';

describe('validateEmail', () => {
  it('accepts normal addresses, trimming whitespace', () => {
    expect(validateEmail(' asha@example.com ')).toBeUndefined();
  });
  it('rejects empty and malformed addresses', () => {
    expect(validateEmail('')).toBe('E-mail is required');
    expect(validateEmail('asha@')).toBe('Enter a valid e-mail address');
    expect(validateEmail('a b@c.com')).toBe('Enter a valid e-mail address');
  });
});

describe('validatePassword', () => {
  it('requires 8+ characters with a letter and a digit', () => {
    expect(validatePassword('Password1')).toBeUndefined();
    expect(validatePassword('short1')).toBe('Use at least 8 characters');
    expect(validatePassword('onlyletters')).toBe('Use at least one letter and one digit');
    expect(validatePassword('12345678')).toBe('Use at least one letter and one digit');
  });
  it('rejects passwords longer than 72 UTF-8 bytes (BCrypt limit)', () => {
    expect(validatePassword('ä1'.repeat(30))).toBe('Password is too long'); // 60 chars, 90 bytes
  });
});

describe('validateDisplayName', () => {
  it('allows letters (any script), digits, spaces and . _ \' -', () => {
    expect(validateDisplayName("Asha O'Neil-2")).toBeUndefined();
    expect(validateDisplayName('आशा')).toBeUndefined();
  });
  it('rejects markup and bad lengths', () => {
    expect(validateDisplayName('<b>x</b>')).toMatch(/Use letters/);
    expect(validateDisplayName('A')).toBe('Use 2 to 40 characters');
  });
});

describe('compactErrors', () => {
  it('drops fields without errors', () => {
    expect(compactErrors({ a: undefined, b: 'bad' })).toEqual({ b: 'bad' });
  });
});
