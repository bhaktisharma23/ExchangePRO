const dateTimeFormatter = new Intl.DateTimeFormat('en-IN', {
  dateStyle: 'medium',
  timeStyle: 'medium',
  timeZone: 'Asia/Kolkata',
});

/** Formats an ISO-8601 instant in Indian Standard Time, e.g. "15 Jan 2026, 2:45:00 pm IST". */
export function formatDateTimeIst(iso: string | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return `${dateTimeFormatter.format(date)} IST`;
}

/** Returns the value or an em dash for missing values, so tables never show "undefined". */
export function orDash(value: string | number | null | undefined): string {
  return value === null || value === undefined || value === '' ? '—' : String(value);
}

const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/**
 * Formats a decimal string from the API in Indian grouping: "100000.0000" -> "₹1,00,000.00".
 * The string is passed straight to Intl (which accepts exact decimal strings), so no binary floating-point
 * conversion happens.
 */
export function formatInr(amount: string | undefined): string {
  if (amount === undefined || amount === '' || Number.isNaN(Number(amount))) return '—';
  return inrFormatter.format(amount as `${number}`);
}

const priceFormatter = new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const integerFormatter = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 });
const timeFormatter = new Intl.DateTimeFormat('en-IN', {
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
  timeZone: 'Asia/Kolkata',
});

/** "1400.0500" -> "1,400.05" (exact: the string is formatted without float conversion). */
export function formatPrice(value: string | undefined): string {
  if (value === undefined || value === '' || Number.isNaN(Number(value))) return '—';
  return priceFormatter.format(value as `${number}`);
}

/** Signed change: "+12.35" / "-3.10" / "0.00". */
export function formatSignedPrice(value: string | undefined): string {
  const formatted = formatPrice(value);
  if (formatted === '—') return formatted;
  return Number(value) > 0 ? `+${formatted}` : formatted;
}

/** Signed percentage: "+1.25%". */
export function formatPercent(value: string | undefined): string {
  if (value === undefined || Number.isNaN(Number(value))) return '—';
  const n = Number(value);
  return `${n > 0 ? '+' : ''}${priceFormatter.format(value as `${number}`)}%`;
}

/** Volume with Indian digit grouping: 12345678 -> "1,23,45,678". */
export function formatVolume(value: number | undefined): string {
  return value === undefined ? '—' : integerFormatter.format(value);
}

/** "14:45:03 IST" */
export function formatTimeIst(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : `${timeFormatter.format(d)} IST`;
}
