import { describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest } from './apiClient';
import { jsonResponse } from '@/test/renderWithProviders';

describe('apiRequest', () => {
  it('returns parsed JSON on success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ ok: true })));

    await expect(apiRequest('/api/x')).resolves.toEqual({ ok: true });
  });

  it('converts a Problem Details response into an ApiError', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            status: 400,
            detail: 'Request validation failed',
            code: 'VALIDATION_FAILED',
            correlationId: 'abc-123-def',
            errors: [{ field: 'quantity', message: 'must be greater than 0' }],
          },
          400,
        ),
      ),
    );

    const error = await apiRequest('/api/x').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.code).toBe('VALIDATION_FAILED');
    expect(apiError.correlationId).toBe('abc-123-def');
    expect(apiError.fieldErrors).toHaveLength(1);
    expect(apiError.isNetworkError).toBe(false);
  });

  it('falls back to the correlation header when the body is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('Bad gateway', { status: 502, headers: { 'X-Correlation-Id': 'hdr-12345678' } })),
    );

    const error = (await apiRequest('/api/x').catch((e: unknown) => e)) as ApiError;

    expect(error.status).toBe(502);
    expect(error.correlationId).toBe('hdr-12345678');
  });

  it('reports network failures with status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    const error = (await apiRequest('/api/x').catch((e: unknown) => e)) as ApiError;

    expect(error.isNetworkError).toBe(true);
    expect(error.message).toBe('Cannot reach the EXCHANGE PRO server');
  });
});
