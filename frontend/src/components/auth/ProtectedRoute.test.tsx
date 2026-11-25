import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuthStore } from '@/stores/authStore';
import { authResponse, testUser } from '@/test/fetchMock';

function renderAt(element: React.ReactNode) {
  return render(
    <MemoryRouter initialEntries={['/private']}>
      <Routes>
        <Route path="/private" element={element} />
        <Route path="/login" element={<p>Login page</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  it('waits while the session is being restored', () => {
    renderAt(<ProtectedRoute><p>Secret</p></ProtectedRoute>);

    expect(screen.getByRole('status')).toHaveTextContent('Restoring session');
    expect(screen.queryByText('Secret')).not.toBeInTheDocument();
  });

  it('redirects anonymous users to the login page', () => {
    useAuthStore.getState().clearSession();

    renderAt(<ProtectedRoute><p>Secret</p></ProtectedRoute>);

    expect(screen.getByText('Login page')).toBeInTheDocument();
  });

  it('renders the page for signed-in users', () => {
    useAuthStore.getState().setSession(authResponse());

    renderAt(<ProtectedRoute><p>Secret</p></ProtectedRoute>);

    expect(screen.getByText('Secret')).toBeInTheDocument();
  });

  it('blocks users without the required role', () => {
    useAuthStore.getState().setSession(authResponse({ user: { ...testUser, role: 'USER' } }));

    renderAt(<ProtectedRoute requiredRole="ADMIN"><p>Admin area</p></ProtectedRoute>);

    expect(screen.getByRole('alert')).toHaveTextContent('Access denied');
    expect(screen.queryByText('Admin area')).not.toBeInTheDocument();
  });
});
