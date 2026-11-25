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
