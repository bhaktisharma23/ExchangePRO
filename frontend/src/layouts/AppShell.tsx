import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Activity, LogOut } from 'lucide-react';
import { MarketDataBadge } from '@/components/MarketDataBadge';
import { useSystemStatus } from '@/hooks/useSystemStatus';
import { useLogout } from '@/hooks/useAuthActions';
import { useAuthStore } from '@/stores/authStore';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `px-2.5 py-1 rounded-sm ${isActive ? 'bg-surface-3 text-fg' : 'text-fg-muted hover:text-fg'}`;

/** Top-level layout: brand bar with navigation and session controls, routed content, disclaimer footer. */
export function AppShell() {
  const { data } = useSystemStatus();

  return (
    <div className="flex min-h-full flex-col">
      <header className="flex h-11 shrink-0 items-center gap-4 border-b border-line bg-surface px-4">
        <Link to="/" className="flex items-center gap-2">
          <Activity className="h-4 w-4 text-accent" aria-hidden />
          <span className="text-sm font-bold tracking-[0.18em]">EXCHANGE PRO</span>
        </Link>
        <nav aria-label="Main" className="flex items-center gap-1">
          <SessionNav />
          <NavLink to="/system" className={navLinkClass}>
            System
          </NavLink>
        </nav>
        <div className="ml-auto flex items-center gap-3">
          <MarketDataBadge marketData={data?.marketData} />
          <SessionControls />
        </div>
      </header>

      <main className="flex-1 p-4">
        <Outlet />
      </main>

      <footer className="border-t border-line px-4 py-2 text-[11px] text-fg-muted">
        Trading simulator. All funds are virtual; no real securities are bought or sold.
      </footer>
    </div>
  );
}

function SessionNav() {
  const status = useAuthStore((s) => s.status);
  if (status !== 'authenticated') return null;
  return (
    <NavLink to="/account" className={navLinkClass}>
      Account
    </NavLink>
  );
}

function SessionControls() {
  const status = useAuthStore((s) => s.status);
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();
  const navigate = useNavigate();

  if (status === 'initializing') return null;
  if (status === 'anonymous' || !user) {
    return (
      <div className="flex items-center gap-2">
        <Link to="/login" className="px-2 text-fg-muted hover:text-fg">
          Sign in
        </Link>
        <Link to="/register" className="rounded-sm bg-accent px-2.5 py-1 font-semibold text-white hover:bg-accent/90">
          Register
        </Link>
      </div>
    );
  }
  return (
    <div className="flex items-center gap-3">
      <span className="text-fg-muted">
        <span className="text-fg">{user.displayName}</span>
        {user.role === 'ADMIN' && (
          <span className="ml-2 rounded-sm border border-accent/40 px-1.5 py-0.5 text-[10px] font-bold text-accent">
            ADMIN
          </span>
        )}
      </span>
      <button
        type="button"
        onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/login', { replace: true }) })}
        disabled={logout.isPending}
        className="inline-flex items-center gap-1 rounded-sm px-2 py-1 text-fg-muted hover:bg-surface-3 hover:text-fg disabled:opacity-50"
      >
        <LogOut className="h-3.5 w-3.5" aria-hidden />
        Sign out
      </button>
    </div>
  );
}
