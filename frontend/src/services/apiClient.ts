import type { ProblemDetail } from '@/types/api';
import type { AuthResponse } from '@/types/auth';
import { useAuthStore } from '@/stores/authStore';

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '';
const REFRESH_PATH = '/api/v1/auth/refresh';

/**
 * Error thrown for any failed API call. `status` is 0 when the server could not be reached at all
 * (network error, backend stopped), which lets the UI distinguish "backend down" from "request rejected".
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly correlationId?: string;
  readonly fieldErrors: { field: string; message: string }[];

  constructor(message: string, status: number, problem?: Partial<ProblemDetail>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = problem?.code;
    this.correlationId = problem?.correlationId;
    this.fieldErrors = problem?.errors ?? [];
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  signal?: AbortSignal;
  /** Attach the access token and transparently refresh it on 401. Default true. */
  authenticated?: boolean;
}

/**
 * Performs a JSON request.
 *
 * When an authenticated request returns 401 (access token expired), the client refreshes the session once
 * and retries the request. If the refresh fails, the session is cleared and the error is re-thrown, which
 * sends protected routes back to the login page.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const authenticated = options.authenticated ?? true;
  try {
    return await send<T>(path, options, authenticated);
  } catch (error) {
    if (authenticated && error instanceof ApiError && error.status === 401 && (await refreshSession())) {
      return send<T>(path, options, true);
    }
    throw error;
  }
}

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Exchanges the HttpOnly refresh cookie for a new access token.
 *
 * Refresh tokens are single-use (rotated on every refresh, and reuse logs the user out everywhere), so
 * concurrent refreshes must never send the same cookie twice:
 * - within a tab, simultaneous callers share one in-flight promise ("single flight");
 * - across tabs, the Web Locks API serialises refreshes, so the second tab runs after the first has stored
 *   the rotated cookie and therefore sends the new one.
 *
 * Resolves to true on success, false if the user must sign in again.
 */
export function refreshSession(): Promise<boolean> {
  refreshInFlight ??= withCrossTabLock(async () => {
    try {
      const auth = await send<AuthResponse>(REFRESH_PATH, { method: 'POST' }, false);
      useAuthStore.getState().setSession(auth);
      return true;
    } catch {
      useAuthStore.getState().clearSession();
      return false;
    }
  }).finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

function withCrossTabLock<T>(task: () => Promise<T>): Promise<T> {
  const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
  return locks ? (locks.request('exchangepro-session-refresh', task) as Promise<T>) : task();
}

async function send<T>(path: string, options: RequestOptions, authenticated: boolean): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  const token = useAuthStore.getState().accessToken;
  if (authenticated && token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      credentials: 'include', // send the refresh cookie to /api/v1/auth/* (also cross-origin in production)
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError('Cannot reach the EXCHANGE PRO server', 0);
  }

  if (!response.ok) {
    const problem = await readProblem(response);
    throw new ApiError(
      problem?.detail ?? `Request failed with status ${response.status}`,
      response.status,
      { ...problem, correlationId: problem?.correlationId ?? response.headers.get('X-Correlation-Id') ?? undefined },
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function readProblem(response: Response): Promise<Partial<ProblemDetail> | undefined> {
  try {
    return (await response.json()) as Partial<ProblemDetail>;
  } catch {
    return undefined;
  }
}
