import { ApiError } from '@/services/apiClient';

/** Human-readable message for a failed form submission. */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.isNetworkError) return 'Cannot reach the server. Check that the backend is running.';
    if (error.code === 'VALIDATION_FAILED' && error.fieldErrors.length > 0) return 'Please correct the highlighted fields.';
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

/** Maps backend field errors ({field, message}) onto form fields. */
export function serverFieldErrors<K extends string>(error: unknown, fields: readonly K[]): Partial<Record<K, string>> {
  if (!(error instanceof ApiError)) return {};
  const result: Partial<Record<K, string>> = {};
  for (const { field, message } of error.fieldErrors) {
    if ((fields as readonly string[]).includes(field)) result[field as K] = message;
  }
  return result;
}
