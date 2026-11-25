import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/auth';
import { useAuthStore } from '@/stores/authStore';
import { apiRequest, refreshSession } from './apiClient';

export const authApi = {
  async login(request: LoginRequest): Promise<AuthResponse> {
    const auth = await apiRequest<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: request,
      authenticated: false,
    });
    useAuthStore.getState().setSession(auth);
    return auth;
  },

  async register(request: RegisterRequest): Promise<AuthResponse> {
    const auth = await apiRequest<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: request,
      authenticated: false,
    });
    useAuthStore.getState().setSession(auth);
    return auth;
  },

  /** Revokes the session server-side. The local session is cleared even if the server is unreachable. */
  async logout(): Promise<void> {
    try {
      await apiRequest<void>('/api/v1/auth/logout', { method: 'POST', authenticated: false });
    } finally {
      useAuthStore.getState().clearSession();
    }
  },

  restoreSession: refreshSession,
};
