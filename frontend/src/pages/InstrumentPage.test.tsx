import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { InstrumentPage } from './InstrumentPage';
import { renderWithProviders } from '@/test/renderWithProviders';
import { json, mockFetchRoutes, problem } from '@/test/fetchMock';
import { quote, simulatedSource } from '@/test/marketFixtures';

// The chart library draws on <canvas>, which jsdom does not implement.
vi.mock('@/charts/CandlestickChart', () => ({
  CandlestickChart: ({ candles }: { candles: unknown[] }) => <div data-testid="chart">{candles.length} candles</div>,
}));

const instrument = { symbol: 'M&M', name: 'Mahindra & Mahindra Ltd', exchange: 'NSE', sector: 'Automobile', currency: 'INR', tickSize: '0.0500', lotSize: 1 };
const candles = (n: number) => ({
  symbol: 'M&M', timeframe: '5m', source: simulatedSource,
  candles: Array.from({ length: n }, (_, i) => ({ openTime: `2026-01-15T09:${String(i).padStart(2, '0')}:00Z`, open: '1', high: '2', low: '1', close: '2', volume: 1 })),
});

function renderAt(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/instruments/:symbol" element={<InstrumentPage />} />
    </Routes>,
    { route: path },
  );
}

describe('InstrumentPage', () => {
  it('shows instrument details, live quote and chart (symbol with & is URL-encoded)', async () => {
    const fetchMock = mockFetchRoutes({
      'GET /api/v1/instruments/M%26M': json(instrument),
      'GET /api/v1/market/quotes/M%26M': json({ source: simulatedSource, quote: quote('M&M', { lastPrice: '3600.0000', change: '36.0000', changePercent: '1.01' }) }),
      'GET /api/v1/market/candles/M%26M?timeframe=5m&limit=300': json(candles(3)),
    });

    renderAt('/instruments/M%26M');

    expect(await screen.findByText(/Mahindra & Mahindra Ltd · NSE · Automobile/)).toBeInTheDocument();
    expect(await screen.findByTestId('last-price')).toHaveTextContent('₹3,600.00');
    expect(await screen.findByText('3 candles')).toBeInTheDocument();
    expect(screen.getByText('SIMULATED DATA')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalled();
  });

  it('loads new candles when the timeframe changes', async () => {
    mockFetchRoutes({
      'GET /api/v1/instruments/M%26M': json(instrument),
      'GET /api/v1/market/quotes/M%26M': json({ source: simulatedSource, quote: quote('M&M') }),
      'GET /api/v1/market/candles/M%26M?timeframe=5m&limit=300': json(candles(3)),
      'GET /api/v1/market/candles/M%26M?timeframe=1d&limit=300': json({ ...candles(7), timeframe: '1d' }),
    });
    renderAt('/instruments/M%26M');
    await screen.findByText('3 candles');

    await userEvent.setup().click(screen.getByRole('button', { name: '1D' }));

    expect(await screen.findByText('7 candles')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '1D' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('shows a not-found message for unknown symbols', async () => {
    mockFetchRoutes({
      'GET /api/v1/instruments/NOPE': problem(404, 'RESOURCE_NOT_FOUND', 'Instrument not found: NOPE'),
      'GET /api/v1/market/quotes/NOPE': problem(404, 'RESOURCE_NOT_FOUND', 'Instrument not found: NOPE'),
      'GET /api/v1/market/candles/NOPE?timeframe=5m&limit=300': problem(404, 'RESOURCE_NOT_FOUND', 'Instrument not found: NOPE'),
    });

    renderAt('/instruments/NOPE');

    expect(await screen.findByText('Instrument NOPE not found')).toBeInTheDocument();
  });
});
