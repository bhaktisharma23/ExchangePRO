import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SystemStatusPage } from './SystemStatusPage';
import { jsonResponse, renderWithProviders } from '@/test/renderWithProviders';
import type { SystemStatus } from '@/types/system';

const status: SystemStatus = {
  application: 'EXCHANGE PRO',
  version: '0.1.0-SNAPSHOT',
  activeProfiles: [],
  serverTime: '2026-01-15T09:15:00Z',
  database: { state: 'UP', productVersion: 'PostgreSQL 16.10', schemaVersion: '1' },
  marketData: { mode: 'SIMULATION', label: 'SIMULATED DATA', realTime: false },
};

describe('SystemStatusPage', () => {
  it('shows a loading state, then data from the backend', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(status));
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<SystemStatusPage />);

    expect(screen.getByRole('status')).toHaveTextContent('Loading system status');
    expect(await screen.findByText('PostgreSQL 16.10')).toBeInTheDocument();
    expect(screen.getByText('0.1.0-SNAPSHOT')).toBeInTheDocument();
    expect(screen.getByText('SIMULATED DATA')).toBeInTheDocument();
    expect(screen.getByText(/not NSE\/BSE prices/)).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/system/status', expect.anything());
  });

  it('shows the database as DOWN when the backend reports it', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ ...status, database: { state: 'DOWN' } })),
    );

    renderWithProviders(<SystemStatusPage />);

    expect(await screen.findByText('DOWN')).toBeInTheDocument();
  });

  it('shows an error state when the backend is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    renderWithProviders(<SystemStatusPage />);

    // useSystemStatus retries once (~1 s back-off) before surfacing the error.
    expect(await screen.findByRole('alert', {}, { timeout: 4000 })).toHaveTextContent('Backend unreachable');
  });

  it('refetches when Refresh is clicked', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse(status)));
    vi.stubGlobal('fetch', fetchMock);
    renderWithProviders(<SystemStatusPage />);
    await screen.findByText('PostgreSQL 16.10');

    await userEvent.click(screen.getByRole('button', { name: /refresh/i }));

    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  });
});
