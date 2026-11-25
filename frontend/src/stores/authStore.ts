import { create } from 'zustand';
import type { AuthResponse, UserProfile } from '@/types/auth';

/**
 * - `initializing`: page just loaded; we are trying to restore the session from the refresh cookie.
 * - `authenticated`: we hold a valid access token.
 * - `anonymous`: no session.
 */
export type AuthStatus = 'initializing' | 'authenticated' | 'anonymous';

interface AuthState {
  status: AuthStatus;
  /**
   * Kept in memory only, never in localStorage/sessionStorage, so injected scripts cannot read it from storage
   * and it disappears when the tab closes. A reload restores the session from the HttpOnly refresh cookie.
   */
  accessToken: string | null;
  user: UserProfile | null;
  setSession: (auth: AuthResponse) => void;
  setUser: (user: UserProfile) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  status: 'initializing',
  accessToken: null,
  user: null,
  setSession: (auth) => set({ status: 'authenticated', accessToken: auth.accessToken, user: auth.user }),
  setUser: (user) => set({ user }),
  clearSession: () => set({ status: 'anonymous', accessToken: null, user: null }),
}));
