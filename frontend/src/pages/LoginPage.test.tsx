import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { renderWithProviders } from '@/test/renderWithProviders';
import { authResponse, json, mockFetchRoutes, problem } from '@/test/fetchMock';
import { useAuthStore } from '@/stores/authStore';

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/account" element={<p>Account page</p>} />
    </Routes>,
    { route: '/login' },
  );
}

async function fillAndSubmit(email: string, password: string) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('E-mail'), email);
  await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
}

describe('LoginPage', () => {
  it('signs in, stores the session in memory and navigates to the account page', async () => {
    const fetchMock = mockFetchRoutes({ 'POST /api/v1/auth/login': json(authResponse()) });
    renderLogin();

    await fillAndSubmit('  asha@example.com ', 'Password1');

    expect(await screen.findByText('Account page')).toBeInTheDocument();
    expect(useAuthStore.getState()).toMatchObject({ status: 'authenticated', accessToken: 'access-token-1' });
    expect(JSON.parse(fetchMock.mock.calls[0]![1]!.body as string)).toEqual({
      email: 'asha@example.com',
      password: 'Password1',
    });
    expect(localStorage.length).toBe(0); // tokens are never persisted to storage
  });

  it('shows the server error for wrong credentials', async () => {
    mockFetchRoutes({
      'POST /api/v1/auth/login': problem(401, 'INVALID_CREDENTIALS', 'Invalid e-mail or password'),
    });
    renderLogin();

    await fillAndSubmit('asha@example.com', 'WrongPass1');

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid e-mail or password');
    expect(useAuthStore.getState().status).not.toBe('authenticated');
  });

  it('validates before calling the server', async () => {
    const fetchMock = mockFetchRoutes({});
    renderLogin();

    await userEvent.setup().click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByText('E-mail is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
