import { useEffect } from 'react';
import { authApi } from '@/services/authApi';

let bootstrap: Promise<boolean> | null = null;

/**
 * On page load, try to restore the session from the HttpOnly refresh cookie (the access token lived only in
 * memory and is gone after a reload). Runs once per page load, even though React StrictMode mounts effects
 * twice in development.
 */
export function useSessionBootstrap() {
  useEffect(() => {
    bootstrap ??= authApi.restoreSession();
  }, []);
}

/** Test helper: allow the bootstrap to run again. */
export function resetSessionBootstrapForTests() {
  bootstrap = null;
}
