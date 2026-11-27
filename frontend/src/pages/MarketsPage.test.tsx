import { describe, expect, it } from 'vitest';
import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MarketsPage } from './MarketsPage';
import { renderWithProviders } from '@/test/renderWithProviders';
import { json, mockFetchRoutes } from '@/test/fetchMock';
import { overview, quote, simulatedSource } from '@/test/marketFixtures';
import { useMarketStore } from '@/stores/marketStore';

const quotes = [
  quote('RELIANCE', { name: 'Reliance Industries Ltd', lastPrice: '1400.0000', change: '14.0000', changePercent: '1.01' }),
  quote('TCS', { name: 'Tata Consultancy Services Ltd', lastPrice: '3100.0000', change: '-31.0000', changePercent: '-0.99' }),
];

describe('MarketsPage', () => {
  it('shows backend quotes with the SIMULATED DATA label', async () => {
    mockFetchRoutes({ 'GET /api/v1/market/overview': json(overview(quotes)) });

    renderWithProviders(<MarketsPage />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('RELIANCE')).toBeInTheDocument();
    expect(within(table).getByText('1,400.00')).toBeInTheDocument();
    expect(within(table).getByText('+14.00 (+1.01%)')).toBeInTheDocument();
    expect(screen.getByText('SIMULATED DATA')).toBeInTheDocument();
    expect(screen.getByText(/Updated .* IST/)).toBeInTheDocument();
  });

  it('updates prices live when a WebSocket message arrives', async () => {
    mockFetchRoutes({ 'GET /api/v1/market/overview': json(overview(quotes)) });
    renderWithProviders(<MarketsPage />);
    await screen.findByRole('table');

    act(() => {
      useMarketStore.getState().applyUpdate({
        type: 'QUOTES',
        version: 1,
        source: simulatedSource,
        asOf: '2026-01-15T09:15:01Z',
        quotes: [quote('RELIANCE', { name: 'Reliance Industries Ltd', lastPrice: '1412.3500', change: '26.3500', changePercent: '1.90' })],
      });
    });

    expect(within(screen.getByRole('table')).getByText('1,412.35')).toBeInTheDocument();
  });

  it('filters the table by symbol or company name', async () => {
    mockFetchRoutes({ 'GET /api/v1/market/overview': json(overview(quotes)) });
    renderWithProviders(<MarketsPage />);
    const table = await screen.findByRole('table');

    await userEvent.setup().type(screen.getByLabelText('Filter instruments'), 'tata');

    expect(within(table).queryByText('RELIANCE')).not.toBeInTheDocument();
    expect(within(table).getByText('TCS')).toBeInTheDocument();
  });
});
