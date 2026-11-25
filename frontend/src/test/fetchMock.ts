import { vi } from 'vitest';
import type { AuthResponse, UserProfile } from '@/types/auth';

type Handler = (init: RequestInit) => Response | Promise<Response>;

/**
 * Stubs global fetch with a router keyed by "METHOD /path". A list of handlers is consumed in order,
 * which lets a test script e.g. "first 401, then 200". Unmocked requests fail loudly.
 */
export function mockFetchRoutes(routes: Record<string, Handler | Handler[]>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const key = `${init.method ?? 'GET'} ${typeof input === 'string' ? input : input.toString()}`;
    const route = routes[key];
    const handler = Array.isArray(route) ? route.shift() : route;
    if (!handler) throw new Error(`Unmocked request: ${key}`);
    return handler(init);
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export const json = (body: unknown, status = 200) => () =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

export const problem = (status: number, code: string, detail: string, errors?: unknown) =>
  json({ status, code, detail, errors }, status);

export const testUser: UserProfile = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'asha@example.com',
  displayName: 'Asha Trader',
  role: 'USER',
  enabled: true,
  createdAt: '2026-01-15T09:15:00Z',
};

export const authResponse = (overrides: Partial<AuthResponse> = {}): AuthResponse => ({
  accessToken: 'access-token-1',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: testUser,
  ...overrides,
});

export function headerOf(init: RequestInit | undefined, name: string): string | undefined {
  return (init?.headers as Record<string, string> | undefined)?.[name];
}
