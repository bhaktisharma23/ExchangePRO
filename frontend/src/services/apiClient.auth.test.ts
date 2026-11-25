import { describe, expect, it } from 'vitest';
import { apiRequest, ApiError } from './apiClient';
import { useAuthStore } from '@/stores/authStore';
import { authResponse, headerOf, json, mockFetchRoutes, problem } from '@/test/fetchMock';

const signIn = () => useAuthStore.getState().setSession(authResponse({ accessToken: 'expired-token' }));

describe('apiRequest authentication', () => {
  it('sends the access token as a Bearer header', async () => {
    signIn();
    const fetchMock = mockFetchRoutes({ 'GET /api/v1/accounts/me': json({ ok: true }) });

    await apiRequest('/api/v1/accounts/me');

    expect(headerOf(fetchMock.mock.calls[0]![1], 'Authorization')).toBe('Bearer expired-token');
  });

  it('does not send the token to unauthenticated endpoints', async () => {
    signIn();
    const fetchMock = mockFetchRoutes({ 'GET /api/v1/system/status': json({}) });

    await apiRequest('/api/v1/system/status', { authenticated: false });

    expect(headerOf(fetchMock.mock.calls[0]![1], 'Authorization')).toBeUndefined();
  });

  it('refreshes once on 401 and retries with the new token', async () => {
    signIn();
    const fetchMock = mockFetchRoutes({
      'GET /api/v1/accounts/me': [problem(401, 'UNAUTHORIZED', 'expired'), json({ accountId: 'a1' })],
      'POST /api/v1/auth/refresh': json(authResponse({ accessToken: 'fresh-token' })),
    });

    await expect(apiRequest('/api/v1/accounts/me')).resolves.toEqual({ accountId: 'a1' });

    expect(headerOf(fetchMock.mock.calls[2]![1], 'Authorization')).toBe('Bearer fresh-token');
    expect(useAuthStore.getState().accessToken).toBe('fresh-token');
  });

  it('clears the session when the refresh fails', async () => {
    signIn();
    mockFetchRoutes({
      'GET /api/v1/accounts/me': problem(401, 'UNAUTHORIZED', 'expired'),
      'POST /api/v1/auth/refresh': problem(401, 'INVALID_REFRESH_TOKEN', 'Session expired'),
    });

    const error = await apiRequest('/api/v1/accounts/me').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(401);
    expect(useAuthStore.getState().status).toBe('anonymous');
    expect(useAuthStore.getState().accessToken).toBeNull();
  });

  it('uses a single refresh for concurrent 401s (refresh tokens are single-use)', async () => {
    signIn();
    const fetchMock = mockFetchRoutes({
      'GET /api/v1/a': [problem(401, 'UNAUTHORIZED', 'x'), json({ a: 1 })],
      'GET /api/v1/b': [problem(401, 'UNAUTHORIZED', 'x'), json({ b: 2 })],
      'POST /api/v1/auth/refresh': json(authResponse({ accessToken: 'fresh-token' })),
    });

    await Promise.all([apiRequest('/api/v1/a'), apiRequest('/api/v1/b')]);

    const refreshCalls = fetchMock.mock.calls.filter(([url]) => url === '/api/v1/auth/refresh');
    expect(refreshCalls).toHaveLength(1);
  });

  it('does not try to refresh after a failed login', async () => {
    const fetchMock = mockFetchRoutes({
      'POST /api/v1/auth/login': problem(401, 'INVALID_CREDENTIALS', 'Invalid e-mail or password'),
    });

    await expect(
      apiRequest('/api/v1/auth/login', { method: 'POST', body: {}, authenticated: false }),
    ).rejects.toMatchObject({ code: 'INVALID_CREDENTIALS' });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
