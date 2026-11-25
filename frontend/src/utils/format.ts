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
