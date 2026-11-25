import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AccountPage } from './AccountPage';
import { renderWithProviders } from '@/test/renderWithProviders';
import { authResponse, json, mockFetchRoutes, testUser } from '@/test/fetchMock';
import { useAuthStore } from '@/stores/authStore';

const account = {
  accountId: 'acc-1',
  currency: 'INR',
  availableBalance: '100000.0000',
  reservedBalance: '0.0000',
  totalBalance: '100000.0000',
};

describe('AccountPage', () => {
  it('shows the virtual balance from the backend in Indian format', async () => {
    useAuthStore.getState().setSession(authResponse());
    mockFetchRoutes({ 'GET /api/v1/accounts/me': json(account) });

    renderWithProviders(<AccountPage />);

    expect(await screen.findByTestId('available-balance')).toHaveTextContent('₹1,00,000.00');
    expect(screen.getByText('asha@example.com')).toBeInTheDocument();
  });

  it('updates the display name and the session user', async () => {
    useAuthStore.getState().setSession(authResponse());
    mockFetchRoutes({
      'GET /api/v1/accounts/me': json(account),
      'PATCH /api/v1/users/me': json({ ...testUser, displayName: 'Asha K' }),
    });
    renderWithProviders(<AccountPage />);
    const user = userEvent.setup();

    const input = screen.getByLabelText('Display name');
    await user.clear(input);
    await user.type(input, 'Asha K');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Saved')).toBeInTheDocument();
    expect(useAuthStore.getState().user?.displayName).toBe('Asha K');
  });

  it('does not request a trading account for administrators', async () => {
    useAuthStore.getState().setSession(authResponse({ user: { ...testUser, role: 'ADMIN' } }));
    const fetchMock = mockFetchRoutes({});

    renderWithProviders(<AccountPage />);

    expect(screen.getByText(/do not hold a trading account/)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
