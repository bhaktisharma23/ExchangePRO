import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { RegisterPage } from './RegisterPage';
import { renderWithProviders } from '@/test/renderWithProviders';
import { authResponse, json, mockFetchRoutes, problem } from '@/test/fetchMock';

function renderRegister() {
  return renderWithProviders(
    <Routes>
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/markets" element={<p>Markets page</p>} />
    </Routes>,
    { route: '/register' },
  );
}

async function fill(displayName: string, email: string, password: string) {
  const user = userEvent.setup();
  if (displayName) await user.type(screen.getByLabelText('Display name'), displayName);
  if (email) await user.type(screen.getByLabelText('E-mail'), email);
  if (password) await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Create account' }));
}

describe('RegisterPage', () => {
  it('registers and navigates to the markets page', async () => {
    mockFetchRoutes({ 'POST /api/v1/auth/register': json(authResponse(), 201) });
    renderRegister();

    await fill('Asha Trader', 'asha@example.com', 'Password1');

    expect(await screen.findByText('Markets page')).toBeInTheDocument();
  });

  it('shows client-side validation errors without calling the server', async () => {
    const fetchMock = mockFetchRoutes({});
    renderRegister();

    await fill('A', 'not-an-email', 'weak');

    expect(screen.getByText('Use 2 to 40 characters')).toBeInTheDocument();
    expect(screen.getByText('Enter a valid e-mail address')).toBeInTheDocument();
    expect(screen.getByText('Use at least 8 characters')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('shows a duplicate e-mail error from the server', async () => {
    mockFetchRoutes({
      'POST /api/v1/auth/register': problem(409, 'EMAIL_ALREADY_REGISTERED', 'An account with this e-mail address already exists'),
    });
    renderRegister();

    await fill('Asha Trader', 'asha@example.com', 'Password1');

    expect(await screen.findByRole('alert')).toHaveTextContent('already exists');
  });

  it('maps server field errors onto the form', async () => {
    mockFetchRoutes({
      'POST /api/v1/auth/register': problem(400, 'VALIDATION_FAILED', 'Request validation failed', [
        { field: 'email', message: 'must be a well-formed email address' },
      ]),
    });
    renderRegister();

    await fill('Asha Trader', 'asha@example.com', 'Password1');

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument();
  });
});
