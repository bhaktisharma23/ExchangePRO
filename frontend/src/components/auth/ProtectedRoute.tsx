import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import type { Role } from '@/types/auth';

interface ProtectedRouteProps {
  children: ReactNode;
  /** If set, only users with this role may see the page. The backend enforces the same rule. */
  requiredRole?: Role;
}

/**
 * Hides pages from users who are not signed in. This is a UX guard only: real protection is on the backend,
 * which rejects every API call without a valid token or with the wrong role.
 */
export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const status = useAuthStore((s) => s.status);
  const role = useAuthStore((s) => s.user?.role);
  const location = useLocation();

  if (status === 'initializing') {
    return (
      <div role="status" className="flex h-40 items-center justify-center text-fg-muted">
        Restoring session…
      </div>
    );
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (requiredRole && role !== requiredRole) {
    return (
      <div role="alert" className="mx-auto mt-16 flex max-w-md items-start gap-3 rounded-sm border border-line bg-surface p-4">
        <ShieldAlert className="h-5 w-5 shrink-0 text-down" aria-hidden />
        <div>
          <p className="font-semibold">Access denied</p>
          <p className="text-fg-muted">This page requires the {requiredRole} role.</p>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}

/** Sends already-signed-in users away from the login/register pages. */
export function GuestOnlyRoute({ children }: { children: ReactNode }) {
  const status = useAuthStore((s) => s.status);
  if (status === 'authenticated') return <Navigate to="/markets" replace />;
  return <>{children}</>;
}
