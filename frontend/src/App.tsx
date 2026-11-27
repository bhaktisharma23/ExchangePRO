import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/layouts/AppShell';
import { GuestOnlyRoute, ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { useSessionBootstrap } from '@/hooks/useSessionBootstrap';
import { useMarketStream } from '@/hooks/useMarketData';
import { SystemStatusPage } from '@/pages/SystemStatusPage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { AccountPage } from '@/pages/AccountPage';
import { MarketsPage } from '@/pages/MarketsPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

// The chart library is large; load the instrument page (and the chart) only when it is first opened.
const InstrumentPage = lazy(() => import('@/pages/InstrumentPage').then((m) => ({ default: m.InstrumentPage })));

const pageFallback = (
  <div role="status" className="p-6 text-fg-muted">
    Loading…
  </div>
);

export function App() {
  useSessionBootstrap();
  useMarketStream();

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/markets" replace />} />
        <Route path="markets" element={<ProtectedRoute><MarketsPage /></ProtectedRoute>} />
        <Route
          path="instruments/:symbol"
          element={<ProtectedRoute><Suspense fallback={pageFallback}><InstrumentPage /></Suspense></ProtectedRoute>}
        />
        <Route path="login" element={<GuestOnlyRoute><LoginPage /></GuestOnlyRoute>} />
        <Route path="register" element={<GuestOnlyRoute><RegisterPage /></GuestOnlyRoute>} />
        <Route path="account" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
        <Route path="system" element={<SystemStatusPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
