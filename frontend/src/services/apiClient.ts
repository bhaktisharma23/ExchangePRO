import type { ProblemDetail } from '@/types/api';

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '';

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

/** Performs a JSON request and converts every failure into an {@link ApiError}. */
export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...init.headers },
    });
  } catch {
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

  return (await response.json()) as T;
}

async function readProblem(response: Response): Promise<Partial<ProblemDetail> | undefined> {
  try {
    return (await response.json()) as Partial<ProblemDetail>;
  } catch {
    return undefined;
  }
}
