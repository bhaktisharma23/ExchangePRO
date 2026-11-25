/**
 * Client-side form validation. Mirrors the backend rules so users get instant feedback, but the backend
 * remains the authority: it validates every request again.
 */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const LETTER = /\p{L}/u;
const DIGIT = /\p{N}/u;
// \p{M} = combining marks, required for Indic scripts (Devanagari vowel signs are marks, not letters).
const DISPLAY_NAME = /^[\p{L}\p{M}\p{N} ._'-]+$/u;

export function validateEmail(value: string): string | undefined {
  const email = value.trim();
  if (!email) return 'E-mail is required';
  if (email.length > 254 || !EMAIL.test(email)) return 'Enter a valid e-mail address';
  return undefined;
}

export function validatePassword(value: string): string | undefined {
  if (!value) return 'Password is required';
  if (value.length < 8) return 'Use at least 8 characters';
  if (new TextEncoder().encode(value).length > 72) return 'Password is too long';
  if (!LETTER.test(value) || !DIGIT.test(value)) return 'Use at least one letter and one digit';
  return undefined;
}

export function validateDisplayName(value: string): string | undefined {
  const name = value.trim();
  if (!name) return 'Display name is required';
  if (name.length < 2 || name.length > 40) return 'Use 2 to 40 characters';
  if (!DISPLAY_NAME.test(name)) return "Use letters, digits, spaces and . _ ' - only";
  return undefined;
}

export type FieldErrors<K extends string> = Partial<Record<K, string>>;

/** Drops undefined entries so `Object.keys(errors).length === 0` means "valid". */
export function compactErrors<K extends string>(errors: Record<K, string | undefined>): FieldErrors<K> {
  return Object.fromEntries(Object.entries(errors).filter(([, v]) => v !== undefined)) as FieldErrors<K>;
}
